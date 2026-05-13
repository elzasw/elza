import React from 'react';
import { describe, it, expect } from 'vitest';
import { delay, http, HttpResponse } from 'msw';

import { renderWithProviders, screen } from 'test/test-utils';
import { server } from 'test/mocks/server';
import AdminLogsPage from './AdminLogsPage';

/**
 * Page-mount smoke test. Proves that:
 *   - The full import graph of a real page resolves under Vitest.
 *   - `renderWithProviders` wires enough providers (Redux + IntlProvider +
 *     MemoryRouter) for a connected class component + Ribbon + PageLayout to
 *     mount.
 *   - The singleton `@stomp/stompjs` client in websocketActions.jsx is the
 *     FakeStompClient (no real socket is opened during render).
 *   - MSW intercepts the initial `/api/admin/logs` call.
 *
 * The fetch is held open with `delay('infinite')` so the component stays in
 * the loading state — AdminLogsDetail would otherwise restart the poll on
 * every response via setTimeout.
 */
describe('AdminLogsPage (smoke)', () => {
    it('mounts and shows the loading state while logs fetch', async () => {
        server.use(
            http.get('/api/admin/logs', async () => {
                await delay('infinite');
                return HttpResponse.json({ lines: [], lineCount: 0 });
            }),
        );

        renderWithProviders(<AdminLogsPage />, {
            route: '/admin/logs',
            // Ribbon's mapStateToProps reads shape details not present in
            // the reducers' initial state. Seed the minimum needed to render.
            preloadedState: {
                userDetail: {
                    id: 1,
                    username: 'test',
                    userPermissions: {},
                    permissionsMap: {},
                    authTypes: [],
                    fetched: true,
                    fetching: false,
                },
            },
        });

        expect(await screen.findByDisplayValue('Načítání...')).toBeInTheDocument();
    });
});
