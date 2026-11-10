import { describe, it, expect } from 'vitest';
import { http, HttpResponse } from 'msw';
import type { AnyAction } from 'redux';
import type { ThunkDispatch } from 'redux-thunk';
import type { BulkAppendRequest, BulkChangeResult, BulkItemValue } from 'elza-api';

import { createTestStore } from 'test/test-utils';
import { server } from 'test/mocks/server';
import type { AppState } from 'typings/store';
import { fundBulkAppend } from './fundDataGrid';

const VERSION_ID = 7;

const values: BulkItemValue[] = [
    { itemSpecId: 11, data: { dataType: 'ENUM' } },
    { itemSpecId: 12, data: { dataType: 'ENUM' } },
];

interface ToastState {
    toastr: { toasts: { style: string }[] };
}

function captureAppendRequests(result: BulkChangeResult) {
    const requests: { versionId: string; body: BulkAppendRequest }[] = [];
    server.use(
        http.put('/api/v1/descItems/:versionId/bulk-append', async ({ request, params }) => {
            requests.push({ versionId: String(params.versionId), body: (await request.json()) as BulkAppendRequest });
            return HttpResponse.json(result);
        })
    );
    return requests;
}

describe('fundBulkAppend', () => {
    it('pošle hodnoty a vybrané JP a ohlásí výsledek', async () => {
        const result: BulkChangeResult = { created: 3, skippedDuplicate: 1, changedNodes: 2 };
        const requests = captureAppendRequests(result);
        const store = createTestStore();
        const dispatch = store.dispatch as ThunkDispatch<AppState, void, AnyAction>;

        const response = await dispatch(fundBulkAppend(VERSION_ID, 5, values, [{ id: 1, version: 2 }], 'NODES'));

        expect(response).toEqual(result);
        expect(requests).toEqual([
            {
                versionId: String(VERSION_ID),
                body: {
                    itemTypeId: 5,
                    selection: { selectionType: 'NODES', nodes: [{ id: 1, version: 2 }] },
                    values,
                },
            },
        ]);
        const { toasts } = (store.getState() as ToastState).toastr;
        expect(toasts).toHaveLength(1);
        expect(toasts[0].style).toBe('success');
    });

    it('pro celý AS neposílá žádné JP', async () => {
        const requests = captureAppendRequests({ created: 0, skippedDuplicate: 0, changedNodes: 0 });
        const dispatch = createTestStore().dispatch as ThunkDispatch<AppState, void, AnyAction>;

        await dispatch(fundBulkAppend(VERSION_ID, 5, values, [], 'FUND'));

        expect(requests[0].body.selection).toEqual({ selectionType: 'FUND', nodes: [] });
    });
});
