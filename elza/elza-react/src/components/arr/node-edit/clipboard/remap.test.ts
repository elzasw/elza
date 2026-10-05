import { DataType, NodeItem } from 'elza-api';
import { describe, expect, it } from 'vitest';
import { ClipboardItem } from './itemClipboard';
import { GetItemTypeInfo, ItemTypeInfo, normalizeString, selectItemsForType } from './remap';

// 1, 2: STRING without spec; 3: TEXT without spec; 4: STRING with spec; 5: INT
const typeInfos: Record<number, ItemTypeInfo> = {
    1: { dataType: DataType.String, useSpecification: false },
    2: { dataType: DataType.String, useSpecification: false },
    3: { dataType: DataType.Text, useSpecification: false },
    4: { dataType: DataType.String, useSpecification: true },
    5: { dataType: DataType.Int, useSpecification: false },
};
const getItemTypeInfo: GetItemTypeInfo = (itemTypeId) => typeInfos[itemTypeId];

function stringItem(itemTypeId: number, stringValue: string, extra: Partial<NodeItem> = {}): ClipboardItem {
    return { itemTypeId, data: { dataType: DataType.String, stringValue } as NodeItem['data'], ...extra };
}

function textItem(itemTypeId: number, textValue: string): ClipboardItem {
    return { itemTypeId, data: { dataType: DataType.Text, textValue } as NodeItem['data'] };
}

describe('selectItemsForType', () => {
    it('returns the items of the target type without remapping', () => {
        const items = [stringItem(1, 'a'), stringItem(2, 'b')];

        expect(selectItemsForType(items, 2, getItemTypeInfo)).toEqual([stringItem(2, 'b')]);
    });

    it('remaps STRING into another STRING type', () => {
        const result = selectItemsForType([stringItem(1, 'a', { position: 3 })], 2, getItemTypeInfo);

        expect(result).toEqual([stringItem(2, 'a', { position: 3 })]);
    });

    it('remaps STRING into TEXT', () => {
        const result = selectItemsForType([stringItem(1, 'a')], 3, getItemTypeInfo);

        expect(result).toEqual([textItem(3, 'a')]);
    });

    it('remaps TEXT into STRING, normalizing line breaks', () => {
        const result = selectItemsForType([textItem(3, ' first\r\nsecond  line ')], 1, getItemTypeInfo);

        expect(result).toEqual([stringItem(1, 'first second line')]);
    });

    it('does not remap into or from a type with a specification', () => {
        expect(selectItemsForType([stringItem(1, 'a')], 4, getItemTypeInfo)).toEqual([]);
        expect(selectItemsForType([stringItem(4, 'a', { itemSpecId: 9 })], 1, getItemTypeInfo)).toEqual([]);
    });

    it('does not remap into another data type', () => {
        expect(selectItemsForType([stringItem(1, 'a')], 5, getItemTypeInfo)).toEqual([]);
    });

    it('does not remap when the clipboard holds several compatible types', () => {
        const items = [stringItem(1, 'a'), textItem(3, 'b')];

        expect(selectItemsForType(items, 2, getItemTypeInfo)).toEqual([]);
    });

    it('ignores incompatible types when picking the source type', () => {
        const intItem: ClipboardItem = { itemTypeId: 5, data: { dataType: DataType.Int, integerValue: 1 } as NodeItem['data'] };

        const result = selectItemsForType([stringItem(1, 'a'), intItem], 2, getItemTypeInfo);

        expect(result).toEqual([stringItem(2, 'a')]);
    });

    it('does not remap undefined items', () => {
        const items: ClipboardItem[] = [{ itemTypeId: 1, undefined: true }];

        expect(selectItemsForType(items, 2, getItemTypeInfo)).toEqual([]);
    });

    it('does not remap types missing from the reference tables', () => {
        expect(selectItemsForType([stringItem(1, 'a')], 99, getItemTypeInfo)).toEqual([]);
        expect(selectItemsForType([stringItem(99, 'a')], 1, getItemTypeInfo)).toEqual([]);
    });
});

describe('normalizeString', () => {
    it('matches the backend: control characters become spaces, spaces collapse, ends trimmed', () => {
        expect(normalizeString('\ta\n\nb\u0001c  ')).toBe('a b c');
    });
});
