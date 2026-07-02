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
| `Authorization` | `ELZA-AI-HMAC-SHA256 KeyId=<keyId>,Signature=<base64>` |

There is no nonce: the ±2 min window bounds replay, and the server may
additionally reject a duplicate `Signature` seen within that window (no client
change needed).

## String to sign

Six lines joined by a single `\n` (LF), **no trailing newline**, fixed order:

```
<HTTP method>          e.g. POST
<host>                 e.g. ai.example.com   (no scheme, no port unless non-default)
<request path>         e.g. /elza/tasks      (the full path as requested, incl. any endpoint prefix)
<canonical query>      raw query string as sent, or empty line if none
<X-AI-Date>            the header value, verbatim
<hex SHA-256 of body>  lowercase hex; for an empty body, the hash of ""
```

Notes:

- The signed path is the **full request path** — the provider-assigned endpoint
  prefix included. With endpoint `https://ai.example.com/elza`, submitting a task
  signs the path `/elza/tasks`.
- Task polling uses a query string (`?wait=20`); the query line is then the raw
  query as sent, e.g. `wait=20`.

## Signature

```
Signature = Base64( HMAC-SHA256( secret, stringToSign ) )
```

Standard Base64 (with `+`/`/` and `=` padding), single line.

## Server verification

1. Parse `KeyId` and `Signature` from `Authorization`; require `X-AI-Date`.
2. Reject if `X-AI-Date` is outside ±2 min of server time.
3. Rebuild `stringToSign` from the received method, host, path, query, date, and
   the SHA-256 of the received body.
4. For each active secret of the `KeyId`, compute the HMAC and compare to
   `Signature` in **constant time**. Accept on the first match; else `401`.
5. Check the subscriber has an active AI subscription (else `402`).
6. (Optional) reject a `Signature` already seen within the window.

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

## Reference client (Bash, `curl` + `openssl` only)

```sh
# inputs: $url (full), $secret, $key_id, $body
host=$(printf '%s' "$url" | sed -E 's#^[a-z]+://([^/]+).*#\1#')
path=$(printf '%s' "$url" | sed -E 's#^[a-z]+://[^/]+##; s#\?.*##')
query=$(printf '%s' "$url" | sed -nE 's#^[^?]*\?##p')
date=$(date -u +%Y-%m-%dT%H:%M:%SZ)
bodyhash=$(printf '%s' "$body" | openssl dgst -sha256 -hex | sed 's/^.*= *//')
sts=$(printf '%s\n%s\n%s\n%s\n%s\n%s' POST "$host" "$path" "$query" "$date" "$bodyhash")
sig=$(printf '%s' "$sts" | openssl dgst -sha256 -hmac "$secret" -binary | openssl base64 -A)
curl -fsS -X POST "$url" \
  -H "Content-Type: application/json" \
  -H "X-AI-Date: $date" \
  -H "Authorization: ELZA-AI-HMAC-SHA256 KeyId=$key_id,Signature=$sig" \
  --data-raw "$body"
```
