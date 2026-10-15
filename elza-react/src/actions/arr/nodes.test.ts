import { describe, it, expect } from 'vitest';
import { http, HttpResponse } from 'msw';
import type { AnyAction } from 'redux';
import type { ThunkDispatch } from 'redux-thunk';

import { createTestStore } from 'test/test-utils';
import { server } from 'test/mocks/server';
import { fundInitState } from 'stores/app/arr/fund';
import type { AppState } from 'typings/store';
import { moveNodes } from './nodes';

const VERSION_ID = 7;

interface NodeTabs {
    activeIndex: number | null;
    nodes: unknown[];
}

interface TestState {
    arrRegion: { funds: { moving: boolean; nodes: NodeTabs }[] };
}

function treeNode(id: number, depth: number) {
    return { id, depth, version: 1, name: `JP ${id}` };
}

describe('moveNodes', () => {
    it('bez otevřené záložky JP (Přesuny otevřené adresou) přesun dokončí bez chyby', async () => {
        const moveRequests: unknown[] = [];
        server.use(
            http.put('/api/arrangement/moveLevelUnder', async ({ request }) => {
                moveRequests.push(await request.json());
                return new HttpResponse(null, { status: 200 });
            })
        );
        const base = createTestStore().getState() as { arrRegion: object };
        const fund = fundInitState({ id: 5, versionId: VERSION_ID, name: 'Fond Testovací', closed: false });
        const store = createTestStore({ arrRegion: { ...base.arrRegion, activeIndex: 0, funds: [fund] } });
        const dispatch = store.dispatch as ThunkDispatch<AppState, void, AnyAction>;

        await dispatch(
            moveNodes('UNDER', VERSION_ID, [treeNode(3, 2)], treeNode(1, 1), treeNode(2, 2), treeNode(1, 1))
        );

        expect(moveRequests).toHaveLength(1);
        const movedFund = (store.getState() as TestState).arrRegion.funds[0];
        expect(movedFund.moving).toBe(false);
        expect(movedFund.nodes.activeIndex).toBeNull();
    });
});
