Personal API Keys
=================

Machine clients (integrations, scripts, third-party tools) call Elza's REST API
without knowing a user's password by sending a personal API key in the
`X-API-Key` header. The key is bound to one user account, has a required
expiration (default 365 days, configurable up to 730), and can be revoked at
any time. Only the SHA-256 hash of the secret part is stored; the full token
is shown to the user once at creation and cannot be retrieved after.


How to obtain a key
-------------------

The React UI does not use API keys — the key owner logs in interactively and
creates a key from **User settings → Přístupové klíče API → Nový klíč**. The
dialog shows the full token once with a copy-to-clipboard button; after
closing it the token is unrecoverable and a new key must be issued instead.

Administrators with `USR_PERM` or `USER_CONTROL_ENTITY` over the given user
can list and revoke the user's keys under **Administration → Users →
detail → Přístupové klíče API**, but never see the token value — issuing a
key is always done by the owner.


Using the key
-------------

Send the token in the `X-API-Key` header on every request. The connection is
stateless: no login step, no session cookie.

    curl -H "X-API-Key: elza_k7Qm2xP9aB4c_Zt7mQb4Vw1XcR9sK2fLh8PnU3oJdY6gEaTiC0rMx" \
         https://elza.archiv.cz/api/v1/publications/available/PROD

A rejected request returns `401` with a JSON body whose `code` distinguishes
the reason (`MALFORMED_TOKEN`, `UNKNOWN_KEY`, `INVALID_SECRET`, `EXPIRED`,
`REVOKED`, `USER_INACTIVE`); the `message` field is human-readable text.


Limits of a key-authenticated request
-------------------------------------

The security context matches an interactive login of the same user: existing
`@AuthMethod` and `UsrPermission` checks apply unchanged, and audit
(`arr_change.user_id`) records the owner. To prevent escalation into
permanent access, a request authenticated by an API key cannot:

  * change the user's password
  * create, list, or revoke API keys (own or anyone else's)
  * modify authentication credentials of other users

These operations return `403` when reached through an API key.


Configuration
-------------

`elza.security.api-keys` in `elza.yaml`:

  * `enabled` — default `true`. `false` makes the server ignore the header on
    every request.
  * `header-name` — default `X-API-Key`. Changing it requires regenerating
    OpenAPI clients that mention the header explicitly.
  * `default-validity-days` — default `365`. Applied when the user does not
    choose an expiration date.
  * `max-validity-days` — default `730`. Upper bound the UI and API accept
    for a new key's validity; rejected with `400` if exceeded.

Deactivating a user (`usr_user.active = false`) invalidates all of their keys
automatically. Changing a password does not.
