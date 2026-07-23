import { Popover, PopoverSurface, Spinner } from "@fluentui/react-components";
import { copyDescItemType, nocopyDescItemType } from "actions/arr/nodeSetting";
import { WebApi } from "actions";
import { DataType, FormItemType, MandatoryType } from "elza-api";
import { useEffect, useMemo } from "react";
import { defineMessages, FormattedMessage } from "react-intl";
import { useAppThunkDispatch } from "utils/hooks";
import { useAppSelector } from "utils/hooks/useAppSelector";
import { useActiveFund, useActiveParent, useNodeFormData, useStrictMode } from "./hooks";
import { NodeFormContext } from "./NodeFormContext";
import { DescItemTypeFields } from "./DescItemTypeFields";
import { useStyles } from "./styles";

const popoverWidthByTypeWidth: Record<number, number> = {
    0: 600,
    1: 300,
    2: 400,
    3: 500,
    4: 600,
};

const messages = defineMessages({
    notAllowed: {
        id: "arr.fundDataGrid.cellForm.notAllowed",
        defaultMessage: "Tento prvek popisu není možné přidat k jednotce popisu.",
    },
});

interface Props {
    fondsVersionId: number;
    nodeId: number;
    nodeVersionId?: number;
    descItemTypeId: number;
    target: HTMLElement;
    onClose: () => void;
}

