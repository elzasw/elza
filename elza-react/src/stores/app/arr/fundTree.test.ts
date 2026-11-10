import { describe, it, expect, vi } from 'vitest';
import * as types from 'actions/constants/ActionTypes';
import fundTree, { COLLAPSED_EXPANDED_LIMIT } from './fundTree';

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

const tree = (nodes: { id: number; depth: number }[], expandedIds: Record<number, boolean>) => ({
    ...fundTree(undefined, { type: 'init' }),
    nodes,
    expandedIds,
});

const collapse = (id: number, depth: number) => ({ type: types.FUND_FUND_TREE_COLLAPSE_NODE, node: { id, depth } });

describe('fundTree reducer - expanded descendants of a collapsed node', () => {
    // 1 -> 2 -> 3 -> 4, 1 -> 5
    const nodes = [
        { id: 1, depth: 1 },
        { id: 2, depth: 2 },
        { id: 3, depth: 3 },
        { id: 4, depth: 4 },
        { id: 5, depth: 2 },
    ];
    const collapsedState = () => fundTree(tree(nodes, { 1: true, 2: true, 3: true }), collapse(2, 2));

    it('moves them out of expandedIds into the memory', () => {
        const next = collapsedState();

        expect(next.expandedIds).toEqual({ 1: true });
        expect(next.collapsedExpandedIds).toEqual([{ nodeId: 2, expandedIds: [3] }]);
        expect(next.nodes).toEqual([
            { id: 1, depth: 1 },
            { id: 2, depth: 2 },
            { id: 5, depth: 2 },
        ]);
    });

    it('forgets them once the node is expanded again', () => {
        const next = fundTree(collapsedState(), { type: types.FUND_FUND_TREE_EXPAND_NODE, node: { id: 2, depth: 2 } });

        expect(next.collapsedExpandedIds).toEqual([]);
    });

    it('forgets them when the server expands the node', () => {
        const next = fundTree(collapsedState(), {
            ...receive([], [{ id: 1 }, { id: 2 }, { id: 3 }]),
            expandedIdsExtension: [1, 2],
        });

        expect(next.collapsedExpandedIds).toEqual([]);
    });

    it('clears the memory when the whole tree is collapsed', () => {
        const next = fundTree(collapsedState(), { type: types.FUND_FUND_TREE_COLLAPSE });

        expect(next.collapsedExpandedIds).toEqual([]);
    });

    it('keeps the memory through save and load', () => {
        const saved = fundTree(collapsedState(), { type: types.STORE_SAVE });

        const loaded = fundTree(saved, { type: types.STORE_LOAD });

        expect(loaded.collapsedExpandedIds).toEqual([{ nodeId: 2, expandedIds: [3] }]);
    });

    it('loads state saved without the memory', () => {
        const saved = fundTree(tree(nodes, { 1: true }), { type: types.STORE_SAVE });
        delete saved.collapsedExpandedIds;

        const loaded = fundTree(saved, { type: types.STORE_LOAD });

        expect(loaded.collapsedExpandedIds).toEqual([]);
    });

    it(`keeps only the newest ${COLLAPSED_EXPANDED_LIMIT} entries`, () => {
        const parentIds = Array.from({ length: COLLAPSED_EXPANDED_LIMIT + 1 }, (_, position) => 100 + position);
        const wideNodes = parentIds.flatMap((id) => [
            { id, depth: 2 },
            { id: id + 1000, depth: 3 },
        ]);
        const expandedIds = Object.fromEntries(wideNodes.map((node) => [node.id, true]));

        let state = tree(wideNodes, expandedIds);
        parentIds.forEach((id) => {
            state = fundTree(state, collapse(id, 2));
        });

        expect(state.collapsedExpandedIds).toHaveLength(COLLAPSED_EXPANDED_LIMIT);
        expect(state.collapsedExpandedIds[0]).toEqual({ nodeId: 101, expandedIds: [1101] });
    });
});
