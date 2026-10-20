import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import { http, HttpResponse } from 'msw';
import React, { isValidElement } from 'react';
import { useSelector } from 'react-redux';

import { renderWithProviders, screen, fireEvent, createTestStore } from 'test/test-utils';
import type { TestStore } from 'test/test-utils';
import { server } from 'test/mocks/server';
import { fundInitState } from 'stores/app/arr/fund';
import type { Fund } from 'typings/store';
import type { TreeNodeVO } from 'typings/Outputs';
import FundTreeMain from './FundTreeMain';

/**
 * Context menu of a unit in the Pořádání tree: "Přesunout" opens the movements page with the unit selected
 * in the left tree, and is offered only where moving is possible.
 */

const FUND_ID = 5;
const VERSION_ID = 7;

interface TestState {
    arrRegion: { funds: Fund[] };
    focus: unknown;
    contextMenu: { visible: boolean; menu: unknown };
}

function treeNode(id: number, depth: number, name: string, hasChildren: boolean): TreeNodeVO {
    return { id, depth, name, hasChildren, icon: 'fa-folder', referenceMark: [], version: 1, arrPerm: true };
}

const rootNodes: TreeNodeVO[] = [
    treeNode(1, 1, 'Fond Testovací', true),
    treeNode(2, 2, 'Série A', false),
    treeNode(3, 2, 'Série B', false),
];

interface HostProps {
    readMode: boolean;
}

/** Renders the tree the way the left panel of ArrPage does. */
function FundTreeMainHost({ readMode }: HostProps) {
    const fund = useSelector((state: TestState) => state.arrRegion.funds[0]);
    const focus = useSelector((state: TestState) => state.focus);
    return (
        <FundTreeMain
            className="fund-tree-container"
            fund={fund}
            versionId={fund.versionId}
            {...fund.fundTree}
            focus={focus}
            readMode={readMode}
        />
    );
}

function createStoreWithFund(lockDate: string | null = null): TestStore {
    const base = createTestStore().getState() as { arrRegion: object };
    const fund: Fund = fundInitState({
        id: FUND_ID,
        versionId: VERSION_ID,
        name: 'Fond Testovací',
        closed: lockDate != null,
        lockDate,
        activeVersion: { id: VERSION_ID, lockDate, issues: [] },
    });
    return createTestStore({ arrRegion: { ...base.arrRegion, activeIndex: 0, funds: [fund] } });
}

/** Right-clicks the unit and renders the menu the tree put into the store. */
async function openContextMenu(store: TestStore, readMode: boolean, nodeName: string) {
    renderWithProviders(<FundTreeMainHost readMode={readMode} />, { store });
    const label = (await screen.findByText(nodeName)).closest('.node')?.querySelector('.node-label');
    if (!label) {
        throw new Error(`Uzel ${nodeName} nemá popisek`);
    }
    fireEvent.contextMenu(label);

    const { contextMenu } = store.getState() as TestState;
    if (!isValidElement(contextMenu.menu)) {
        throw new Error('Kontextové menu není element');
    }
    renderWithProviders(contextMenu.menu, { store });
    // a link offered in every mode, so an absent "Přesunout" is not just an unrendered menu
    expect(screen.getByRole('link', { name: 'Otevřít v tabulce' })).toBeInTheDocument();
}

beforeEach(() => {
    // jsdom lays nothing out; VirtualList renders rows only into a container with a height
    vi.spyOn(HTMLElement.prototype, 'clientHeight', 'get').mockImplementation(function (this: HTMLElement) {
        return this.classList.contains('fa-tree-lazy-container') ? 600 : 0;
    });

    server.use(
        http.post('/api/arrangement/fundTree', () =>
            HttpResponse.json({ nodes: rootNodes, expandedIdsExtension: [1] })
        ),
        http.get(`/api/v1/action/fund-version/${VERSION_ID}`, () => HttpResponse.json([]))
    );
});

afterEach(() => {
    vi.restoreAllMocks();
});

describe('FundTreeMain – kontextové menu Přesunout', () => {
    it('nabídne odkaz na Přesuny s uzlem vybraným v levém stromu', async () => {
        const store = createStoreWithFund();
        await openContextMenu(store, false, 'Série B');

        expect(screen.getByRole('link', { name: 'Přesunout' })).toHaveAttribute(
            'href',
            `/fund/${FUND_ID}/movements?l=3`
        );
    });

    it('v režimu čtení Přesunout nenabídne', async () => {
        const store = createStoreWithFund();
        await openContextMenu(store, true, 'Série B');

        expect(screen.queryByRole('link', { name: 'Přesunout' })).not.toBeInTheDocument();
    });

    it('v uzavřené verzi Přesunout nenabídne', async () => {
        const store = createStoreWithFund('2026-01-01T00:00:00');
        await openContextMenu(store, false, 'Série B');

        expect(screen.queryByRole('link', { name: 'Přesunout' })).not.toBeInTheDocument();
    });
});
