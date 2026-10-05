import { DataType, FormItemType, MandatoryType, NodeItem } from 'elza-api';
import { describe, expect, it } from 'vitest';
import { ClipboardItem } from './itemClipboard';
import { planPaste, STRING_MAX_LENGTH } from './pasteRules';

const FUND_ID = 1;

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

function stringItem(itemTypeId: number, stringValue: string, extra: Partial<NodeItem> = {}): NodeItem {
    return {
        itemTypeId,
        data: { dataType: DataType.String, stringValue } as NodeItem['data'],
        ...extra,
    };
}

function enumItem(itemTypeId: number, itemSpecId: number, extra: Partial<NodeItem> = {}): NodeItem {
    return { itemTypeId, itemSpecId, data: { dataType: DataType.Enum } as NodeItem['data'], ...extra };
}

function undefinedItem(itemTypeId: number, itemSpecId?: number): ClipboardItem {
    return { itemTypeId, itemSpecId, undefined: true };
}

function plan(ownItems: NodeItem[], incomingItems: ClipboardItem[], itemTypes: FormItemType[], sourceFundId = FUND_ID) {
    return planPaste({ ownItems, incomingItems, itemTypes, sourceFundId, targetFundId: FUND_ID });
}

describe('planPaste', () => {
    it('accepts values into an empty type', () => {
        const result = plan([], [stringItem(1, 'a'), stringItem(1, 'b')], [itemType(1)]);

        expect(result.items).toHaveLength(2);
        expect(result.skipped).toEqual([]);
    });

    it('skips values equal to an own value silently, matching each own value once', () => {
        const result = plan([stringItem(1, 'a')], [stringItem(1, 'a'), stringItem(1, 'a')], [itemType(1)]);

        expect(result.duplicateCount).toBe(1);
        expect(result.items).toHaveLength(1);
        expect(result.skipped).toEqual([]);
    });

    it('pastes into an impossible type', () => {
        const result = plan([], [stringItem(1, 'a')], [itemType(1, { type: MandatoryType.Impossible })]);

        expect(result.items).toHaveLength(1);
    });

    it('pastes into a type missing from the form', () => {
        const result = plan([], [stringItem(1, 'a')], []);

        expect(result.items).toHaveLength(1);
    });

    describe('repeatable', () => {
        const types = [itemType(1, { repeatable: false })];

        it('skips a value when a non-repeatable type already has one', () => {
            const result = plan([stringItem(1, 'a')], [stringItem(1, 'b')], types);

            expect(result.skipped).toEqual([{ item: stringItem(1, 'b'), reason: 'notRepeatable' }]);
        });

        it('accepts only the first of several values into an empty non-repeatable type', () => {
            const result = plan([], [stringItem(1, 'a'), stringItem(1, 'b')], types);

            expect(result.items).toEqual([stringItem(1, 'a')]);
            expect(result.skipped.map(({ reason }) => reason)).toEqual(['notRepeatable']);
        });

        it('treats an own undefined item with a spec as a value', () => {
            const result = plan([{ ...undefinedItem(1, 7) }], [enumItem(1, 8)], types);

            expect(result.skipped.map(({ reason }) => reason)).toEqual(['notRepeatable']);
        });
    });

    describe('undefined items', () => {
        it('skips an undefined item into a type that is not undefinable', () => {
            const result = plan([], [undefinedItem(1)], [itemType(1, { undefinable: false })]);

            expect(result.skipped.map(({ reason }) => reason)).toEqual(['notUndefinable']);
        });

        it('skips a bare undefined item into a type with an own value', () => {
            const result = plan([stringItem(1, 'a')], [undefinedItem(1)], [itemType(1)]);

            expect(result.skipped.map(({ reason }) => reason)).toEqual(['undefinedNextToValue']);
        });

        it('treats a bare undefined item equal to an own one as a duplicate', () => {
            const result = plan([undefinedItem(1)], [undefinedItem(1)], [itemType(1)]);

            expect(result.duplicateCount).toBe(1);
            expect(result.skipped).toEqual([]);
        });

        it('accepts a bare undefined item into an empty type', () => {
            const result = plan([], [undefinedItem(1)], [itemType(1, { repeatable: false })]);

            expect(result.items).toEqual([undefinedItem(1)]);
        });

        it('skips an undefined item with a spec the node already has with a value', () => {
            const result = plan([enumItem(1, 7)], [undefinedItem(1, 7)], [itemType(1)]);

            expect(result.skipped.map(({ reason }) => reason)).toEqual(['specExists']);
        });

        it('accepts an undefined item with a spec the node does not have', () => {
            const result = plan([enumItem(1, 7)], [undefinedItem(1, 8)], [itemType(1)]);

            expect(result.items).toEqual([undefinedItem(1, 8)]);
        });
    });

    describe('values next to undefined items', () => {
        it('skips a value into a type with a bare undefined item', () => {
            const result = plan([undefinedItem(1)], [stringItem(1, 'a')], [itemType(1)]);

            expect(result.skipped.map(({ reason }) => reason)).toEqual(['wouldRewriteUndefined']);
        });

        it('skips a value with a spec the node has as undefined', () => {
            const result = plan([undefinedItem(1, 7)], [enumItem(1, 7)], [itemType(1)]);

            expect(result.skipped.map(({ reason }) => reason)).toEqual(['wouldRewriteUndefined']);
        });

        it('accepts a value with another spec next to an undefined item with a spec', () => {
            const result = plan([undefinedItem(1, 7)], [enumItem(1, 8)], [itemType(1)]);

            expect(result.items).toEqual([enumItem(1, 8)]);
        });
    });

    describe('fund-bound references', () => {
        const structureRef: ClipboardItem = {
            itemTypeId: 1,
            data: { dataType: DataType.Structured, structuredObjectId: 5 } as NodeItem['data'],
        };
        const fileRef: ClipboardItem = {
            itemTypeId: 2,
            data: { dataType: DataType.FileRef, fileId: 6 } as NodeItem['data'],
        };
        const recordRef: ClipboardItem = {
            itemTypeId: 3,
            data: { dataType: DataType.RecordRef, value: 7 } as NodeItem['data'],
        };
        const types = [itemType(1), itemType(2), itemType(3)];

        it('skips structure and file references from another fund, keeps record references', () => {
            const result = plan([], [structureRef, fileRef, recordRef], types, 2);

            expect(result.skipped.map(({ reason }) => reason)).toEqual(['otherFund', 'otherFund']);
            expect(result.items).toEqual([recordRef]);
        });

        it('accepts structure and file references within the same fund', () => {
            const result = plan([], [structureRef, fileRef], types);

            expect(result.items).toHaveLength(2);
        });
    });

    it('skips a type calculated automatically', () => {
        const result = plan([], [stringItem(1, 'a')], [itemType(1, { cal: true, calSt: false })]);

        expect(result.skipped.map(({ reason }) => reason)).toEqual(['calculated']);
    });

    it('accepts a calculated type switched to manual', () => {
        const result = plan([], [stringItem(1, 'a')], [itemType(1, { cal: true, calSt: true })]);

        expect(result.items).toHaveLength(1);
    });

    describe('string length', () => {
        it('truncates a STRING value over the limit and reports it', () => {
            const longValue = 'x'.repeat(STRING_MAX_LENGTH + 5);

            const result = plan([], [stringItem(1, longValue)], [itemType(1)]);

            expect(result.items[0].data).toMatchObject({ stringValue: 'x'.repeat(STRING_MAX_LENGTH) });
            expect(result.truncated).toEqual(result.items);
        });

        it('counts characters, not UTF-16 units', () => {
            const emojiValue = '😀'.repeat(STRING_MAX_LENGTH);

            const result = plan([], [stringItem(1, emojiValue)], [itemType(1)]);

            expect(result.truncated).toEqual([]);
        });

        it('does not truncate TEXT values', () => {
            const textItem: ClipboardItem = {
                itemTypeId: 1,
                data: { dataType: DataType.Text, textValue: 'x'.repeat(STRING_MAX_LENGTH + 5) } as NodeItem['data'],
            };

            const result = plan([], [textItem], [itemType(1)]);

            expect(result.truncated).toEqual([]);
        });
    });
});
