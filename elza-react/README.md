# ELZA React

Uživatelské rozhraní aplikace elza

Pro spuštění nebo sestavení je potřeba:
* [Node.js 20.x.x+](https://nodejs.org/)

## Testy

Frontend používá [Vitest](https://vitest.dev/) a [React Testing Library](https://testing-library.com/). HTTP volání se mockuje přes [MSW](https://mswjs.io/), STOMP WebSocket přes vlastní `FakeStompClient`.

```bash
npm test               # jednorázové spuštění
npm run test:watch     # watch mód
npm run test:coverage  # report pokrytí
```

Testy se spouští i v rámci Maven buildu ve fázi `test`. Přeskočení standardními Maven způsoby:
* `-DskipTests` nebo `-Pskiptest` (nastaví `maven.test.skip=true`).

Testy jsou umístěny vedle zdrojových souborů jako `*.test.ts(x)` / `*.spec.ts(x)`. Sdílené pomůcky žijí v [src/test/](src/test/):

* [`test-utils.tsx`](src/test/test-utils.tsx) — `renderWithProviders` (Redux + `IntlProvider` + `MemoryRouter`) a `createTestStore`.
* [`setup.ts`](src/test/setup.ts) — globální setup: jest-dom matchery, stub `window.serverContextPath`, mock `@stomp/stompjs`, lifecycle MSW serveru.
* [`mocks/stomp.ts`](src/test/mocks/stomp.ts) — `FakeStompClient`; testy si berou instanci přes `getLatestStompClient()` a simulují příchozí zprávy přes `deliverFrame(destination, body)`.
* [`mocks/handlers.ts`](src/test/mocks/handlers.ts) + [`server.ts`](src/test/mocks/server.ts) — výchozí MSW handlery a server.

Příklady: [src/stores/app/status.test.ts](src/stores/app/status.test.ts) (reducer) a [src/test/mocks/stomp.test.ts](src/test/mocks/stomp.test.ts) (STOMP mock).

Detailní plán rozvoje testů a návod „jak napsat test" je v [refactoring.md](refactoring.md).
