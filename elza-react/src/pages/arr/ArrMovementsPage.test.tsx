import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import { delay, http, HttpResponse } from 'msw';
import PropTypes from 'prop-types';
import React, { PropsWithChildren, useLayoutEffect } from 'react';
import { Route, useHistory } from 'react-router-dom';
import type { Action, History } from 'history';

import { renderWithProviders, fireEvent, createTestStore, act, within, waitFor } from 'test/test-utils';
import type { TestStore } from 'test/test-utils';
import { server } from 'test/mocks/server';
import { fundInitState } from 'stores/app/arr/fund';
import * as types from 'actions/constants/ActionTypes';
import { addShortcutManager } from 'components/Utils';
import type { Fund } from 'typings/store';
import type { TreeNodeVO } from 'typings/Outputs';
import { MOVEMENTS, URL_FUND } from '../../constants';
import ArrParentPageKeymap from './ArrParentPageKeymap';
import ArrMovementsPage from './ArrMovementsPage';

/**
 * Movements page: the query ?l=<leftNodeId>&r=<rightNodeId> and the selection in the two movements trees
 * are kept in sync in both directions.
 */

const FUND_ID = 5;
const VERSION_ID = 7;

interface FundTreeRequest {
    versionId: number;
    nodeId: number | null;
    includeIds: number[];
    expandedIds: string[];
}

interface MovementsTree {
    selectedIds: Record<number, boolean>;
    selectedId: number | null;
    fetched: boolean;
}

type TestFund = Fund & { fundTreeMovementsLeft: MovementsTree; fundTreeMovementsRight: MovementsTree };

interface TestState {
    arrRegion: { funds: TestFund[] };
}

interface UrlChange {
    search: string;
    action: Action;
}

type TreeSide = 'left' | 'right';

function treeNode(id: number, depth: number, name: string, hasChildren: boolean): TreeNodeVO {
    return { id, depth, name, hasChildren, icon: 'fa-folder', referenceMark: [], version: 1, arrPerm: true };
}

const rootNodes: TreeNodeVO[] = [
    treeNode(1, 1, 'Fond Testovací', true),
    treeNode(2, 2, 'Série A', true),
    treeNode(3, 2, 'Série B', false),
];
const childNodes: TreeNodeVO[] = [treeNode(4, 3, 'Spis 1', false)];

/** Layout provides the shortcut manager to the Shortcuts rendered by ArrParentPage through legacy context. */
class ShortcutsProvider extends React.Component<PropsWithChildren> {
    static childContextTypes = { shortcuts: PropTypes.object.isRequired };
    shortcutManager: unknown;

    constructor(props: PropsWithChildren) {
        super(props);
        addShortcutManager(this, ArrParentPageKeymap);
    }

    getChildContext() {
        return { shortcuts: this.shortcutManager };
    }

    render() {
        return this.props.children;
    }
}

let testHistory: History | undefined;
let urlChanges: UrlChange[];

/**
 * Records every history change synchronously, so two replaces in one render are counted twice.
 * Rendered before the page, its layout effect subscribes before the page's componentDidMount runs.
 */
function HistoryProbe(): null {
    const history = useHistory();
    useLayoutEffect(() => {
        testHistory = history;
        return history.listen((location, action) => {
            urlChanges.push({ search: location.search, action });
        });
    }, [history]);
    return null;
}

function currentUrl(): string {
    if (!testHistory) {
        throw new Error('Historie routeru není zachycena');
    }
    return testHistory.location.pathname + testHistory.location.search;
}

function currentSearch(): string {
    if (!testHistory) {
        throw new Error('Historie routeru není zachycena');
    }
    return testHistory.location.search;
}

interface StoreOptions {
    left?: number[];
    right?: number;
    versionId?: number;
    /** A locked version, set on the tab the way getFundFromFundAndVersion does. */
    lockDate?: string;
}

function createStoreWithFund(options?: StoreOptions): TestStore {
    const base = createTestStore().getState() as { arrRegion: object };
    const versionId = options?.versionId ?? VERSION_ID;
    const lockDate = options?.lockDate ?? null;
    const fund: TestFund = fundInitState({
        id: FUND_ID,
        versionId,
        name: 'Fond Testovací',
        closed: lockDate != null,
        lockDate,
        activeVersion: { id: versionId, lockDate, issues: [] },
    });
    const leftSelectedIds: Record<number, boolean> = {};
    options?.left?.forEach((nodeId) => {
        leftSelectedIds[nodeId] = true;
    });
    const fundWithSelection: TestFund = {
        ...fund,
        fundTreeMovementsLeft: { ...fund.fundTreeMovementsLeft, selectedIds: leftSelectedIds },
        fundTreeMovementsRight: { ...fund.fundTreeMovementsRight, selectedId: options?.right ?? null },
    };
    return createTestStore({
        arrRegion: { ...base.arrRegion, activeIndex: 0, funds: [fundWithSelection] },
        userDetail: {
            id: 1,
            username: 'test',
            userPermissions: {},
            permissionsMap: { FUND_ARR_ALL: { permission: 'FUND_ARR_ALL' } },
            authTypes: [] as string[],
            fetched: true,
            fetching: false,
        },
    });
}

