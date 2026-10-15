import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import { http, HttpResponse } from 'msw';
import React, { isValidElement } from 'react';
import { useSelector } from 'react-redux';

import { renderWithProviders, screen, fireEvent, createTestStore, act, waitFor } from 'test/test-utils';
import type { TestStore } from 'test/test-utils';
import { server } from 'test/mocks/server';
import { fundInitState } from 'stores/app/arr/fund';
import { fundTreeInvalidate, fundTreeSelectNode } from 'actions/arr/fundTree';
import * as types from 'actions/constants/ActionTypes';
import type { Fund } from 'typings/store';
import type { TreeNodeVO } from 'typings/Outputs';
import { FundTreeMovementsLeft } from './FundTreeMovementsLeft';

/**
 * Left movements tree: loads on mount and again when the store invalidates it; click, expand,
 * collapse, search and context menu are reflected in the FUND_TREE_AREA_MOVEMENTS_LEFT tree store.
 */

const VERSION_ID = 7;

interface FundTreeRequest {
    versionId: number;
    nodeId: number | null;
    includeIds: number[];
    expandedIds: string[];
}

interface FulltextRequest {
    versionId: number;
    nodeId: number | null;
    searchValue: string;
}

interface MovementsTree {
    expandedIds: Record<number, boolean>;
    selectedIds: Record<number, boolean>;
    selectedId: number | null;
    focusId: number | null;
    nodes: TreeNodeVO[];
    fetched: boolean;
    isFetching: boolean;
    dirty: boolean;
    fetchingIncludeIds: Record<number, boolean>;
    multipleSelection: boolean;
    filterText: string | null;
}

type TestFund = Fund & { fundTreeMovementsLeft: MovementsTree };

interface TestState {
    arrRegion: { funds: TestFund[] };
    contextMenu: { visible: boolean; menu: unknown; position: { x: number; y: number } | null };
}

function treeNode(id: number, depth: number, name: string, hasChildren: boolean): TreeNodeVO {
    return { id, depth, name, hasChildren, icon: 'fa-folder', referenceMark: [], version: 1, arrPerm: true };
}

const rootNodes: TreeNodeVO[] = [
    treeNode(1, 1, 'Fond Testovací', true),
    treeNode(2, 2, 'Série A', true),
    treeNode(3, 2, 'Série B', false),
];
const childNodes: TreeNodeVO[] = [treeNode(4, 3, 'Spis 1', false)];

/** Renders the tree the way ArrMovementsPage does: fund, version and the whole tree store as props. */
function MovementsHost() {
    const fund = useSelector((state: TestState) => state.arrRegion.funds[0]);
    return <FundTreeMovementsLeft fund={fund} versionId={fund.versionId} {...fund.fundTreeMovementsLeft} />;
}

function createStoreWithFund(): TestStore {
    const base = createTestStore().getState() as { arrRegion: object };
    const fund: TestFund = fundInitState({ id: 5, versionId: VERSION_ID, name: 'Fond Testovací', closed: false });
    return createTestStore({ arrRegion: { ...base.arrRegion, activeIndex: 0, funds: [fund] } });
}

function movementsTree(store: TestStore): MovementsTree {
    return (store.getState() as TestState).arrRegion.funds[0].fundTreeMovementsLeft;
}

function nodeElement(name: string): HTMLElement {
    const element = screen.getByText(name).closest('.node');
    if (!(element instanceof HTMLElement)) {
        throw new Error(`Uzel ${name} není vykreslen`);
    }
    return element;
}

let treeRequests: FundTreeRequest[];
let fulltextRequests: FulltextRequest[];

