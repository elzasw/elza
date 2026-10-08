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
    // Login asks whether the first-run setup is needed whenever nobody is logged in.
    http.get('/api/v1/setup', () => HttpResponse.json({ setupRequired: false })),
    // The package administration lists the packages of dpkg that are not loaded; none by default.
    http.get('/api/v1/packages/available', () =>
        HttpResponse.json({ items: [], restartRequired: false }),
    ),
    http.get('/api/v1/languages', () =>
        HttpResponse.json([
            { tag: 'cs', code: 'cze', uiEnabled: true, scopeEnabled: true, defaultLanguage: true },
            { tag: 'de', code: 'ger', uiEnabled: false, scopeEnabled: true, defaultLanguage: false },
            { tag: 'en', code: 'eng', uiEnabled: true, scopeEnabled: true, defaultLanguage: false },
        ]),
    ),
];
