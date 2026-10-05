import { NodeItem } from 'elza-api';
import { hasValue, isValueEqual } from './utils';

export interface MergeItemsOptions {
    replace: boolean;
}

export interface MergeItemsResult {
    createItems: NodeItem[];
    deleteItems: NodeItem[];
    /** Incoming items without a value whose type and spec the node does not have yet. */
    missingEmptyItems: NodeItem[];
}

/**
 * Merges incoming items into the node's own items.
 * Incoming values equal to an existing own value are skipped; new values are positioned after
 * the existing ones of their type (or from 1 when replacing). With `replace`, the remaining own
 * values of every type that received incoming values are deleted.
 */
export function mergeItemsIntoNode(
    ownItems: NodeItem[],
    incomingItems: NodeItem[],
    { replace }: MergeItemsOptions,
): MergeItemsResult {
    const itemsWithoutValue = incomingItems.filter((item) => !hasValue(item));
    const itemsWithValue = incomingItems
        .filter((item) => hasValue(item))
        .sort((a, b) => (a.itemTypeId ?? 0) - (b.itemTypeId ?? 0) || (a.position ?? 0) - (b.position ?? 0));

    const createItems: NodeItem[] = [];
    const deleteItems: NodeItem[] = [];

    const itemTypePositions = new Map<number | undefined, number>();
    const skippedItemObjectIds: number[] = [];

    itemsWithValue.forEach((incomingItem) => {
        // remove items already processed, exclude items without values
        const pendingItems = ownItems.filter(
            ({ itemObjectId }) => itemObjectId != undefined && !skippedItemObjectIds.includes(itemObjectId),
        );

        // skip items that already have the same value
        const itemWithSameValue = pendingItems.find(
            (ownItem) => ownItem.itemTypeId === incomingItem.itemTypeId && isValueEqual(ownItem, incomingItem),
        );
        if (itemWithSameValue?.itemObjectId != undefined) {
            skippedItemObjectIds.push(itemWithSameValue.itemObjectId);
            return;
        }

        const highestPositionItem = ownItems
            .filter(({ itemTypeId }) => itemTypeId === incomingItem.itemTypeId)
            .sort((a, b) => (a.position ?? 0) - (b.position ?? 0))
            .pop();

        const lastPosition =
            itemTypePositions.get(incomingItem.itemTypeId) || // incremented position
            (!replace && highestPositionItem?.position) || // previous item position
            0;
        const nextPosition = lastPosition + 1;
        itemTypePositions.set(incomingItem.itemTypeId, nextPosition);

        createItems.push({
            ...incomingItem,
            position: nextPosition,
        });
    });

    // Delete the remaining unprocessed own values of every type that received incoming values
    if (replace) {
        const processedItemTypeIds = itemsWithValue.map(({ itemTypeId }) => itemTypeId);
        ownItems
            .filter(
                ({ itemObjectId }) => itemObjectId != undefined && !skippedItemObjectIds.includes(itemObjectId),
            )
            .forEach((ownItem) => {
                if (processedItemTypeIds.includes(ownItem.itemTypeId)) {
                    deleteItems.push(ownItem);
                }
            });
    }

    const missingEmptyItems = itemsWithoutValue.filter(
        ({ itemTypeId, itemSpecId }) =>
            !ownItems.find((ownItem) => ownItem.itemTypeId === itemTypeId && ownItem.itemSpecId === itemSpecId),
    );

    return { createItems, deleteItems, missingEmptyItems };
}
