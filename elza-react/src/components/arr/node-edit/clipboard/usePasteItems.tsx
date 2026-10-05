import { WebApi } from 'actions';
import { NodeFormData } from 'elza-api';
import { useIntl } from 'react-intl';
import { addToastrDanger, addToastrSuccess, addToastrWarning } from 'components/shared/toastr/ToastrActions';
import { useAppThunkDispatch } from 'utils/hooks';
import { useAppSelector } from 'utils/hooks/useAppSelector';
import { ClipboardItem, readItemClipboard } from './itemClipboard';
import { clipboardMessages, skipReasonMessages } from './messages';
import { PastePlan, STRING_MAX_LENGTH } from './pasteRules';
import { preparePaste } from './preparePaste';
import { useItemTypeInfo } from './useItemTypeInfo';

interface PasteItemsParams {
    formData: NodeFormData;
    nodeId: number;
    nodeVersion: number;
    fundId: number;
    fundVersionId: number;
    /** Pastes only into this DescItemType, remapping STRING/TEXT values when allowed; every stored DescItem into its own type when omitted. */
    itemTypeId?: number;
}

/** Collapses repeated lines into one with a count, keeping the first occurrence's order. */
function countLines(lines: string[]) {
    const counts = new Map<string, number>();
    lines.forEach((line) => counts.set(line, (counts.get(line) ?? 0) + 1));
    return Array.from(counts, ([line, count]) => (count > 1 ? `${line} (${count}×)` : line));
}

/** Appends the clipboard items to the node, following the paste rules, and reports the result in a toast. */
export function usePasteItems() {
    const dispatch = useAppThunkDispatch();
    const { formatMessage } = useIntl();
    const itemTypeRefs = useAppSelector(({ refTables }) => refTables.descItemTypes.itemsMap);
    const getItemTypeInfo = useItemTypeInfo();

    function getItemLabel({ itemTypeId, itemSpecId }: ClipboardItem) {
        const typeRef = itemTypeId != undefined ? itemTypeRefs[itemTypeId] : undefined;
        if (!typeRef) {
            return `#${itemTypeId}`;
        }
        const specName =
            itemSpecId != undefined ? typeRef.descItemSpecs?.find(({ id }) => id === itemSpecId)?.name : undefined;
        return specName ? `${typeRef.name} – ${specName}` : typeRef.name;
    }

    function getReportLines({ skipped, truncated }: PastePlan) {
        const skippedLines = skipped.map(({ item, reason }) =>
            formatMessage(clipboardMessages.reportLine, {
                label: getItemLabel(item),
                reason: formatMessage(skipReasonMessages[reason]),
            }),
        );
        const truncatedLines = truncated.map((item) =>
            formatMessage(clipboardMessages.reportLine, {
                label: getItemLabel(item),
                reason: formatMessage(clipboardMessages.truncated, { max: STRING_MAX_LENGTH }),
            }),
        );
        return countLines([...skippedLines, ...truncatedLines]);
    }

    return async function pasteItems({
        formData,
        nodeId,
        nodeVersion,
        fundId,
        fundVersionId,
        itemTypeId,
    }: PasteItemsParams) {
        const clipboard = readItemClipboard();
        if (!clipboard) {
            return;
        }

        const { plan, createItems } = preparePaste({
            clipboardItems: clipboard.items,
            sourceFundId: clipboard.fundId,
            descItems: formData.descItems,
            itemTypes: formData.itemTypes,
            nodeId,
            targetFundId: fundId,
            itemTypeId,
            getItemTypeInfo,
        });

        if (createItems.length > 0) {
            try {
                await WebApi.updateDescItems(fundVersionId, nodeId, nodeVersion, createItems, [], []);
            } catch (error) {
                const errorMessage = (error as { errorMessage?: string } | undefined)?.errorMessage;
                dispatch(addToastrDanger(formatMessage(clipboardMessages.pasteFailed), errorMessage ?? null));
                return;
            }
        }

        // The merge also skips a repeated value the plan matched against an already used own item.
        const duplicateCount = plan.duplicateCount + plan.items.length - createItems.length;
        const reportLines = getReportLines(plan);
        const hasProblems = reportLines.length > 0;
        const hasDetails = hasProblems || duplicateCount > 0;

        const title = formatMessage(clipboardMessages.pasted, { count: createItems.length });
        const details = hasDetails ? (
            <>
                {duplicateCount > 0 && <div>{formatMessage(clipboardMessages.duplicates, { count: duplicateCount })}</div>}
                {hasProblems && (
                    <ul>
                        {reportLines.map((line) => (
                            <li key={line}>{line}</li>
                        ))}
                    </ul>
                )}
            </>
        ) : null;

        dispatch(hasProblems ? addToastrWarning(title, details) : addToastrSuccess(title, details));
    };
}
