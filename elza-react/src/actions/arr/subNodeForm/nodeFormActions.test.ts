import { describe, it, expect } from 'vitest';
import { http, HttpResponse } from 'msw';
import type { AnyAction } from 'redux';
import type { ThunkDispatch } from 'redux-thunk';

import { createTestStore } from 'test/test-utils';
import { server } from 'test/mocks/server';
import { fundInitState } from 'stores/app/arr/fund';
import { nodeInitState } from 'stores/app/arr/node';
import type { AppState } from 'typings/store';
import { nodeFormActions } from './nodeFormActions';

const VERSION_ID = 7;
const PARENT_ID = 10;
const DELETED_NODE_ID = 25503;

interface NodeTab {
    id: number;
    routingKey: string;
    selectedSubNodeId: number | null;
}

interface TestState {
    arrRegion: { funds: { nodes: { activeIndex: number | null; nodes: NodeTab[] } }[] };
}

/** Store with one fund whose single node tab has a selected sub node, as restored from localStorage. */
function createStoreWithNodeTab() {
    const tab = { ...nodeInitState({ id: PARENT_ID, version: 1 }), selectedSubNodeId: DELETED_NODE_ID };
    const fund = fundInitState({ id: 5, versionId: VERSION_ID, name: 'Fond Testovací', closed: false });
    fund.nodes = { ...fund.nodes, activeIndex: 0, nodes: [tab] };
    const base = createTestStore().getState() as { arrRegion: object };
    const store = createTestStore({ arrRegion: { ...base.arrRegion, activeIndex: 0, funds: [fund] } });
    return { store, routingKey: tab.routingKey as string };
}

function respondNodeData(code: string) {
    server.use(
        http.post('/api/v1/node/node-data', () =>
            HttpResponse.json({ type: 'BaseCode', code, level: 'danger', properties: {} }, { status: 404 }),
        ),
    );
}

describe('nodeFormActions._fundSubNodeFormFetch', () => {
    it('zavře záložku, když vybraná JP mezitím neexistuje', async () => {
        respondNodeData('ID_NOT_EXIST');
        const { store, routingKey } = createStoreWithNodeTab();
        const dispatch = store.dispatch as ThunkDispatch<AppState, void, AnyAction>;

        await dispatch(nodeFormActions._fundSubNodeFormFetch(VERSION_ID, DELETED_NODE_ID, routingKey, false));

        const nodes = (store.getState() as TestState).arrRegion.funds[0].nodes;
        expect(nodes.nodes).toHaveLength(0);
    });

    it('jinou chybu propustí dál a záložku ponechá', async () => {
        respondNodeData('OPTIMISTIC_LOCKING_ERROR');
        const { store, routingKey } = createStoreWithNodeTab();
        const dispatch = store.dispatch as ThunkDispatch<AppState, void, AnyAction>;

        await expect(
            dispatch(nodeFormActions._fundSubNodeFormFetch(VERSION_ID, DELETED_NODE_ID, routingKey, false)),
        ).rejects.toMatchObject({ code: 'OPTIMISTIC_LOCKING_ERROR' });

        const nodes = (store.getState() as TestState).arrRegion.funds[0].nodes;
        expect(nodes.nodes).toHaveLength(1);
    });
});
