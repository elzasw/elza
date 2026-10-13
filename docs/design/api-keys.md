# Personal API keys

Integrations (publication system, digital archive, scripts, third-party
tools) call Elza's REST API with a personal API key instead of a user's
password. The administrator's view (configuration, rejection codes) is in
`admin-guide/source/05-security.rst`, section "Personal API keys".

## Why

Before API keys, machine clients could only log in with a user name and
password (`login`, HTTP Basic):

- the password sat in the integration's configuration, and changing it broke
  the integration,
- access could not be limited in time or cut off without changing the password,
- the audit could not tell an interactive user from an integration.

An API key is a random secret bound to one `usr_user`, with a mandatory
expiry, sent in an HTTP header, revocable at any time. A user may hold many
keys (typically one per integration) so keys can be rotated without downtime
and revoked independently. The design follows common practice (GitHub
personal access tokens, Stripe keys, OWASP): only a hash is stored, the token
is shown once, it has a public identifier and a recognizable prefix, expiry is
mandatory, last use is recorded and every authentication attempt is audited.

## Token format

```
elza_<keyId>_<secret>
elza_k7Qm2xP9aB4c_Zt7mQb4Vw1XcR9sK2fLh8PnU3oJdY6gEaTiC0rMx
```

| part | form | stored | purpose |
|---|---|---|---|
| `elza_` | fixed prefix | – | recognizable by people and tools (secret scanning, pre-commit hooks); the filter rejects anything else without a DB query |
| `keyId` | 12 chars base62, random (`SecureRandom`) | plain, unique index | public identifier: the server finds the single row by it before checking the secret; UI, API and audit show `elza_<keyId>` |
| `secret` | 43 chars base62 (> 256 bits) | SHA-256 hex only | never stored, logged or shown again after creation |

`ApiKeyToken.parse` accepts only the exact shape
(`elza_[A-Za-z0-9]+_[A-Za-z0-9]+`): a Bearer token, surrounding whitespace or a
garbled copy is rejected as `MALFORMED_TOKEN`, never silently trimmed.

**SHA-256, not bcrypt:** the secret carries 256 bits of random entropy, so a
dictionary attack on the hash is pointless, and a slow hash would only delay
every API request. Hashes are compared in constant time
(`MessageDigest.isEqual`).

## Storage

Table `usr_api_key` (changeset `20260910120000`, `db.elza-3-part-03.xml`),
entity `UsrApiKey`: `api_key_id`, `user_id` (owner), `name` (≤ 250),
`key_id` (unique index `ux_usr_api_key_key_id`), `secret_hash`,
`create_date`, `expire_date` (NOT NULL), `last_used_date`, `revoked_date`,
`revoked_by_user_id` (nullable — the built-in admin has no row). Index
`ix_usr_api_key_user` serves the per-user listing.

**Why a new table, not `usr_authentication`:** that table has one row per
authentication type of a user and a value of up to 250 chars. Keys are many
per user and carry a name, expiry, last use and revocation history.

## Lifecycle

| state | condition | |
|---|---|---|
| `ACTIVE` | not revoked, `expire_date > now` | the only state that authenticates |
| `EXPIRED` | `expire_date <= now` | cannot be extended; the user creates a new key (rotation) |
| `REVOKED` | `revoked_date` set | terminal; the row is kept for history |

- **Create** (`ApiKeyService.create`): name required; expiry defaults to
  `default-validity-days` (365), must be in the future and at most
  `max-validity-days` (730) ahead. The response carries the full `token` —
  the only place it ever appears.
- **Revoke**: sets `revoked_date` and `revoked_by_user_id`; idempotent.
- **Last use** is written at most once per minute per key (in-memory gate per
  node), so a busy integration does not turn each request into a DB write.
- **Deactivating the user** invalidates all their keys: creating the security
  context for an inactive user fails (`LockedException` → `USER_INACTIVE`).

## Authentication

**Header `X-API-Key`, not `Authorization: Bearer`.** The `Authorization`
header is already used by the optional OAuth2/JWT provider and HTTP Basic; a
separate header gives unambiguous routing without inspecting the token. Bearer
support could be added later, recognized by the `elza_` prefix.

**Why not an HMAC request signature** (as towards the AI provider)? Signing
(date, nonce, body hash) protects against eavesdropping and replay even
without TLS and suits a public provider. Internal integrations talk to Elza
over HTTPS, and a plain token is far easier to use (curl, scripts). The
diagnosable rejections of the HMAC scheme were adopted.

Server side (`ApplicationSecurity.apiKeySecurityFilterChain`):

