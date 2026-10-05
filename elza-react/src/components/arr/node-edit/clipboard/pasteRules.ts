import { DataString, DataType, FormItemType, NodeItem } from 'elza-api';
import { isDataString } from '../templates/types';
import { isValueEqual } from '../templates/utils';
import { ClipboardItem } from './itemClipboard';

/** Size of the `arr_data_string.string_value` column; the backend does not send it. */
export const STRING_MAX_LENGTH = 1000;

export type PasteSkipReason =
    | 'calculated'
    | 'otherFund'
    | 'notUndefinable'
    | 'undefinedNextToValue'
    | 'specExists'
    | 'wouldRewriteUndefined'
    | 'notRepeatable';

export interface SkippedItem {
    item: ClipboardItem;
    reason: PasteSkipReason;
}

export interface PastePlan {
    /** Items to paste, long strings already truncated. */
    items: ClipboardItem[];
    /** Items equal to an own value of the node; skipped silently. */
    duplicateCount: number;
    skipped: SkippedItem[];
    truncated: ClipboardItem[];
}

interface PastePlanParams {
    /** The target node's own items (inherited ones excluded). */
    ownItems: NodeItem[];
    incomingItems: ClipboardItem[];
    itemTypes: FormItemType[];
    sourceFundId: number;
    targetFundId: number;
}

// Structured objects and files belong to a fund; a reference to them is invalid in another one.
const fundBoundDataTypes: string[] = [DataType.Structured, DataType.FileRef];

function isBareUndefined(item: NodeItem) {
    return Boolean(item.undefined) && item.itemSpecId == undefined;
}

/** A value, or an undefined item with a spec; a bare undefined is not a value. */
function isValue(item: NodeItem) {
    return !isBareUndefined(item);
}

function truncateLongString(item: ClipboardItem) {
    const data = item.data;
    if (!data || !isDataString(data) || data.stringValue == undefined) {
        return { item, isTruncated: false };
    }
    // Code points, not UTF-16 units: the DB column limit counts characters.
    const characters = Array.from(data.stringValue);
    if (characters.length <= STRING_MAX_LENGTH) {
        return { item, isTruncated: false };
    }
    const truncatedData: DataString = { ...data, stringValue: characters.slice(0, STRING_MAX_LENGTH).join('') };
    return { item: { ...item, data: truncatedData }, isTruncated: true };
}

function getSkipReason(
    incomingItem: ClipboardItem,
    typeItems: NodeItem[],
    itemType: FormItemType | undefined,
    isOtherFund: boolean,
): PasteSkipReason | undefined {
    const isCalculatedAutomatically = Boolean(itemType?.cal) && !itemType?.calSt;
    if (isCalculatedAutomatically) {
        return 'calculated';
    }

    const isFundBound = incomingItem.data != undefined && fundBoundDataTypes.includes(incomingItem.data.dataType);
    if (isOtherFund && isFundBound) {
        return 'otherFund';
    }

    const hasSpec = incomingItem.itemSpecId != undefined;
    const hasSameSpecItem = hasSpec && typeItems.some(({ itemSpecId }) => itemSpecId === incomingItem.itemSpecId);

    if (incomingItem.undefined) {
        if (itemType && !itemType.undefinable) {
            return 'notUndefinable';
        }
        if (!hasSpec && typeItems.some(isValue)) {
            return 'undefinedNextToValue';
        }
        if (hasSameSpecItem) {
            return 'specExists';
        }
    } else {
        const hasSameSpecUndefined =
            hasSpec && typeItems.some((item) => item.undefined && item.itemSpecId === incomingItem.itemSpecId);
        if (typeItems.some(isBareUndefined) || hasSameSpecUndefined) {
            return 'wouldRewriteUndefined';
        }
    }

    const isNotRepeatable = itemType?.repeatable === false;
    if (isNotRepeatable && isValue(incomingItem) && typeItems.some(isValue)) {
        return 'notRepeatable';
    }

    return undefined;
}

/**
 * Decides which clipboard items can be appended to the target node.
 * Items are checked in order, each against the node's own items plus the items accepted before it.
 * The mandatory level is not checked: an impossible type is pasted and left to validation.
 */
export function planPaste({
    ownItems,
    incomingItems,
    itemTypes,
    sourceFundId,
    targetFundId,
}: PastePlanParams): PastePlan {
    const isOtherFund = sourceFundId !== targetFundId;
    const unmatchedOwnItems = [...ownItems];
    const effectiveItems: NodeItem[] = [...ownItems];
    const plan: PastePlan = { items: [], duplicateCount: 0, skipped: [], truncated: [] };

    incomingItems.forEach((incomingItem) => {
        const duplicateIndex = unmatchedOwnItems.findIndex(
            (ownItem) => ownItem.itemTypeId === incomingItem.itemTypeId && isValueEqual(ownItem, incomingItem),
        );
        if (duplicateIndex >= 0) {
            unmatchedOwnItems.splice(duplicateIndex, 1);
            plan.duplicateCount++;
            return;
        }

        const itemType = itemTypes.find(({ itemTypeId }) => itemTypeId === incomingItem.itemTypeId);
        const typeItems = effectiveItems.filter(({ itemTypeId }) => itemTypeId === incomingItem.itemTypeId);
        const skipReason = getSkipReason(incomingItem, typeItems, itemType, isOtherFund);
        if (skipReason) {
            plan.skipped.push({ item: incomingItem, reason: skipReason });
            return;
        }

        const { item, isTruncated } = truncateLongString(incomingItem);
        if (isTruncated) {
            plan.truncated.push(item);
        }
        plan.items.push(item);
        effectiveItems.push(item);
    });

    return plan;
}
