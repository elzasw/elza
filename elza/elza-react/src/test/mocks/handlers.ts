import { http, HttpResponse } from 'msw';

/**
 * Default MSW request handlers.
 *
 * Add handlers here that should be available to every test. For test-specific
 * responses, use `server.use(...)` inside the test itself.
 */
export const handlers = [
    http.get('/api/userDetail', () =>
        HttpResponse.json({ id: 1, username: 'test', userName: 'test', permissions: [] }),
    ),
    // Ribbon fires this on mount (extSystemListFetchIfNeeded). Default to empty
    // so rendering the page frame doesn't crash in smoke tests.
    http.get('/api/admin/externalSystems', () => HttpResponse.json([])),
];
