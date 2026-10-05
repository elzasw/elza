import { describe, it, expect, vi } from 'vitest';
import * as types from 'actions/constants/ActionTypes';
import fundTree from './fundTree';

// the reducer imports components/shared only for side effects, which leads back to the store
vi.mock('components/shared', () => ({}));

/**
 * Vybraný uzel, který mezitím někdo smazal: server ho ve stromu nevrátí ani na vyžádání,
 * a kdyby zůstal vybraný, žádal by se pořád znovu (nekonečná smyčka načítání stromu).
 */
const receive = (includeIds: number[], nodes: { id: number }[]) => ({
    type: types.FUND_FUND_TREE_RECEIVE,
    area: types.FUND_TREE_AREA_MAIN,
    versionId: 1,
    nodeId: null as number | null,
    expandedIds: {},
    includeIds,
    nodes,
    expandedIdsExtension: [] as number[],
    receivedAt: 0,
});

describe('fundTree reducer', () => {
    it('drops a selected node the server did not return', () => {
        const state = { ...fundTree(undefined, { type: 'init' }), selectedId: 234643 };

        const next = fundTree(state, receive([234643], [{ id: 1 }]));

        expect(next.selectedId).toBeNull();
    });

    it('keeps a selected node that came back', () => {
        const state = { ...fundTree(undefined, { type: 'init' }), selectedId: 2 };

        const next = fundTree(state, receive([2], [{ id: 1 }, { id: 2 }]));

        expect(next.selectedId).toBe(2);
    });

    it('drops only the missing nodes of a multiple selection', () => {
        const state = { ...fundTree(undefined, { type: 'init' }), multipleSelection: true, selectedIds: { 2: true, 9: true } };

        const next = fundTree(state, receive([2, 9], [{ id: 2 }]));

        expect(next.selectedIds).toEqual({ 2: true });
    });
});