- A **separate `SecurityFilterChain`**, ordered before the main chain and
  selected when the feature is enabled and the header is present. It is
  `STATELESS`, without form login and HTTP Basic, so integrations create no
  HTTP session and do not fill the `SessionRegistry` (the main chain allows 10
  sessions per user). Without the header — or with the feature disabled — a
  request goes through the main chain unchanged.
- `ApiKeyAuthenticationFilter` parses the header; `ApiKeyAuthenticationProvider`
  (registered only in this chain) checks, in one transaction: key exists → not
  revoked → not expired → secret hash matches → user active, then builds the
  same `Authentication` as an interactive login
  (`UserService.createAuthentication`), with `UserDetail.authenticationMethod
  = API_KEY`.
- Applies to everything the chain matches, i.e. the REST API (`/api/v1/...`
  and the older controllers). SOAP (`/services/*`) stays on HTTP Basic.

### Diagnosable rejections

A rejection returns `401` with a JSON body
(`ApiKeyAuthenticationEntryPoint`): `code` is stable for programs, `message`
is a core message rendered in the request language.

| code | what the client should fix |
|---|---|
| `MALFORMED_TOKEN` | send the token exactly as issued |
| `UNKNOWN_KEY` | key does not exist or was issued by another installation |
| `INVALID_SECRET` | token damaged (e.g. truncated while copying) |
| `EXPIRED` | create a new key |
| `REVOKED` | create a new key; revocation is a deliberate action |
| `USER_INACTIVE` | contact the administrator |

Each attempt is written to the SIEM log (`SiemAuditLogger`, authentication
type `API_KEY`): success with user, source IP and `keyId`; failure with source
IP, `keyId` when known, and the failure code — never the secret. Key creation
and revocation are audited too (`api_key_created`, `api_key_revoked`: actor,
owner, `keyId`, expiry). The application log has only a DEBUG line per
rejection.

## Permissions and limits

A key-authenticated request runs **with the full permissions of the owner**:
`@AuthMethod`/`UsrPermission` checks and change tracking
(`arr_change.user_id`) work as after a password login. No new permission
exists.

| action | who |
|---|---|
| create, list, revoke own keys (`/api/v1/user/my/api-keys`) | any user with a `usr_user` row, logged in interactively |
| list, revoke another user's keys (`/api/v1/admin/user/{userId}/api-keys`) | `USR_PERM`, or `USER_CONTROL_ENTITY` over the user |
| create a key for another user | nobody — only the owner may see the secret |

A key must not be usable to escalate into permanent access, so
`UserService.requireInteractiveAuth()` answers `403` for a key-authenticated
request on all key management operations (own and admin), on the password
policy update and on every write of login credentials: a password change (own
or another user's) and setting or removing an authentication of a user when
it is created or edited. The credential check sits in `UserService` (`updateAuth`,
`changePasswordPrivate`, removal in `changeUser`), so every path that writes
credentials is covered, not only the current endpoints.

Revoking a key that belongs to someone else through the "my" endpoint returns `404`, so users cannot probe for other users' keys. The
built-in default user (no `usr_user` row) cannot own keys; its listing is
empty.

The React UI never uses API keys; it logs in through `login`.

## Decisions

- **No non-expiring keys.** The maximum validity is configurable
  (`max-validity-days`); integrations rotate keys.
- **A password change does not revoke keys.** Keys are independent of the
  password; only revocation, expiry or deactivating the user invalidate them.
- **SOAP stays on HTTP Basic.** The same mechanism could cover it later.
- **No scopes.** A key has the owner's full permissions; for restricted
  integrations create a technical user with just the needed permissions and
  issue the key to it. Scopes could be added later (a column of allowed
  permissions intersected in `UserDetail`).
- **Expiry warning only in the UI** (keys expiring within 30 days are
  highlighted); `usr_user` holds no e-mail address for notifications.
- **Rate limiting later** — a simple per-source-IP limit on failed attempts;
  Elza has no rate limiting today.

## Configuration

`elza.security.api-keys` (`ApiKeyProperties`, documented commented-out in
`elza.yaml.template`): `enabled` (true), `header-name` (`X-API-Key`),
`default-validity-days` (365), `max-validity-days` (730).

## Open work

- **`securitySchemes` in the OpenAPI contract:** `elza-openapi.yml` declares
  no `apiKey` scheme (`in: header`, `name: X-API-Key`), so generated
  documentation and clients do not know how to authenticate.
- Rate limiting of failed attempts per source IP (see Decisions).
