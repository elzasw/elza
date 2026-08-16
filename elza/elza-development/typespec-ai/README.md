# Elza AI Provider API (TypeSpec)

TypeSpec definition of the **Elza AI Provider API** — the vendor-neutral protocol
between Elza (client) and an **AI provider**: a service that executes AI tasks
over archival description data (advisory validation of descriptions, value
extraction, inquiry research assistance, …).

Elza is open-source software and this protocol does not depend on any particular
provider: **any party may implement it** and offer AI services to Elza
installations. A provider gives each subscriber a base endpoint URL and signing
credentials; Elza is configured with them as an external system.

Status: **experimental / development** — the contract is being developed
together with its first client (Elza) and first provider implementation.

## Files

| File | Purpose |
|------|---------|
| `main.tsp` | The contract (compiles to OpenAPI 3). |
| `security.md` | Authoritative definition of the `ELZA-AI-HMAC-SHA256` request signing, incl. test vectors and a reference client. |
| `tspconfig.yaml` | Emitter config — output goes **directly into elza-core resources**: `elza-core/src/main/resources/ai/elza-ai-provider.openapi.yaml` (the single committed copy; elza-core's build generates the Java client `cz.tacr.elza.aiprovider.client.*` from it — execution `openapi-ai-provider-client` in `elza-core/pom.xml`). |
| `tasks/` | Per-task-type contracts (Elza-owned input format + output schema + Elza-side mapping): [tasks/elza-revision.md](tasks/elza-revision.md), [tasks/elza-enhance-description.md](tasks/elza-enhance-description.md). |

## Build

Requires Node.js 20+.

```sh
npm install
npm run build     # emits ../../elza-core/src/main/resources/ai/elza-ai-provider.openapi.yaml
npm run check     # compile without emitting
npm run format
```

**Always run the build and commit the emitted OpenAPI in elza-core resources
together with `main.tsp` changes** — the Java client is generated from the
committed OpenAPI, not from TypeSpec. Other consumers (e.g. the provider
implementation in `elza-ai-provider.git`) copy that emitted file into their own
repository.

## Design summary

- **Asynchronous task protocol, client-initiated only.** Elza typically runs
  inside infrastructure unreachable from the internet, so every connection goes
  Elza → provider: submit a task, long-poll its state, done. No callbacks.
- **Full interaction model from v1** so services can grow without breaking
  changes: tool-use loop (`awaiting_tools` + `POST …/tool-results` — the provider
  asks Elza to run searches locally; only search *results* leave the archive),
  advisory `progress` for user display, `cancel` (STOP), and follow-up refinement
  via `parentTaskId` (the provider holds conversation context; the protocol is
  delta-only).
- **Contract split by change cadence.** The client owns `taskType`, `input`
  shape and `outputSchema` (they change with client releases); the provider owns
  the prompt per task type and can improve it anytime (`promptVersion` is
  reported per task). The provider guarantees `output` validates against the
  submitted schema.
- **Provider-assigned endpoint.** All paths are relative to a base URL the
  provider assigns (it may embed the provider's own account routing); Elza
  stores endpoint + `KeyId` + secret.
- **Auth = HMAC request signing** (`ELZA-AI-HMAC-SHA256`): stateless, the secret
  never travels, replay bounded by a ±2 min window. The provider issues keys
  scoped to the whole installation or to a named user (per-user attribution and
  quotas). See `security.md`. The single unauthenticated operation is
  `GET /ping` — a liveness probe that also returns the server time, so a client
  can tell a wrong endpoint from bad credentials and detect its own clock skew.
- **Metering built in**: every task reports `usage` (tokens informative,
  `costUnits` billable per the provider's price list); `402` signals a missing
  subscription or exhausted quota.
- **Additive evolution**: new task types, states and optional fields only;
  clients ignore unknown fields and treat unknown states as "in progress".
  `GET /info` advertises protocol version, task types, profiles, limits — and
  doubles as the connection test.

## First scenarios

1. `elza.echo` — integration-test task: exercises auth, submit, poll,
   schema-enforced output and metering with trivial cost. Implement first on
   both sides.
2. `elza.revision` — advisory revision of description units (read-only;
   findings land as Elza issues). The first production scenario.

Design background (use cases, architecture, provider-side considerations):
`doc/AI-INTEGRATION-PROPOSAL.md` in `elza-ai-provider.git`.
