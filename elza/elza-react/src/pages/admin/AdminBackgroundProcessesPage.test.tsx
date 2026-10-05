import {describe, expect, it} from 'vitest';
import {HttpResponse, http} from 'msw';
import userEvent from '@testing-library/user-event';

import {server} from 'test/mocks/server';
import {enMessages} from 'test/i18n-catalog';
import {renderWithProviders, screen} from 'test/test-utils';
import AdminBackgroundProcessesPage from './AdminBackgroundProcessesPage';

const QUEUES = [
    {
        type: 'BULK',
        load: 0.0064,
        requestPerHour: 5,
        waitingRequests: 0,
        runningThreadCount: 0,
        totalThreadCount: 1,
        currentThreads: [],
    },
    {
        type: 'NODE',
        load: 0.5,
        requestPerHour: 97,
        waitingRequests: 12,
        runningThreadCount: 1,
        totalThreadCount: 3,
        currentThreads: [{fundVersionId: 7, requestId: 42, beginTime: '2026-09-21T08:15:30', runningTime: 1000, currentId: 99}],
    },
];

const PRELOADED_STATE: Record<string, unknown> = {
    // Ribbon's mapStateToProps reads shape details not present in the reducers'
    // initial state. Seed the minimum needed to render.
    userDetail: {
        id: 1,
        username: 'test',
        userPermissions: {},
        permissionsMap: {},
        authTypes: [] as string[],
        fetched: true,
        fetching: false,
    },
};

function renderPage() {
    return renderWithProviders(<AdminBackgroundProcessesPage />, {
        route: '/admin/backgroundProcesses',
        locale: 'en',
        messages: enMessages,
        preloadedState: PRELOADED_STATE,
    });
}

describe('AdminBackgroundProcessesPage', () => {
    it('lists the queues in a fixed order, whatever order the server sends', async () => {
        server.use(http.get('/api/v1/admin/async-requests', () => HttpResponse.json(QUEUES)));

        renderPage();

        // NODE is declared before BULK, so it must come first even though the
        // server listed it second.
        const headers = await screen.findAllByRole('button', {name: /validation|Bulk actions/i});
        expect(headers.map(header => header.textContent)).toEqual([
            expect.stringContaining('Description unit validation'),
            expect.stringContaining('Bulk actions'),
        ]);
    });

    it('loads the queue contents only once its panel is expanded', async () => {
        let detailCalls = 0;
        server.use(
            http.get('/api/v1/admin/async-requests', () => HttpResponse.json(QUEUES)),
            http.get('/api/v1/admin/async-requests/NODE', () => {
                detailCalls++;
                return HttpResponse.json([{fund: {id: 3, name: 'Testovací AS'}, fundVersionId: 7, requestCount: 12}]);
            }),
        );

        renderPage();

        const header = await screen.findByRole('button', {name: /Description unit validation/i});
        expect(detailCalls).toBe(0);

        await userEvent.click(header);

        expect(await screen.findByText('Testovací AS')).toBeInTheDocument();
        expect(detailCalls).toBe(1);
        // The running thread of the queue is detail too - shown only when opened.
        expect(screen.getByText(/queue ID #42/i)).toBeInTheDocument();
    });

    it('reports a failed load and keeps the page usable', async () => {
        server.use(http.get('/api/v1/admin/async-requests', () => new HttpResponse(null, {status: 500})));

        renderPage();

        expect(await screen.findByText('Queue state could not be loaded')).toBeInTheDocument();
    });
});
