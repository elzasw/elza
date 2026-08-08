# Elza AI Provider API — request signing (`ELZA-AI-HMAC-SHA256`)

Authoritative definition of how requests to an Elza AI provider are
authenticated. The contract in [main.tsp](main.tsp) references this page for the
signing algorithm (OpenAPI can't express HMAC signing natively). The scheme is a
stateless HMAC request signature with the request **body included** in the
signature.

## Credentials

The provider issues each subscriber a **key**: a public `KeyId` and a **secret**
(the HMAC key). The secret is **never transmitted** — it is used only to sign.

The `KeyId` is opaque to the client; the provider chooses its form and scope:

| Scope | Example `KeyId` | Use |
|-------|-----------------|-----|
| Installation/organization | `ACME` | One key for the whole installation; task attribution via `metadata.requestedBy`. |
| Named user | `ACME/petr` | Key issued to one user — per-user attribution, and per-user quotas on the provider side. |

Constraints on `KeyId`: no `,`, no whitespace (it is carried as an
`Authorization` parameter). A subscriber (or user) may hold several active keys
(rotation); the server accepts a request whose signature is valid under **any**
active secret of the `KeyId`.

## Required headers

| Header | Value |
|--------|-------|
| `X-AI-Date` | Request creation time, UTC, ISO 8601 (e.g. `2026-07-02T10:00:00Z`). Must be within **±2 minutes** of server time. |
| `X-AI-Nonce` | A value the client does not repeat within the freshness window — a UUID or ≥16 random bytes (hex/base64). At most 128 characters. Optional on the wire for backward compatibility; **every current client must send it**. |
| `Authorization` | `ELZA-AI-HMAC-SHA256 KeyId=<keyId>,Signature=<base64>` |

### Why the nonce exists

`X-AI-Date` has one-second granularity, so a client legitimately repeating a
request inside one second signs a byte-identical string. Without a nonce, that
repeat is **indistinguishable from an attacker replaying the request**, and a
server refusing duplicate signatures refuses honest traffic: a client that
long-polls a task, answers a tool round and immediately re-polls does exactly
this, and was refused in production (2026-08-07). The nonce is what makes each
request unique, so replay can be rejected without guessing.

**A nonce is per transmission, not per logical request.** Every attempt gets a
fresh value — including a retry of a request that timed out, whose fate the
client does not know. Do not reuse a nonce to express "this is the same
request": that is what `SubmitTask.requestId` is for, and the server's
idempotency (submit dedup, tool-result re-sends as no-ops) is what makes the
retry safe. Reusing it only earns a `401`.

A server that receives no nonce cannot tell a repeat from a replay and resolves
that ambiguity in favour of the honest client: it **must not** refuse a repeated
safe request (`GET`, `HEAD`, `OPTIONS`, `TRACE`), and **should** still refuse a
repeated state-changing one. This is the defined behaviour of a nonce-less
request, not a grace period — but it is a real concession: an attacker who
captured a request without its response can, within the date window, replay it
and obtain the response. Sending the nonce closes that completely, which is why
every current client does.

## String to sign

Six lines joined by a single `\n` (LF), **no trailing newline**, fixed order —
plus a **seventh line, the nonce, when `X-AI-Nonce` is sent**:

```
<HTTP method>          e.g. POST
<host>                 e.g. ai.example.com   (no scheme, no port unless non-default)
<request path>         e.g. /elza/tasks      (the full path as requested, incl. any endpoint prefix)
<canonical query>      raw query string as sent, or empty line if none
<X-AI-Date>            the header value, verbatim
<hex SHA-256 of body>  lowercase hex; for an empty body, the hash of ""
<X-AI-Nonce>           the header value, verbatim — this line is present only when the header is
```

Notes:

- The signed path is the **full request path** — the provider-assigned endpoint
  prefix included. With endpoint `https://ai.example.com/elza`, submitting a task
  signs the path `/elza/tasks`.
- Task polling uses a query string (`?wait=20`); the query line is then the raw
  query as sent, e.g. `wait=20`.
- The nonce line is **conditional, not empty-when-absent**: a request without the
  header signs exactly the original six lines, so older clients keep working
  unchanged. The two forms are different strings, so an attacker cannot strip the
  header from a captured request — the signature would no longer verify.

## Signature

```
Signature = Base64( HMAC-SHA256( secret, stringToSign ) )
```

Standard Base64 (with `+`/`/` and `=` padding), single line.

## Server verification

1. Parse `KeyId` and `Signature` from `Authorization`; require `X-AI-Date`.
2. Reject if `X-AI-Date` is outside ±2 min of server time.
3. If `X-AI-Nonce` is present, reject it unless it is 1–128 characters of
   visible ASCII with no whitespace (this bounds the replay store's memory, and
   keeps the value from being normalized differently by the HTTP layer than by
   the signer).
4. Rebuild `stringToSign` from the received method, host, path, query, date, the
   SHA-256 of the received body, and — only if the header was sent — the nonce.
5. For each active secret of the `KeyId`, compute the HMAC and compare to
   `Signature` in **constant time**. Accept on the first match; else `401`.
6. Replay: reject when the `(KeyId, nonce)` pair has already been seen within the
   window. For a request **without** a nonce, reject a repeated `Signature` only
   on state-changing methods — never on a safe one (see *Why the nonce exists*).
7. Check the subscriber has an active AI subscription (else `402`).

### Rejections must be diagnosable

A `401` never reaches the application, so a rejection the server does not
explain is invisible on both sides: the provider logs nothing, and the client
sees only "unauthorized". Every rejection should therefore be **logged** with
the method, path and claimed `KeyId`, and its `message` should name the cause
distinctly. The causes are unrelated and have unrelated fixes:

| Cause | What the client must change |
|-------|-----------------------------|
| Missing/malformed `Authorization` | Send the header in the documented form. |
| Stale `X-AI-Date` | Fix the clock. The message carries the **server's own time** and the allowed skew, so the offset is visible without asking anyone; `GET /ping` reports it unauthenticated too. |
| Malformed `X-AI-Nonce` | Send 1–128 visible-ASCII characters, no whitespace. |
| Unknown `KeyId` | Wrong or retired key — a different problem from a bad signature, and named separately. A `KeyId` is an issued identifier, not a secret. |
| Signature mismatch | The signing **input** differs. The server should log the canonical string it rebuilt at DEBUG: it contains no secret, and seeing it is what actually resolves these (a proxied `Host`, a dropped path prefix, a re-encoded query). |
| Replayed nonce | Use a fresh nonce per transmission (see above). |
| Replayed signature (nonce-less) | Send `X-AI-Nonce`. |

Never reveal which secret matched, and never log the `Signature` or the secret.

## Worked example 1 — POST with a user-scoped key

Endpoint: `https://ai.example.com/elza`

```
secret      = Zt7mQb4Vw1XcR9sK2fLh8PnU3oJdY6gEaTiC0rMx
KeyId       = ACME/petr
method      = POST
host        = ai.example.com
path        = /elza/tasks
query       = (empty)
X-AI-Date   = 2026-07-02T10:00:00Z
body        = {"requestId":"5f0c1b2a-9d4e-4c3b-8a71-2e6f0d9b4c11","taskType":"elza.echo","input":{"message":"Hello provider"},"outputSchema":{"type":"object","properties":{"message":{"type":"string"}},"required":["message"]}}
```

```
SHA-256(body) = ddb4ce45a211f970f42bb208a2db3bf8cf3834bbc3bb2793e44a7f2f2bdd08ab

stringToSign (between the markers, LF-joined):
>>>POST
ai.example.com
/elza/tasks

2026-07-02T10:00:00Z
ddb4ce45a211f970f42bb208a2db3bf8cf3834bbc3bb2793e44a7f2f2bdd08ab<<<

Signature = NvvN411aLl56dyOoEgfEIDgnwMSWycggKJKF8R5DvNA=
```

Resulting headers:

```
X-AI-Date: 2026-07-02T10:00:00Z
Authorization: ELZA-AI-HMAC-SHA256 KeyId=ACME/petr,Signature=NvvN411aLl56dyOoEgfEIDgnwMSWycggKJKF8R5DvNA=
```

## Worked example 2 — GET with a query (long poll, installation key)

```
secret      = Zt7mQb4Vw1XcR9sK2fLh8PnU3oJdY6gEaTiC0rMx
KeyId       = ACME
method      = GET
host        = ai.example.com
path        = /elza/tasks/t-01HVX3Z8KQ
query       = wait=20
X-AI-Date   = 2026-07-02T10:00:00Z
body        = (empty)
```

```
SHA-256("")   = e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855

stringToSign (between the markers, LF-joined):
>>>GET
ai.example.com
/elza/tasks/t-01HVX3Z8KQ
wait=20
2026-07-02T10:00:00Z
e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855<<<

Signature = 09QBRMZxxdw5Qh7fRDNCd+e9w8OBmprztBvUjXGujSQ=

Authorization: ELZA-AI-HMAC-SHA256 KeyId=ACME,Signature=09QBRMZxxdw5Qh7fRDNCd+e9w8OBmprztBvUjXGujSQ=
```

## Worked example 3 — the same GET, with a nonce

Identical to example 2 except for `X-AI-Nonce`, which adds the seventh line —
so the signature differs. This is the form every current client sends.

```
secret      = Zt7mQb4Vw1XcR9sK2fLh8PnU3oJdY6gEaTiC0rMx
KeyId       = ACME
method      = GET
host        = ai.example.com
path        = /elza/tasks/t-01HVX3Z8KQ
query       = wait=20
X-AI-Date   = 2026-07-02T10:00:00Z
X-AI-Nonce  = 0f9a1c7e-3b62-4a58-9d0e-71c5a8f4b2d3
body        = (empty)
```

```
stringToSign (between the markers, LF-joined):
>>>GET
ai.example.com
/elza/tasks/t-01HVX3Z8KQ
wait=20
2026-07-02T10:00:00Z
e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855
0f9a1c7e-3b62-4a58-9d0e-71c5a8f4b2d3<<<

Signature = RA/4ThOJpCrXdnY1kzg9SikalMRRoJOwrxSroF4WbLo=
```

Resulting headers:

```
X-AI-Date: 2026-07-02T10:00:00Z
X-AI-Nonce: 0f9a1c7e-3b62-4a58-9d0e-71c5a8f4b2d3
Authorization: ELZA-AI-HMAC-SHA256 KeyId=ACME,Signature=RA/4ThOJpCrXdnY1kzg9SikalMRRoJOwrxSroF4WbLo=
```

## Reference client (Bash, `curl` + `openssl` only)

```sh
# inputs: $url (full), $secret, $key_id, $body
host=$(printf '%s' "$url" | sed -E 's#^[a-z]+://([^/]+).*#\1#')
path=$(printf '%s' "$url" | sed -E 's#^[a-z]+://[^/]+##; s#\?.*##')
query=$(printf '%s' "$url" | sed -nE 's#^[^?]*\?##p')
date=$(date -u +%Y-%m-%dT%H:%M:%SZ)
nonce=$(uuidgen)      # any value not repeated within the window
bodyhash=$(printf '%s' "$body" | openssl dgst -sha256 -hex | sed 's/^.*= *//')
sts=$(printf '%s\n%s\n%s\n%s\n%s\n%s\n%s' POST "$host" "$path" "$query" "$date" "$bodyhash" "$nonce")
sig=$(printf '%s' "$sts" | openssl dgst -sha256 -hmac "$secret" -binary | openssl base64 -A)
curl -fsS -X POST "$url" \
  -H "Content-Type: application/json" \
  -H "X-AI-Date: $date" \
  -H "X-AI-Nonce: $nonce" \
  -H "Authorization: ELZA-AI-HMAC-SHA256 KeyId=$key_id,Signature=$sig" \
  --data-raw "$body"
```