/** Spies on the store before rendering, so the dispatch handed to the page and the trees is the spy. */
function spyOnSelectActions(store: TestStore) {
    const dispatchSpy = vi.spyOn(store, 'dispatch');
    return () =>
        dispatchSpy.mock.calls
            .map(([action]) => action)
            .filter((action) => action.type === types.FUND_FUND_TREE_SELECT_NODE)
            .map((action) => ({ area: action.area, nodeId: action.nodeId }));
}

function activeFund(store: TestStore): TestFund {
    return (store.getState() as TestState).arrRegion.funds[0];
}

function treeContainer(container: HTMLElement, side: TreeSide): HTMLElement {
    const element = container.querySelector(`.tree-${side}-container`);
    if (!(element instanceof HTMLElement)) {
        throw new Error(`Chybí strom přesunů ${side}`);
    }
    return element;
}

function nodeElement(container: HTMLElement, side: TreeSide, name: string): HTMLElement {
    const element = within(treeContainer(container, side)).getByText(name).closest('.node');
    if (!(element instanceof HTMLElement)) {
        throw new Error(`Uzel ${name} není ve stromu ${side} vykreslen`);
    }
    return element;
}

let treeRequests: FundTreeRequest[];

beforeEach(() => {
    // jsdom lays nothing out; VirtualList renders rows only into a container with a height
    vi.spyOn(HTMLElement.prototype, 'clientHeight', 'get').mockImplementation(function (this: HTMLElement) {
        return this.classList.contains('fa-tree-lazy-container') ? 600 : 0;
    });

    treeRequests = [];
    urlChanges = [];
    testHistory = undefined;
    server.use(
        http.get(`/api/arrangement/getFund/${FUND_ID}`, () =>
            HttpResponse.json({ id: FUND_ID, name: 'Fond Testovací', versions: [{ id: VERSION_ID, lockDate: null }] })
        ),
        // held open: once the item types arrive, the page would also load the description item groups
        http.get('/api/rule/descItemTypes', async () => {
            await delay('infinite');
            return HttpResponse.json([]);
        }),
        http.post('/api/arrangement/fundTree', async ({ request }) => {
            const body = (await request.json()) as FundTreeRequest;
            treeRequests.push(body);
            if (body.includeIds.includes(4)) {
                const nodesWithPath = [rootNodes[0], rootNodes[1], ...childNodes, rootNodes[2]];
                return HttpResponse.json({ nodes: nodesWithPath, expandedIdsExtension: [1, 2] });
            }
            return HttpResponse.json({ nodes: rootNodes, expandedIdsExtension: [1] });
        })
    );
});

afterEach(() => {
    vi.restoreAllMocks();
});

const MOVEMENTS_PATH = `${URL_FUND}/:id/${MOVEMENTS}`;
const VERSION_MOVEMENTS_PATH = `${URL_FUND}/:id/v/:versionId/${MOVEMENTS}`;

async function renderPage(route: string, store: TestStore, path = MOVEMENTS_PATH) {
    const result = renderWithProviders(
        <ShortcutsProvider>
            <HistoryProbe />
            <Route path={path} component={ArrMovementsPage} />
        </ShortcutsProvider>,
        { store, route }
    );
    await within(treeContainer(result.container, 'left')).findByText('Série B');
    await within(treeContainer(result.container, 'right')).findByText('Série B');
    // act flushes the effects that react to the received trees; a refetch would be dispatched there
    await act(async () => {});
    return result;
}

