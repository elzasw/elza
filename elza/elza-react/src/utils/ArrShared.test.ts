import { describe, it, expect, vi, beforeEach } from 'vitest';

/**
 * Mock the Api object so the test doesn't pull in the full Redux/component
 * graph (which has a load-order issue around `getServerContextPath()` being
 * called at module top-level in MapPage). The unit under test only needs
 * to route to the right `Api.node.*` method based on identifier shape.
 */
// vi.hoisted lifts these declarations above the hoisted vi.mock call, so the
// factory can capture them without hitting the const-TDZ.
const {
    mockGetNodeInfoByUuid,
    mockGetNodeInfoById,
    mockNodeGetNodeData,
    mockGetFundDetail,
    mockSelectFundTab,
    mockFundsSelectFund,
    mockFundSelectSubNode,
} = vi.hoisted(() => ({
    mockGetNodeInfoByUuid: vi.fn(),
    mockGetNodeInfoById: vi.fn(),
    mockNodeGetNodeData: vi.fn(),
    mockGetFundDetail: vi.fn(),
    mockSelectFundTab: vi.fn(),
    mockFundsSelectFund: vi.fn(),
    mockFundSelectSubNode: vi.fn(),
}));

vi.mock('../api', () => ({
    Api: {
        node: {
            nodeGetNodeInfoByUuid: mockGetNodeInfoByUuid,
            nodeGetNodeInfoById: mockGetNodeInfoById,
            nodeGetNodeData: mockNodeGetNodeData,
        },
    },
}));

vi.mock('../actions/WebApi', () => ({
    WebApi: {
        getFundDetail: mockGetFundDetail,
    },
}));

vi.mock('../actions/arr/fund', () => ({
    selectFundTab: mockSelectFundTab,
}));

vi.mock('../actions/fund/fund', () => ({
    fundsSelectFund: mockFundsSelectFund,
}));

vi.mock('../actions/arr/node', () => ({
    fundSelectSubNode: mockFundSelectSubNode,
}));

vi.mock('../components/arr/ArrUtils', () => ({
    createFundRoot: (fund) => ({ id: 'ROOT', fundId: fund.id }),
    getFundFromFundAndVersion: (fund, version) => ({
        ...fund,
        versionId: version.id,
        lockDate: version.lockDate,
        activeVersion: version,
        closed: version.lockDate != null,
    }),
}));

import { fetchNodeInfo } from './fetchNodeInfo';
import { processNodeNavigation, resolveFundTab } from './ArrShared';

const NODE_INFO_FIXTURE = {
    id: 42,
    version: 7,
    uuid: 'a4f1b2c3-1111-2222-3333-444455556666',
    fundId: 5,
    fundVersionId: 11,
};

describe('fetchNodeInfo', () => {
    beforeEach(() => {
        mockGetNodeInfoByUuid.mockReset();
        mockGetNodeInfoById.mockReset();
        mockGetNodeInfoByUuid.mockResolvedValue({ data: NODE_INFO_FIXTURE });
        mockGetNodeInfoById.mockResolvedValue({ data: NODE_INFO_FIXTURE });
    });

    it('routes a 36-char UUID to nodeGetNodeInfoByUuid', async () => {
        const result = await fetchNodeInfo(NODE_INFO_FIXTURE.uuid);

        expect(mockGetNodeInfoByUuid).toHaveBeenCalledTimes(1);
        expect(mockGetNodeInfoByUuid).toHaveBeenCalledWith(NODE_INFO_FIXTURE.uuid);
        expect(mockGetNodeInfoById).not.toHaveBeenCalled();
        expect(result).toEqual(NODE_INFO_FIXTURE);
    });

    it('routes a numeric string to nodeGetNodeInfoById after parseInt', async () => {
        const result = await fetchNodeInfo('42');

        expect(mockGetNodeInfoById).toHaveBeenCalledTimes(1);
        expect(mockGetNodeInfoById).toHaveBeenCalledWith(42);
        expect(mockGetNodeInfoByUuid).not.toHaveBeenCalled();
        expect(result).toEqual(NODE_INFO_FIXTURE);
    });
});