beforeEach(() => {
    // jsdom lays nothing out; VirtualList renders rows only into a container with a height
    vi.spyOn(HTMLElement.prototype, 'clientHeight', 'get').mockImplementation(function (this: HTMLElement) {
        return this.classList.contains('fa-tree-lazy-container') ? 600 : 0;
    });

    treeRequests = [];
    fulltextRequests = [];
    server.use(
        http.post('/api/arrangement/fundTree', async ({ request }) => {
            const body = (await request.json()) as FundTreeRequest;
            treeRequests.push(body);
            if (body.nodeId === 2) {
                return HttpResponse.json({ nodes: childNodes, expandedIdsExtension: [] });
            }
            if (body.includeIds.includes(4)) {
                const nodesWithPath = [rootNodes[0], rootNodes[1], ...childNodes, rootNodes[2]];
                return HttpResponse.json({ nodes: nodesWithPath, expandedIdsExtension: [1, 2] });
            }
            return HttpResponse.json({ nodes: rootNodes, expandedIdsExtension: [1] });
        }),
        http.post('/api/arrangement/fulltext', async ({ request }) => {
            fulltextRequests.push((await request.json()) as FulltextRequest);
            return HttpResponse.json([
                { nodeId: 2, parent: null },
                { nodeId: 3, parent: null },
            ]);
        })
    );
});

afterEach(() => {
    vi.restoreAllMocks();
});

async function renderTree() {
    const store = createStoreWithFund();
    const result = renderWithProviders(<MovementsHost />, { store });
    await screen.findByText('Série A');
    return { ...result, store };
}

