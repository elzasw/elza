import { describe, it, expect, vi, beforeEach } from 'vitest';
import * as types from 'actions/constants/ActionTypes';
import { WebApi } from 'actions/index';
import { fundTreeNodeExpand, fundTreeNodesExpand } from './fundTree';

vi.mock('actions/index', () => ({ WebApi: { getFundTree: vi.fn() } }));
// unused by the tested actions; importing them leads back to the store and its reducers
vi.mock('./node', () => ({ fundSelectSubNode: vi.fn() }));
vi.mock('components/arr/ArrUtils', () => ({ createFundRoot: vi.fn() }));

// 1 -> 2 (collapsed, 3 was expanded below it), 1 -> 5 (collapsed, 6 was expanded below it)
const fundTreeCopy = {
    expandedIds: { 1: true },
    collapsedExpandedIds: [
        { nodeId: 2, expandedIds: [3] },
        { nodeId: 5, expandedIds: [6] },
    ],
    nodes: [
        { id: 1, depth: 1 },
        { id: 2, depth: 2 },
        { id: 5, depth: 2 },
    ],
};
const getState = () => ({ arrRegion: { globalFundTree: { versionId: 7, fundTreeCopy } } });
const dispatch = (action: unknown): unknown => (typeof action === 'function' ? action(dispatch, getState) : action);

describe('fundTree expand actions', () => {
    beforeEach(() => {
        vi.mocked(WebApi.getFundTree).mockReset().mockResolvedValue({ nodes: [], expandedIdsExtension: [] });
    });

    it('sends the remembered expanded descendants of the expanded node', async () => {
        await fundTreeNodeExpand(types.FUND_TREE_AREA_COPY, { id: 2, depth: 2 })(dispatch, getState);

        expect(WebApi.getFundTree).toHaveBeenCalledWith(7, 2, { 1: true, 2: true, 3: true });
    });

    it('sends the remembered expanded descendants of every expanded node', async () => {
        await fundTreeNodesExpand(types.FUND_TREE_AREA_COPY, [
            { id: 2, depth: 2 },
            { id: 5, depth: 2 },
        ])(dispatch, getState);

        expect(WebApi.getFundTree).toHaveBeenCalledWith(7, null, { 1: true, 2: true, 3: true, 5: true, 6: true }, []);
    });
});