describe('ArrMovementsPage – výběr ve stromech a URL', () => {
    it('uzly z URL vybere v levém i pravém stromu a nenačtený uzel dotáhne přes includeIds', async () => {
        const store = createStoreWithFund();
        const { container } = await renderPage(`/fund/${FUND_ID}/movements?l=4&r=3`, store);

        expect(activeFund(store).fundTreeMovementsLeft.selectedIds).toEqual({ 4: true });
        expect(activeFund(store).fundTreeMovementsRight.selectedId).toBe(3);
        expect(treeRequests).toContainEqual(expect.objectContaining({ versionId: VERSION_ID, includeIds: [4] }));
        expect(nodeElement(container, 'left', 'Spis 1')).toHaveClass('active');
        expect(nodeElement(container, 'right', 'Série B')).toHaveClass('active');
    });

    it('klik v pravém stromu zapíše do URL r, jediný uzel vybraný vlevo zapíše l', async () => {
        const store = createStoreWithFund();
        const { container } = await renderPage(`/fund/${FUND_ID}/movements`, store);
        expect(currentSearch()).toBe('');

        fireEvent.click(nodeElement(container, 'right', 'Série A'));
        expect(currentSearch()).toBe('?r=2');

        fireEvent.click(nodeElement(container, 'left', 'Série B'));
        expect(currentSearch()).toBe('?l=3&r=2');

        expect(urlChanges).toEqual([
            { search: '?r=2', action: 'REPLACE' },
            { search: '?l=3&r=2', action: 'REPLACE' },
        ]);
    });

    it('výběr více uzlů vlevo přes ctrl+klik odebere z URL l a r ponechá', async () => {
        const store = createStoreWithFund();
        const { container } = await renderPage(`/fund/${FUND_ID}/movements?l=2&r=3`, store);
        expect(activeFund(store).fundTreeMovementsLeft.selectedIds).toEqual({ 2: true });

        fireEvent.click(nodeElement(container, 'left', 'Série B'), { ctrlKey: true });

        expect(activeFund(store).fundTreeMovementsLeft.selectedIds).toEqual({ 2: true, 3: true });
        expect(currentSearch()).toBe('?r=3');
    });

    it('URL shodná s výběrem se nepřepisuje a výběr se z ní znovu neaplikuje', async () => {
        const store = createStoreWithFund();
        const selectActions = spyOnSelectActions(store);
        const { container } = await renderPage(`/fund/${FUND_ID}/movements?l=4&r=3`, store);

        expect(urlChanges).toEqual([]);
        expect(currentSearch()).toBe('?l=4&r=3');
        expect(selectActions()).toEqual([
            { area: types.FUND_TREE_AREA_MOVEMENTS_LEFT, nodeId: 4 },
            { area: types.FUND_TREE_AREA_MOVEMENTS_RIGHT, nodeId: 3 },
        ]);
        // main tree, left tree with the URL node 4, right tree with the URL node 3
        const includeIds = treeRequests.map((request) => request.includeIds);
        expect(includeIds).toHaveLength(3);
        expect(includeIds).toEqual(expect.arrayContaining([[], [4], [3]]));

        fireEvent.click(nodeElement(container, 'right', 'Série A'));
        await act(async () => {});
        await act(async () => {});

        expect(urlChanges).toEqual([{ search: '?l=4&r=2', action: 'REPLACE' }]);
        expect(selectActions()).toHaveLength(3);
        expect(activeFund(store).fundTreeMovementsLeft.selectedIds).toEqual({ 4: true });
        expect(activeFund(store).fundTreeMovementsRight.selectedId).toBe(2);
        expect(treeRequests).toHaveLength(3);
    });

    it('URL doplněná o výběr ze store se zapíše jednou a pak zůstane stabilní', async () => {
        const store = createStoreWithFund({ left: [2] });
        const selectActions = spyOnSelectActions(store);
        await renderPage(`/fund/${FUND_ID}/movements?r=3`, store);
        await act(async () => {});

        expect(urlChanges).toEqual([{ search: '?l=2&r=3', action: 'REPLACE' }]);
        expect(selectActions()).toEqual([{ area: types.FUND_TREE_AREA_MOVEMENTS_RIGHT, nodeId: 3 }]);
        expect(activeFund(store).fundTreeMovementsLeft.selectedIds).toEqual({ 2: true });
        expect(activeFund(store).fundTreeMovementsRight.selectedId).toBe(3);
        expect(treeRequests).toHaveLength(3);
    });

    it('bez parametrů v URL ponechá výběr ze store a zapíše ho do URL', async () => {
        const store = createStoreWithFund({ left: [2], right: 3 });
        const { container } = await renderPage(`/fund/${FUND_ID}/movements`, store);

        expect(activeFund(store).fundTreeMovementsLeft.selectedIds).toEqual({ 2: true });
        expect(activeFund(store).fundTreeMovementsRight.selectedId).toBe(3);
        expect(currentSearch()).toBe('?l=2&r=3');
        expect(urlChanges).toEqual([{ search: '?l=2&r=3', action: 'REPLACE' }]);
        expect(nodeElement(container, 'left', 'Série A')).toHaveClass('active');
        expect(nodeElement(container, 'right', 'Série B')).toHaveClass('active');
    });

    it('záložku se zastaralou verzí nahradí otevřenou verzí a výběr z URL přenese do ní', async () => {
        const outdatedVersionId = 6;
        server.use(
            http.get(`/api/arrangement/getFund/${FUND_ID}`, () =>
                HttpResponse.json({
                    id: FUND_ID,
                    name: 'Fond Testovací',
                    versions: [
                        { id: VERSION_ID, lockDate: null },
                        { id: outdatedVersionId, lockDate: '2026-01-01T00:00:00' },
                    ],
                })
            )
        );
        const store = createStoreWithFund({ versionId: outdatedVersionId });
        const { container } = await renderPage(`/fund/${FUND_ID}/movements?l=4&r=3`, store);

        await waitFor(() => {
            expect(activeFund(store).versionId).toBe(VERSION_ID);
            expect(activeFund(store).fundTreeMovementsLeft.fetched).toBe(true);
            expect(activeFund(store).fundTreeMovementsRight.fetched).toBe(true);
        });
        await act(async () => {});

        expect(activeFund(store).fundTreeMovementsLeft.selectedIds).toEqual({ 4: true });
        expect(activeFund(store).fundTreeMovementsRight.selectedId).toBe(3);
        expect(treeRequests).toContainEqual(expect.objectContaining({ versionId: VERSION_ID, includeIds: [4] }));
        expect(treeRequests).toContainEqual(expect.objectContaining({ versionId: VERSION_ID, includeIds: [3] }));
        expect(currentSearch()).toBe('?l=4&r=3');
        expect(urlChanges).toEqual([]);
        expect(nodeElement(container, 'left', 'Spis 1')).toHaveClass('active');
        expect(nodeElement(container, 'right', 'Série B')).toHaveClass('active');
    });

    it('neplatné l z URL výběr vlevo nemění a URL jedním přepsáním srovná se skutečným výběrem', async () => {
        const store = createStoreWithFund({ left: [2] });
        const selectActions = spyOnSelectActions(store);
        await renderPage(`/fund/${FUND_ID}/movements?l=abc&r=3`, store);

        expect(selectActions()).toEqual([{ area: types.FUND_TREE_AREA_MOVEMENTS_RIGHT, nodeId: 3 }]);
        expect(activeFund(store).fundTreeMovementsLeft.selectedIds).toEqual({ 2: true });
        expect(activeFund(store).fundTreeMovementsRight.selectedId).toBe(3);
        expect(urlChanges).toEqual([{ search: '?l=2&r=3', action: 'REPLACE' }]);
    });

    it('uzel z URL, který server ve stromu nevrátí (smazaný), z výběru vypadne a z URL zmizí l', async () => {
        const store = createStoreWithFund();
        await renderPage(`/fund/${FUND_ID}/movements?l=99`, store);

        expect(activeFund(store).fundTreeMovementsLeft.selectedIds).toEqual({});
        expect(currentSearch()).toBe('');
        expect(urlChanges).toEqual([{ search: '', action: 'REPLACE' }]);
        // the missing node is asked for once, not again after every response
        expect(treeRequests.filter((request) => request.includeIds.includes(99))).toHaveLength(1);
    });

    it('v uzavřené verzi z cesty /v/:versionId vybere uzel z URL a adresu ponechá', async () => {
        const closedVersionId = 6;
        server.use(
            http.get(`/api/arrangement/getFund/${FUND_ID}`, () =>
                HttpResponse.json({
                    id: FUND_ID,
                    name: 'Fond Testovací',
                    versions: [
                        { id: VERSION_ID, lockDate: null },
                        { id: closedVersionId, lockDate: '2026-01-01T00:00:00' },
                    ],
                })
            )
        );
        const store = createStoreWithFund({ versionId: closedVersionId, lockDate: '2026-01-01T00:00:00' });
        const { container } = await renderPage(
            `/fund/${FUND_ID}/v/${closedVersionId}/movements?l=4`,
            store,
            VERSION_MOVEMENTS_PATH
        );

        expect(activeFund(store)).toMatchObject({ versionId: closedVersionId, closed: true });
        expect(activeFund(store).fundTreeMovementsLeft.selectedIds).toEqual({ 4: true });
        expect(treeRequests).toContainEqual(expect.objectContaining({ versionId: closedVersionId, includeIds: [4] }));
        expect(currentUrl()).toBe(`/fund/${FUND_ID}/v/${closedVersionId}/movements?l=4`);
        expect(urlChanges).toEqual([]);
        expect(nodeElement(container, 'left', 'Spis 1')).toHaveClass('active');
    });
});