describe('FundTreeMovementsLeft', () => {
    it('při připojení načte celý strom jednou a vykreslí uzly', async () => {
        const { store } = await renderTree();

        expect(screen.getByText('Fond Testovací')).toBeInTheDocument();
        expect(screen.getByText('Série B')).toBeInTheDocument();
        // act flushes the effects that react to the received tree; a refetch would set isFetching when dispatched
        await act(async () => {});
        expect(movementsTree(store).isFetching).toBe(false);
        expect(treeRequests).toEqual([{ versionId: VERSION_ID, nodeId: null, includeIds: [], expandedIds: [] }]);
    });

    it('zneplatněný strom načte znovu', async () => {
        const { store } = await renderTree();

        act(() => {
            store.dispatch(fundTreeInvalidate(VERSION_ID));
        });

        await waitFor(() => expect(treeRequests).toHaveLength(2));
        expect(treeRequests[1]).toMatchObject({ versionId: VERSION_ID, nodeId: null, expandedIds: ['1'] });
        await waitFor(() => expect(movementsTree(store).dirty).toBe(false));
    });

    it('vybraný uzel, který ve stromu není načtený, dotáhne přes includeIds a rozbalí cestu k němu', async () => {
        const { store } = await renderTree();
        expect(screen.queryByText('Spis 1')).not.toBeInTheDocument();

        act(() => {
            store.dispatch(
                fundTreeSelectNode(types.FUND_TREE_AREA_MOVEMENTS_LEFT, VERSION_ID, 4, false, false, null, true)
            );
        });

        await screen.findByText('Spis 1');
        await act(async () => {});
        expect(treeRequests).toHaveLength(2);
        expect(treeRequests[1]).toMatchObject({ versionId: VERSION_ID, nodeId: null, includeIds: [4] });
        expect(movementsTree(store)).toMatchObject({
            isFetching: false,
            selectedIds: { 4: true },
            expandedIds: { 1: true, 2: true },
        });
        expect(nodeElement('Spis 1')).toHaveClass('active');
    });

    it('klik vybere uzel, ctrl+klik přidá další uzel téže úrovně, a strom se kvůli tomu znovu nenačítá', async () => {
        const { store } = await renderTree();

        fireEvent.click(nodeElement('Série A'));
        expect(movementsTree(store).selectedIds).toEqual({ 2: true });
        expect(nodeElement('Série A')).toHaveClass('active');

        fireEvent.click(nodeElement('Série B'), { ctrlKey: true });
        expect(movementsTree(store).selectedIds).toEqual({ 2: true, 3: true });

        fireEvent.click(nodeElement('Fond Testovací'), { ctrlKey: true });
        expect(movementsTree(store).selectedIds).toEqual({ 2: true, 3: true });

        expect(treeRequests).toHaveLength(1);
    });

    it('shift+klik vybere rozsah uzlů', async () => {
        const { store } = await renderTree();

        fireEvent.click(nodeElement('Série A'));
        fireEvent.click(nodeElement('Série B'), { shiftKey: true });

        expect(movementsTree(store).selectedIds).toEqual({ 2: true, 3: true });
    });

    it('rozbalí a zabalí uzel', async () => {
        const { store, container } = await renderTree();

        const toggle = nodeElement('Série A').querySelector('.exp-col');
        if (!toggle) {
            throw new Error('Uzel Série A nemá přepínač rozbalení');
        }
        fireEvent.click(toggle);

        expect(await screen.findByText('Spis 1')).toBeInTheDocument();
        expect(treeRequests[1]).toMatchObject({ versionId: VERSION_ID, nodeId: 2 });
        expect(movementsTree(store).expandedIds).toEqual({ 1: true, 2: true });

        const collapseToggle = nodeElement('Série A').querySelector('.exp-col');
        if (!collapseToggle) {
            throw new Error('Uzel Série A nemá přepínač rozbalení');
        }
        fireEvent.click(collapseToggle);

        await waitFor(() => expect(screen.queryByText('Spis 1')).not.toBeInTheDocument());
        expect(movementsTree(store).expandedIds).toEqual({ 1: true });
        expect(container.querySelectorAll('.node')).toHaveLength(3);
    });

    it('tlačítko zabalení stromu zruší výběr a nechá jen první úroveň', async () => {
        const { store, container } = await renderTree();

        fireEvent.click(nodeElement('Série A'));
        const collapseAll = container.querySelector('.fa-tree-lazy-actions .tree-button');
        if (!collapseAll) {
            throw new Error('Chybí tlačítko zabalení stromu');
        }
        fireEvent.click(collapseAll);

        expect(movementsTree(store).selectedIds).toEqual({});
        expect(movementsTree(store).expandedIds).toEqual({ 1: true });
    });

    it('pravé tlačítko zaměří uzel a zobrazí prázdné kontextové menu na pozici kurzoru', async () => {
        const { store } = await renderTree();

        const label = nodeElement('Série B').querySelector('.node-label');
        if (!label) {
            throw new Error('Uzel Série B nemá popisek');
        }
        fireEvent.contextMenu(label, { clientX: 30, clientY: 40 });

        expect(movementsTree(store).focusId).toBe(3);
        const { contextMenu } = store.getState() as TestState;
        expect(contextMenu.visible).toBe(true);
        expect(contextMenu.position).toEqual({ x: 30, y: 40 });
        const menu = contextMenu.menu;
        expect(isValidElement(menu)).toBe(true);
        if (isValidElement<{ className?: string; children?: unknown }>(menu)) {
            expect(menu.type).toBe('ul');
            expect(menu.props.className).toBe('dropdown-menu');
            expect(menu.props.children).toBeUndefined();
        }
        expect(treeRequests).toHaveLength(1);
    });

    it('hledání pošle text do store a vyhledá v celém stromu', async () => {
        const { store } = await renderTree();

        const input = screen.getByRole('textbox');
        fireEvent.change(input, { target: { value: 'Série' } });
        expect(movementsTree(store).filterText).toBe('Série');

        fireEvent.keyUp(input, { keyCode: 13 });

        await waitFor(() => expect(fulltextRequests).toHaveLength(1));
        expect(fulltextRequests[0]).toMatchObject({ versionId: VERSION_ID, nodeId: null, searchValue: 'Série' });
        await waitFor(() => expect(movementsTree(store).selectedIds).toEqual({ 2: true }));

        fireEvent.keyUp(input, { keyCode: 13 });
        await waitFor(() => expect(movementsTree(store).selectedIds).toEqual({ 3: true }));

        fireEvent.keyUp(input, { keyCode: 13, shiftKey: true });
        await waitFor(() => expect(movementsTree(store).selectedIds).toEqual({ 2: true }));
    });
});
