import { describe, expect, it } from 'vitest';
import { ApViewSettingRule } from 'api/ApViewSettings';
import { RevisionItem } from 'components/registry/revision';
import { RefTablesState } from 'typings/store';
import { findItemPlacePosition, sortItems } from './partEdit';

const NOTE = 1;
const NM_MAIN = 2;
const NM_MINOR = 3;
const PT_NAME = 10;

// NOTE has the lowest global view order, the rule set puts it last in PT_NAME
const refTables = {
    partTypes: { itemsMap: { [PT_NAME]: { id: PT_NAME, code: 'PT_NAME' } } },
    descItemTypes: {
        itemsMap: {
            [NOTE]: { id: NOTE, code: 'NOTE', viewOrder: 1 },
            [NM_MAIN]: { id: NM_MAIN, code: 'NM_MAIN', viewOrder: 10 },
            [NM_MINOR]: { id: NM_MINOR, code: 'NM_MINOR', viewOrder: 20 },
        },
    },
} as unknown as RefTablesState;

const viewSettings = {
    itemTypes: [
        { code: 'NM_MAIN', partType: 'PT_NAME', position: 10 },
        { code: 'NM_MINOR', partType: 'PT_NAME', position: 20 },
        { code: 'NOTE', partType: 'PT_NAME', position: 70 },
    ],
} as unknown as ApViewSettingRule;

const item = (typeId: number): RevisionItem => ({ typeId, '@class': 'x' }) as RevisionItem;

describe('part item order', () => {
    it('follows the positions of the rule set', () => {
        const sorted = sortItems(PT_NAME, [item(NOTE), item(NM_MINOR), item(NM_MAIN)], refTables, viewSettings);
        expect(sorted.map((i) => i.typeId)).toEqual([NM_MAIN, NM_MINOR, NOTE]);
    });

    it('falls back to the view order of the item type', () => {
        const sorted = sortItems(PT_NAME, [item(NM_MINOR), item(NM_MAIN), item(NOTE)], refTables);
        expect(sorted.map((i) => i.typeId)).toEqual([NOTE, NM_MAIN, NM_MINOR]);
    });

    it('inserts a new item at its place by the rule set', () => {
        const items = [item(NM_MAIN), item(NOTE)];
        expect(findItemPlacePosition(item(NM_MINOR), items, PT_NAME, refTables, viewSettings)).toBe(1);
        expect(findItemPlacePosition(item(NOTE), [item(NM_MAIN)], PT_NAME, refTables, viewSettings)).toBe(1);
    });
});