describe('resolveFundTab', () => {
    const FUND_DETAIL = {
        id: 42,
        name: 'Fund',
        versions: [
            { id: 462, lockDate: null },
            { id: 42, lockDate: '2020-01-01T00:00:00' },
        ],
    };

    beforeEach(() => {
        mockGetFundDetail.mockReset();
        mockSelectFundTab.mockReset();
        mockGetFundDetail.mockResolvedValue(FUND_DETAIL);
        mockSelectFundTab.mockImplementation((fund) => ({ type: 'SELECT_FUND_TAB', fund }));
    });

    it('replaces a restored tab whose version has been approved (closed) since it was saved', async () => {
        // the persisted tab still claims version 42 is open (lockDate null)
        const staleTab = { id: 42, versionId: 42, activeVersion: { id: 42, lockDate: null } };
        const dispatch = vi.fn();

        const result = await resolveFundTab(dispatch, staleTab, 42);

        expect(mockGetFundDetail).toHaveBeenCalledWith(42);
        expect(mockSelectFundTab).toHaveBeenCalledTimes(1);
        expect(mockSelectFundTab.mock.calls[0][0]).toMatchObject({ id: 42, versionId: 462 });
        expect(dispatch).toHaveBeenCalledWith({ type: 'SELECT_FUND_TAB', fund: expect.anything() });
        expect(result).toBe(FUND_DETAIL);
    });

    it('keeps the tab when the server confirms its version is still the open one', async () => {
        const tab = { id: 42, versionId: 462, activeVersion: { id: 462, lockDate: null } };
        const dispatch = vi.fn();

        const result = await resolveFundTab(dispatch, tab, 42);

        expect(mockGetFundDetail).toHaveBeenCalledWith(42);
        expect(mockSelectFundTab).not.toHaveBeenCalled();
        expect(dispatch).not.toHaveBeenCalled();
        expect(result).toBe(tab);
    });

    it('skips the server round-trip only for a URL-pinned version already displayed', async () => {
        const tab = { id: 42, versionId: 42, activeVersion: { id: 42, lockDate: '2020-01-01T00:00:00' } };
        const dispatch = vi.fn();

        const result = await resolveFundTab(dispatch, tab, 42, 42);

        expect(mockGetFundDetail).not.toHaveBeenCalled();
        expect(dispatch).not.toHaveBeenCalled();
        expect(result).toBe(tab);
    });

    it('selects the pinned version when it is not the displayed one', async () => {
        const tab = { id: 42, versionId: 462, activeVersion: { id: 462, lockDate: null } };
        const dispatch = vi.fn();

        await resolveFundTab(dispatch, tab, 42, 42);

        expect(mockGetFundDetail).toHaveBeenCalledWith(42);
        expect(mockSelectFundTab.mock.calls[0][0]).toMatchObject({ id: 42, versionId: 42 });
    });

    it('throws when the pinned version does not exist', async () => {
        const dispatch = vi.fn();

        await expect(resolveFundTab(dispatch, null, 42, 999)).rejects.toThrow(/Fund version not found/);
        expect(mockSelectFundTab).not.toHaveBeenCalled();
    });
});

