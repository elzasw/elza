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

## Kerberos (SSO) ve vývoji

Proxy ve [vite.config.ts](vite.config.ts) přidává tiket z lokální cache k požadavkům na backend. Díky tomu jde SSO vyzkoušet i ve vývoji. Je to volitelné. Bez toho se přihlašuje heslem.

Aplikace běží na `localhost` jako obvykle. O tiket se stará proxy, ne prohlížeč.

Balíček `kerberos` je v `optionalDependencies`. Jeho nativní část se překládá ze zdrojů a potřebuje:

* **Linux** — hlavičky MIT Kerberos (`krb5` / `libkrb5-dev`), `gcc`, `make`, Python 3
* **macOS** — Xcode Command Line Tools
* **Windows** — Visual Studio Build Tools + Windows SDK (použije se SSPI, MIT Kerberos netřeba)

npm od verze 12 nespouští install skripty. Balíček se proto nainstaluje, ale nesestaví. Je potřeba to udělat ručně, a to znovu po každém `npm ci`:

```bash
cd node_modules/kerberos && npx node-gyp rebuild
```

Zapnutí v `.env`:

```
KERBEROS=true                               # bez toho se proxy vůbec nezapojí
                                            # na ENDPOINT s IP adresou se SSO nepoužije
# KERBEROS_SERVICE=HTTP/backend.priklad.cz  # SSPI na Windows chce tvar HTTP/host
```

### Tiket na Linuxu a macOS

Potřeba nakonfigurovaný `/etc/krb5.conf`.

Pořízení tiketu:

```bash
kinit uzivatel@PRIKLAD.CZ
```

### Tiket na Windows

Nenastavuje se nic. Stroj připojený do domény už má tiket z přihlášení.

### Kontrola platnosti

Tikety a jejich expiraci vypíše `klist`. Funguje na Linuxu, macOS i Windows.

### Po vypršení tiketu

Proxy zaloguje `[spnego] no token for ...`. Požadavky pak jdou na backend nepřihlášené. Stejné chování pokud balíček chybí nebo je `KERBEROS` vypnuté.
