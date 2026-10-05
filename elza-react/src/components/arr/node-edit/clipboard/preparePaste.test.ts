import { DataType, FormItemType, MandatoryType, NodeItem } from 'elza-api';
import { describe, expect, it } from 'vitest';
import { ClipboardItem } from './itemClipboard';
import { getPastableTypeIds, preparePaste } from './preparePaste';
import { GetItemTypeInfo, ItemTypeInfo } from './remap';

const FUND_ID = 1;
const NODE_ID = 100;

// 1, 2: STRING without spec; 3: TEXT without spec; 4: STRING with spec; 5: INT
const typeInfos: Record<number, ItemTypeInfo> = {
    1: { dataType: DataType.String, useSpecification: false },
    2: { dataType: DataType.String, useSpecification: false },
    3: { dataType: DataType.Text, useSpecification: false },
    4: { dataType: DataType.String, useSpecification: true },
    5: { dataType: DataType.Int, useSpecification: false },
};
const getItemTypeInfo: GetItemTypeInfo = (itemTypeId) => typeInfos[itemTypeId];

function itemType(itemTypeId: number, overrides: Partial<FormItemType> = {}): FormItemType {
    return {
        itemTypeId,
        type: MandatoryType.Possible,
        repeatable: true,
        undefinable: true,
        favoriteSpecIds: [],
        ...overrides,
    };
}

const allTypes = [itemType(1), itemType(2), itemType(3), itemType(4), itemType(5)];

function stringItem(itemTypeId: number, stringValue: string, extra: Partial<NodeItem> = {}): NodeItem {
    return { itemTypeId, data: { dataType: DataType.String, stringValue } as NodeItem['data'], ...extra };
}

function textItem(itemTypeId: number, textValue: string): ClipboardItem {
    return { itemTypeId, data: { dataType: DataType.Text, textValue } as NodeItem['data'] };
}

function pastable(clipboardItems: ClipboardItem[], descItems: NodeItem[] = [], itemTypes = allTypes) {
    return getPastableTypeIds({
        candidateTypeIds: [1, 2, 3, 4, 5],
        clipboardItems,
        sourceFundId: FUND_ID,
        descItems,
        itemTypes,
        nodeId: NODE_ID,
        targetFundId: FUND_ID,
        getItemTypeInfo,
    });
}

describe('getPastableTypeIds', () => {
    it('lists the stored type and every compatible type', () => {
        expect(pastable([stringItem(1, 'a')])).toEqual([1, 2, 3]);
    });

    it('lists only the stored types when remapping is ambiguous', () => {
        expect(pastable([stringItem(1, 'a'), textItem(3, 'b')])).toEqual([1, 3]);
    });

    it('leaves out a non-repeatable type that already has an own value', () => {
        const ownValue = stringItem(2, 'existing', { nodeId: NODE_ID, itemObjectId: 50, position: 1 });

        const result = pastable([stringItem(1, 'a')], [ownValue], [itemType(1), itemType(2, { repeatable: false }), itemType(3)]);

        expect(result).toEqual([1, 3]);
    });

    it('leaves out a type where every value is already present', () => {
        const ownValue = stringItem(1, 'a', { nodeId: NODE_ID, itemObjectId: 50, position: 1 });

        expect(pastable([stringItem(1, 'a')], [ownValue])).not.toContain(1);
    });

    it('ignores inherited values when deciding', () => {
        const inheritedValue = stringItem(2, 'existing', { nodeId: 1, itemObjectId: 60, position: 1 });

        const result = pastable([stringItem(1, 'a')], [inheritedValue], [itemType(1), itemType(2, { repeatable: false })]);

        expect(result).toContain(2);
    });
});

describe('preparePaste', () => {
    it('returns the rule outcome and the positioned items to create', () => {
        const ownValue = stringItem(1, 'a', { nodeId: NODE_ID, itemObjectId: 50, position: 1 });

        const { plan, createItems } = preparePaste({
            clipboardItems: [stringItem(1, 'a'), stringItem(1, 'b')],
            sourceFundId: FUND_ID,
            descItems: [ownValue],
            itemTypes: allTypes,
            nodeId: NODE_ID,
            targetFundId: FUND_ID,
            getItemTypeInfo,
        });

        expect(plan.duplicateCount).toBe(1);
        expect(createItems).toEqual([stringItem(1, 'b', { position: 2 })]);
    });
});