describe('processNodeNavigation', () => {
    const TARGET_NODE_INFO = { ...NODE_INFO_FIXTURE, id: 6430, fundId: 42, fundVersionId: 462 };

    const OTHER_FUND_TAB = {
        id: 1,
        versionId: 1,
        activeVersion: { id: 1, lockDate: null },
        fundTree: { fetched: true },
    };

    const TARGET_FUND_DETAIL = {
        id: 42,
        name: 'Target fund',
        versions: [
            { id: 462, lockDate: null },
            { id: 42, lockDate: '2020-01-01T00:00:00' },
        ],
    };

    /**
     * Minimal store double: thunks are invoked, the SELECT_FUND_TAB marker
     * action (returned by the mocked selectFundTab) replaces the single arr
     * tab with a tree already marked as fetched, so waitForLoadAS proceeds
     * on its first synchronous pass and the test needs no timers.
     */
    const createStore = (initialTab) => {
        const state = {
            arrRegion: {
                activeIndex: initialTab ? 0 : null,
                funds: initialTab ? [initialTab] : [],
            },
        };
        const dispatched = [];
        const dispatch = (action) => {
            if (typeof action === 'function') {
                return action(dispatch, () => state);
            }
            dispatched.push(action);
            if (action?.type === 'SELECT_FUND_TAB') {
                state.arrRegion = {
                    activeIndex: 0,
                    funds: [{ ...action.fund, fundTree: { fetched: true } }],
                };
            }
            return action;
        };
        return { state, dispatch, getState: () => state, dispatched };
    };

    beforeEach(() => {
        mockNodeGetNodeData.mockReset();
        mockGetFundDetail.mockReset();
        mockSelectFundTab.mockReset();
        mockFundsSelectFund.mockReset();
        mockFundSelectSubNode.mockReset();

        mockFundsSelectFund.mockReturnValue({ type: 'FUNDS_SELECT_FUND' });
        mockSelectFundTab.mockImplementation((fund) => ({ type: 'SELECT_FUND_TAB', fund }));
        mockFundSelectSubNode.mockReturnValue({ type: 'FUND_SELECT_SUBNODE' });
        mockGetFundDetail.mockResolvedValue(TARGET_FUND_DETAIL);
        mockNodeGetNodeData.mockResolvedValue({ data: { parents: [{ id: 6422 }] } });
    });

    it('loads the target fund and uses its open version when the active tab belongs to another fund', async () => {
        const { dispatch, getState } = createStore(OTHER_FUND_TAB);

        await processNodeNavigation(TARGET_NODE_INFO)(dispatch, getState);

        expect(mockGetFundDetail).toHaveBeenCalledWith(42);
        expect(mockSelectFundTab).toHaveBeenCalledTimes(1);
        expect(mockSelectFundTab.mock.calls[0][0]).toMatchObject({ id: 42, versionId: 462 });

        await vi.waitFor(() => expect(mockFundSelectSubNode).toHaveBeenCalled());
        expect(mockNodeGetNodeData).toHaveBeenCalledWith(
            expect.objectContaining({ fundVersionId: 462, nodeId: 6430 }),
        );
        expect(mockFundSelectSubNode.mock.calls[0].slice(0, 3)).toEqual([462, 6430, { id: 6422 }]);
    });

    it('keeps the active tab when it already belongs to the node fund', async () => {
        const { dispatch, getState } = createStore({
            id: 42,
            versionId: 462,
            activeVersion: { id: 462, lockDate: null },
            fundTree: { fetched: true },
        });

        await processNodeNavigation(TARGET_NODE_INFO)(dispatch, getState);

        expect(mockGetFundDetail).not.toHaveBeenCalled();
        expect(mockSelectFundTab).not.toHaveBeenCalled();

        await vi.waitFor(() => expect(mockFundSelectSubNode).toHaveBeenCalled());
        expect(mockNodeGetNodeData).toHaveBeenCalledWith(
            expect.objectContaining({ fundVersionId: 462, nodeId: 6430 }),
        );
    });

    it('reloads the fund when the URL requests a different version than the active tab', async () => {
        const { dispatch, getState } = createStore({
            id: 42,
            versionId: 462,
            activeVersion: { id: 462, lockDate: null },
            fundTree: { fetched: true },
        });

        await processNodeNavigation(TARGET_NODE_INFO, 42)(dispatch, getState);

        expect(mockGetFundDetail).toHaveBeenCalledWith(42);
        expect(mockSelectFundTab.mock.calls[0][0]).toMatchObject({ id: 42, versionId: 42 });

        await vi.waitFor(() => expect(mockFundSelectSubNode).toHaveBeenCalled());
        expect(mockNodeGetNodeData).toHaveBeenCalledWith(
            expect.objectContaining({ fundVersionId: 42, nodeId: 6430 }),
        );
    });

    it('selects the fund root as parent when the node has no parents', async () => {
        mockNodeGetNodeData.mockResolvedValue({ data: { parents: [] } });
        const { dispatch, getState } = createStore(OTHER_FUND_TAB);

        await processNodeNavigation({ ...TARGET_NODE_INFO, id: 6422 })(dispatch, getState);

        await vi.waitFor(() => expect(mockFundSelectSubNode).toHaveBeenCalled());
        expect(mockFundSelectSubNode.mock.calls[0].slice(0, 3)).toEqual([
            462, 6422, { id: 'ROOT', fundId: 42 },
        ]);
    });
});
