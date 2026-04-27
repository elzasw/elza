/**
 * Vitest setup — runs once before the test files are loaded.
 *
 * Responsibilities:
 *   - Register jest-dom matchers.
 *   - Stub browser globals that ELZA modules read at import time.
 *   - Replace `@stomp/stompjs` with FakeStompClient so nothing opens a real
 *     WebSocket.
 *   - Start/stop MSW for HTTP interception.
 */

import '@testing-library/jest-dom/vitest';
import { afterAll, afterEach, beforeAll, beforeEach, vi } from 'vitest';

// Stub the global serverContextPath before any module reads it (e.g.
// websocketActions.jsx builds the STOMP URL from window.serverContextPath at
// import time).
(globalThis as unknown as { serverContextPath: string }).serverContextPath = '';

// Replace `@stomp/stompjs` with the fake. `vi.mock` is hoisted above imports,
// and the async factory resolves when the module is first requested, so tests
// and code under test all receive FakeStompClient as `Client`.
vi.mock('@stomp/stompjs', async () => {
    const { FakeStompClient } = await import('./mocks/stomp');
    return { Client: FakeStompClient };
});

// MSW lifecycle — imported lazily to avoid pulling msw before the env is ready.
const serverPromise = import('./mocks/server').then((m) => m.server);

beforeAll(async () => {
    const server = await serverPromise;
    server.listen({ onUnhandledRequest: 'warn' });
});

afterEach(async () => {
    const server = await serverPromise;
    server.resetHandlers();

    // Clear STOMP client registry between tests.
    const { resetStompRegistry } = await import('./mocks/stomp');
    resetStompRegistry();
});

afterAll(async () => {
    const server = await serverPromise;
    server.close();
});

// Silence noisy console output from code under test that is not relevant to
// assertions. Keep warnings surfaced — just deduplicate the known ones.
const origWarn = console.warn;
beforeEach(() => {
    console.warn = (...args: unknown[]) => {
        const first = args[0];
        if (typeof first === 'string' && first.startsWith('[@formatjs/intl]')) {
            return;
        }
        origWarn(...args);
    };
});