export function FundDataGridCellForm({ fondsVersionId, nodeId, nodeVersionId, descItemTypeId, target, onClose }: Props) {
    const dispatch = useAppThunkDispatch();
    const activeParent = useActiveParent();
    const activeFund = useActiveFund();
    const styles = useStyles();

    const itemTypeRefs = useAppSelector(({ refTables }) => refTables.descItemTypes.itemsMap);
    const dataTypeRefs = useAppSelector(({ refTables }) => refTables.rulDataTypes.itemsMap);
    const groupRefs = useAppSelector(({ refTables }) => refTables.groups.data);
    const structureTypes = useAppSelector(
        ({ refTables }) =>
            refTables.structureTypes.data?.find(({ versionId }) => versionId === activeFund?.versionId)?.data || [],
    );
    const hasOpenModal = useAppSelector(({ modalDialog }) => modalDialog.items.length > 0);
    const nodeSetting = useAppSelector(({ arrRegion }) =>
        arrRegion.nodeSettings.nodes.find(({ id }) => id === activeParent?.id),
    );
    const strictMode = useStrictMode();

    const nodeFormData = useNodeFormData(fondsVersionId, nodeId, nodeVersionId);
    const {
        formItems,
        forcedFormItems,
        addedFormItems,
        itemTypes,
        isLoading,
        addEmptyDescItem,
        deleteDescItem,
        createDescItem: createDescItemBase,
        updateDescItem: updateDescItemBase,
    } = nodeFormData;

    async function createDescItem(item: any, localId: string) {
        await createDescItemBase(item, localId);
        const isSingleItem = !descItemTypeEntry || descItemTypeEntry.descItems.length <= 1;
        const hasData = item.data?.dataId != null;
        if (isSingleItem && hasData) {
            onClose();
        }
    }

    async function updateDescItem(item: any, localId?: string) {
        await updateDescItemBase(item, localId);
        const isSingleItem = !descItemTypeEntry || descItemTypeEntry.descItems.length <= 1;
        const hasData = item.data?.dataId != null;
        if (isSingleItem && hasData) {
            onClose();
        }
    }

    const typeRef = itemTypeRefs?.[descItemTypeId];

    // The server omits IMPOSSIBLE types from itemTypes (IMPOSSIBLE is the default state, so it isn't
    // transferred). A type missing from itemTypes is therefore IMPOSSIBLE for this node, and in
    // strict mode such a type may not be edited.
    const serverTypeForm = itemTypes?.find(({ itemTypeId }) => itemTypeId === descItemTypeId);
    const notAllowed = !isLoading && !serverTypeForm && strictMode;

    const descItemTypeEntry = useMemo(() => {
        if (!formItems || !groupRefs || !typeRef || notAllowed) { return null; }

        // Reconstruct a minimal IMPOSSIBLE FormItemType from the ref tables for a missing type so the
        // cell can still be edited (adding a value transitions it away from IMPOSSIBLE).
        const typeForm: FormItemType = serverTypeForm ?? {
            itemTypeId: descItemTypeId,
            type: MandatoryType.Impossible,
            repeatable: false,
            undefinable: false,
            specs: typeRef.descItemSpecs.map(({ id }) => ({
                itemSpecId: id,
                type: MandatoryType.Impossible,
                repeatable: false,
            })),
            favoriteSpecIds: [],
        };

        let typeWidth: number | undefined;
        for (const id of groupRefs.ids) {
            const group = groupRefs[id];
            const found = group.itemTypes.find((itemType) => itemType.id === descItemTypeId);
            if (found) { typeWidth = found.width; break; }
        }

        const allItems = [...formItems, ...forcedFormItems, ...addedFormItems];
        const descItems = allItems.filter(({ item }) => item.itemTypeId === descItemTypeId);

        return { typeRef, typeForm, typeWidth, descItems };
    }, [formItems, forcedFormItems, addedFormItems, serverTypeForm, notAllowed, groupRefs, typeRef, descItemTypeId]);

    // An anonymous structured field creates its server object as soon as the empty
    // placeholder mounts, so auto-adding one would persist a spurious blank value.
    // Other types (incl. non-anonymous structured) just add an empty field to edit in place.
    const isStructured = dataTypeRefs?.[typeRef?.dataTypeId ?? -1]?.code === DataType.Structured;
    const isAnonymousStructured =
        isStructured &&
        structureTypes.find(({ id }) => id === typeRef?.structureTypeId)?.anonymous === true;

    useEffect(() => {
        // Wait for the node data to settle; adding before the server items arrive would
        // race and leave a stray empty field next to the loaded value.
        if (isLoading || !descItemTypeEntry) { return; }
        if (descItemTypeEntry.descItems.length === 0 && !isAnonymousStructured) {
            addEmptyDescItem(descItemTypeId);
        }
    }, [isLoading, descItemTypeEntry, isAnonymousStructured]);

    async function handleCopyFromPrev(typeId: number) {
        await WebApi.copyOlderSiblingAttribute(activeFund.versionId, nodeId, nodeVersionId, typeId);
    }

    async function handleCopyToggle(typeId: number) {
        const copy = nodeSetting?.descItemTypeCopyIds?.includes(typeId);
        if (!copy) {
            dispatch(copyDescItemType(activeParent.id, typeId));
        } else {
            dispatch(nocopyDescItemType(activeParent.id, typeId));
        }
    }

    const isFirstNode =
        activeParent.childNodes.findIndex((node: any) => node.id === nodeId) === 0;

    return (
        <Popover
            open
            mountNode={{ className: hasOpenModal ? styles.fundDataGridPopoverBehindModal : undefined }}
            onOpenChange={(_event, data) => {
                if (data.open) { return; }
                // Opening a child modal (e.g. the structure "add" dialog) shifts focus/clicks
                // outside the popover, which Fluent reads as a dismiss. Keep the popover open
                // while any modal dialog is on screen.
                if (hasOpenModal) { return; }
                setTimeout(onClose, 0);
            }}
            positioning={{
                target,
                position: "below",
                align: "start",
                offset: { mainAxis: -target.getBoundingClientRect().height },
                // flipBoundary: "viewport",
                // overflowBoundary: "viewport"
            }}
        >
            <PopoverSurface
                className={styles.fundDataGridPopover}
                style={{ width: `${popoverWidthByTypeWidth[descItemTypeEntry?.typeWidth ?? 4] ?? 550}px` }}
            >
                {isLoading || (!descItemTypeEntry && !notAllowed) ? (
                    <Spinner />
                ) : notAllowed ? (
                    <FormattedMessage {...messages.notAllowed} />
                ) : (
                    <NodeFormContext.Provider value={nodeFormData}>
                        <DescItemTypeFields
                            typeRef={descItemTypeEntry.typeRef}
                            typeForm={descItemTypeEntry.typeForm}
                            typeWidth={descItemTypeEntry.typeWidth}
                            descItems={descItemTypeEntry.descItems}
                            fondsVersionId={fondsVersionId}
                            nodeId={nodeId}
                            nodeVersionId={nodeVersionId}
                            nodeSetting={nodeSetting}
                            isFirstNode={isFirstNode}
                            isAnonymousStructured={isAnonymousStructured}
                            handleCopyFromPrev={handleCopyFromPrev}
                            handleCopyToggle={handleCopyToggle}
                            addEmptyDescItem={addEmptyDescItem}
                            deleteDescItem={deleteDescItem}
                            createDescItem={createDescItem}
                            updateDescItem={updateDescItem}
                            hideCopyButtons
                        />
                    </NodeFormContext.Provider>
                )}
            </PopoverSurface>
        </Popover>
    );
}
