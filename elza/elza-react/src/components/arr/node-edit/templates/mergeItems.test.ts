import { describe, expect, it } from 'vitest';
import { DataType, NodeItem } from 'elza-api';
import { mergeItemsIntoNode } from './mergeItems';

function stringItem(itemTypeId: number, stringValue: string | undefined, extra: Partial<NodeItem> = {}): NodeItem {
    return {
        itemTypeId,
        data: { dataType: DataType.String, stringValue } as NodeItem['data'],
        ...extra,
    };
}

function ownStringItem(itemTypeId: number, stringValue: string, itemObjectId: number, position: number): NodeItem {
    return stringItem(itemTypeId, stringValue, { itemObjectId, position });
}

describe('mergeItemsIntoNode', () => {
    it('appends incoming values after the existing own values of the same type', () => {
        const own = [ownStringItem(1, 'a', 100, 1), ownStringItem(1, 'b', 101, 2)];
        const incoming = [stringItem(1, 'c', { position: 1 }), stringItem(1, 'd', { position: 2 })];

        const result = mergeItemsIntoNode(own, incoming, { replace: false });

        expect(result.createItems.map(({ position }) => position)).toEqual([3, 4]);
        expect(result.deleteItems).toEqual([]);
    });

    it('starts positions from 1 for a type the node does not have', () => {
        const result = mergeItemsIntoNode([], [stringItem(2, 'x', { position: 5 })], { replace: false });

        expect(result.createItems).toHaveLength(1);
        expect(result.createItems[0].position).toBe(1);
    });

    it('skips incoming values equal to an existing own value', () => {
        const own = [ownStringItem(1, 'a', 100, 1)];
        const incoming = [stringItem(1, 'a'), stringItem(1, 'b')];

        const result = mergeItemsIntoNode(own, incoming, { replace: false });

        expect(result.createItems).toHaveLength(1);
        expect(result.createItems[0].data).toMatchObject({ stringValue: 'b' });
    });

    it('matches each own value at most once', () => {
        const own = [ownStringItem(1, 'a', 100, 1)];
        const incoming = [stringItem(1, 'a', { position: 1 }), stringItem(1, 'a', { position: 2 })];

        const result = mergeItemsIntoNode(own, incoming, { replace: false });

        expect(result.createItems).toHaveLength(1);
        expect(result.createItems[0].position).toBe(2);
    });

    it('does not treat equal values of a different type as duplicates', () => {
        const own = [ownStringItem(1, 'a', 100, 1)];

        const result = mergeItemsIntoNode(own, [stringItem(2, 'a')], { replace: false });

        expect(result.createItems).toHaveLength(1);
        expect(result.createItems[0].itemTypeId).toBe(2);
    });

    it('with replace, deletes unmatched own values of the affected types only', () => {
        const kept = ownStringItem(1, 'a', 100, 1);
        const replaced = ownStringItem(1, 'b', 101, 2);
        const otherType = ownStringItem(2, 'z', 102, 1);

        const result = mergeItemsIntoNode([kept, replaced, otherType], [stringItem(1, 'a'), stringItem(1, 'c')], {
            replace: true,
        });

        expect(result.deleteItems).toEqual([replaced]);
        expect(result.createItems).toHaveLength(1);
        expect(result.createItems[0].position).toBe(1);
    });

    it('creates an undefined item, which has no data', () => {
        const result = mergeItemsIntoNode([], [{ itemTypeId: 4, undefined: true }], { replace: false });

        expect(result.createItems).toEqual([{ itemTypeId: 4, undefined: true, position: 1 }]);
        expect(result.missingEmptyItems).toEqual([]);
    });

    it('skips an undefined item when the node already has one of that type', () => {
        const own = [{ itemTypeId: 4, undefined: true, itemObjectId: 100, position: 1 }];

        const result = mergeItemsIntoNode(own, [{ itemTypeId: 4, undefined: true }], { replace: false });

        expect(result.createItems).toEqual([]);
    });

    it('does not treat an undefined item as equal to a value', () => {
        const own = [ownStringItem(4, 'a', 100, 1)];

        const result = mergeItemsIntoNode(own, [{ itemTypeId: 4, undefined: true }], { replace: false });

        expect(result.createItems).toHaveLength(1);
    });

    it('treats an item without data as having no value', () => {
        const result = mergeItemsIntoNode([], [{ itemTypeId: 5 }], { replace: false });

        expect(result.createItems).toEqual([]);
        expect(result.missingEmptyItems).toEqual([{ itemTypeId: 5 }]);
    });

    it('returns incoming items without a value only when the node lacks their type and spec', () => {
        const own = [ownStringItem(1, 'a', 100, 1)];
        const incoming = [stringItem(1, undefined), stringItem(3, undefined)];

        const result = mergeItemsIntoNode(own, incoming, { replace: false });

        expect(result.createItems).toEqual([]);
        expect(result.missingEmptyItems.map(({ itemTypeId }) => itemTypeId)).toEqual([3]);
    });
});
