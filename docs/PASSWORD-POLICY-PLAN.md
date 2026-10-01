# Password mechanism upgrade — technical specification

> Implemented in #9963. Differences from this plan, the gaps and the
> postponed options (Phase 2) are listed in
> [password-policy-followup.md](password-policy-followup.md).

## Motivation

ELZA logs a warning when a user still has a password hash in the legacy format
("Uživatel X používá starý mechanismus ukládání hashe hesla…"), but offered no remedy.
The standard remedy — re-hashing the password on successful login — was missing and has
been implemented separately (**Phase 0**, see below). This task builds on it: it adds
password age tracking, a configurable password policy (strength + expiry), a forced
password change at login, and a recovery mechanism for a locked-out administrator.

Design constraint: ELZA is not a banking application. The goal is a modest upgrade of
the current mechanism with clean extension points, not a complete IAM subsystem.

## Current state (verified)

- Legacy hash: unsalted-per-user SHA-256 of `password + "{" + username + elza.security.salt + "}"`
  (`Sha256Support`), recognized by a stored value **not** starting with `{`. Because the
  username is part of the salt, renaming a user invalidates the hash (this is why
  `UserService.changeUser` throws `NEED_CHANGE_PASSWORD` on rename without a new password).
- Current encoder: `DelegatingPasswordEncoder` with `{bcrypt}` as default (static field in
  `UserService`); `UserService.encodePassword` always emits `{bcrypt}`.
- Password login: custom `PasswordAutheticationProvider` (last in the provider chain after
  SSO header / OAuth2 / LDAP / Kerberos), verification in `UserService.matchesPassword`.
- The default admin (`elza.security.allowDefaultUser` + `defaultUsername` + `defaultPassword`)
  has **no DB row** — `UserService.findAuthentication`/`createDefaultUser` fabricate
  unpersisted objects (`authenticationId == null` identifies them).
- `usr_authentication` (`authentication_id`, `user_id`, `auth_type` PASSWORD|SAML2,
  `auth_value`) and `usr_user` have no timestamps. No password policy exists anywhere;
  the admin change-password endpoint accepts even an empty password.
- Password writes funnel through `UserService.changePasswordPrivate` (self + admin REST)
  and `UserService.updateAuth` (user create/edit).

## Phase 0 — re-hash on login (DONE)

`UserService.upgradePasswordEncodingIfNeeded(authentication, rawPassword)`, called from
`PasswordAutheticationProvider.authenticate` right after a successful password match
(inside the existing `TransactionTemplate`):

- Skips the synthetic default-admin row (`authenticationId == null`) — persisting it
  would insert a bogus DB row for a config-defined account.
- Re-encodes when the stored value lacks the `{` prefix (legacy SHA-256), or when
  `DelegatingPasswordEncoder.upgradeEncoding` says so (covers future cost-factor bumps).
- Does not fire `changeUserEvent` (nothing client-visible changed).
- Unit test: `UserServicePasswordUpgradeTest`.

The WARN in `matchesPassword` stays: it now self-heals (fires once per user, at the
upgrading login) and remains meaningful for users who never log in.

**Important for this task:** the re-hash changes only the encoding, not the password —
it must NOT update the new `valid_from` column (otherwise every legacy user would get a
fresh expiry clock, defeating expiry for exactly the population that motivated this work).

## DB changes

One Liquibase changeset in `db/changelog/db.elza-3-part-03.xml` (id `yyyyMMddHHmmss`, author initials):

`usr_authentication`:

| column | type | notes |
|---|---|---|
| `valid_from` | timestamp NOT NULL | moment the current `auth_value` was established; backfill `2016-01-01T00:00:00` via column default, then drop the default (future inserts must set it in code). Named `valid_from`, **not** `created_at` — the row is updated in place on every password change. |
| `change_required` | boolean NOT NULL default false | operational state: admin requires a password change at next login (typical for temporary passwords) |
| `never_expire` | boolean NOT NULL default false | per-account exemption from expiry (technical/integration accounts) |

`usr_user`:

| column | type | notes |
|---|---|---|
| `created_at` | timestamp NOT NULL | row creation; backfill `2016-01-01T00:00:00`; set in `UserService.createUser`. Audit only — not used by expiry logic. |

`usr_policy` — new table, exactly one row seeded by the migration with everything off:

| column | type | notes |
|---|---|---|
| `policy_id` | int PK | |
| `password_expiry_days` | int null | password validity in days; null or <= 0 = no expiry |
| `password_min_length` | int null | minimum password length; null/<= 0 = no minimum |
| `password_min_char_groups` | int null | characters required from N of 4 groups (lowercase / uppercase / digits / other), 1–4; null/<= 0 = off |

Entity fields typed `OffsetDateTime` (follow `ArrExport.createdAt` precedent). SAML2 rows
also carry the NOT NULL columns (backfilled, set in `updateAuth`), but are never evaluated
by the expiry logic.

## Password strength model

`min_length` + "characters from N of 4 groups" (lowercase letters, uppercase letters,
digits, everything else). Deliberately **not** a mask/regex (opaque in admin UI, cannot
produce good Czech error messages) and **not** Passay (needless dependency for ~40 lines).
Each violation maps to a precise Czech message, e.g. "Heslo musí mít alespoň 8 znaků.",
"Heslo musí obsahovat znaky alespoň ze 3 skupin: malá písmena, velká písmena, číslice,
ostatní znaky."

## Expiry model

Computed dynamically — nothing expiry-related is stored per password:

```
needsChange(auth, policy, now) =
       auth.change_required
    || ( policy.password_expiry_days > 0
      && !auth.never_expire
      && auth.valid_from + password_expiry_days < now )
```

Evaluated **only** for `auth_type = PASSWORD` rows (explicit guard in code — SAML2 never
expires). No stored `valid_to`: it would have to be mass-recomputed whenever the admin
changes the policy, and "never expires" would need a sentinel date. The two flags answer
the original open questions: `never_expire` = policy exception (belongs to the account),
`change_required` = operational state (set by admin).

## Backend

- Entity `UsrPolicy` + repository + new `PasswordPolicyService`:
  `validate(String rawPassword)` and `needsChange(UsrAuthentication, now)` implementing
  the models above. The policy row is read at password login only — no caching needed.
  Keeping enforcement behind this service is the extension point for possible per-group
  policies later (Phase 2): only the resolver changes, callers don't.
- Validation funnel: new private `UserService.encodeNewPassword(raw)` =
  `passwordPolicyService.validate(raw)` + `encodePassword(raw)`, used in **both**
  `changePasswordPrivate` and the PASSWORD branch of `updateAuth`. This also fixes the
  hole where the admin endpoint accepts an empty password. The admin-set path gets **no
  policy exemption** — a weak temporary password should be paired with `change_required`,
  not bypass the policy.
- New exception code `UserCode.PASSWORD_POLICY_VIOLATION` with `minLength` /
  `minCharGroups` properties (client toaster key `exception.usr.PASSWORD_POLICY_VIOLATION`).
- `needChangePassword` flag:
  - Set **only** in `PasswordAutheticationProvider` after a successful password login
    (evaluate `needsChange`). This placement structurally answers multi-auth: users
    logging in via SSO/LDAP/Kerberos/SAML2 are never nagged about a password they may not
    even know. The synthetic default admin (`authenticationId == null`) never gets the
    flag — its password lives in `elza.yaml` and cannot be changed via the API (would be
    a lockout loop).
  - Carried on the session object `security/UserDetail` → copied in the
    `UsrUserVO(UserDetail)` constructor → `UserInfoVO` (returned by `GET /api/user/detail`).
  - Cleared on self password change: `changePasswordPrivate` sets
    `change_required = false` and `valid_from = now()`; the controller also flips the
    session flag. The existing `changeUserEvent` websocket → client `reloadUserDetail`
    refreshes the store, closing the dialog.
- Admin `ChangePassword` DTO (`UserController`, nested class): optional `changeRequired`
  and `neverExpire` booleans.
- New admin endpoints GET/PUT for the password policy — authored via the TypeSpec →
  OpenAPI pipeline (schema name `PasswordPolicy`, no VO suffix; add the pom
  `modelNameMapping` entry), TS client regenerated.
- `UserInfoVO` additionally carries `passwordPolicy { minLength, minCharGroups }` so the
  self-change form can validate client-side (non-admins cannot call the GET endpoint).

## Admin password recovery (temporary config option)

Scenario: an administrator forgot their password and no other administrator can reset it
via the UI.

- New properties `elza.security.recovery.username` + `elza.security.recovery.password`
  (plaintext, transient break-glass; both must be set; default unset). Mirrors the
  existing `elza.security.defaultPassword` config pattern.
- The check lives in `UserService.matchesPassword` (it already receives the username, and
  its only two callers — password login and the old-password check in self-change — are
  exactly where recovery must apply): if recovery is configured and the username and
  password match the configured pair → success.
- A recovery login sets `needChangePassword = true` on the session → the forced-change
  dialog opens immediately; the recovery password is accepted as the "old password"; the
  new password is stored as `{bcrypt}` through the normal funnel.
- WARN at startup and on each use while the option is active ("Recovery přihlášení je
  aktivní pro uživatele X — po použití odstraňte volbu z konfigurace!").
- Works only for users that already have a PASSWORD auth row (SAML2-only accounts are out
  of scope — managed by their IdP, or another admin adds password auth first).
- Documented procedure: set options → restart → log in as the user → change the password
  in the forced dialog → remove the options → restart.
- The default admin is NOT recovered this way — its password lives in
  `elza.security.defaultPassword` (config edit).

## Frontend

- **Policy administration**: ribbon button on `AdminUserPage` (permission `USR_PERM`) →
  small modal form with the 3 policy fields. New functional `.tsx` component
  (react-final-form, react-intl strings), calling the generated TS client.
- **Forced-change dialog**: at the `Layout.jsx` level where `Login.jsx` is mounted — when
  `login.logged && userDetail.needChangePassword`, render a non-dismissible modal (same
  `ModalDialogWrapper` pattern as `Login`: `backdrop: 'static'`, no close button) hosting
  the self-change password form. Copy: "Platnost hesla vypršela. Pro pokračování si
  nastavte nové heslo."
  Login itself always succeeds — the flag never produces a 401/403, which also avoids the
  client's automatic logout on 401 (`AjaxUtils.resolveException`). Enforcement is
  client-only in this phase (see Phase 2).
- **`PasswordForm.jsx`**: client-side validation of min length / char groups from
  `userDetail.passwordPolicy` with per-field Czech errors; the admin variant gets
  checkboxes "Vyžadovat změnu hesla při dalším přihlášení" (`changeRequired`) and
  "Heslo bez expirace" (`neverExpire`). Keep the component as redux-form — minimal edits;
  migration to react-final-form is orthogonal churn and not part of this task.
- **Hand-maintained TS mirrors** (this controller is outside the OpenAPI pipeline!):
  `src/api/UserInfoVO.ts`, `src/typings/store/UserDetail.types.ts`,
  `src/stores/app/user/userDetail.jsx`. Forgetting them fails silently (undefined flag).

## Tests

- Password validation matrix (lengths, group counts, edge values).
- `needsChange`: expired / `never_expire` / `change_required` / SAML2 row / synthetic admin.
- Phase 0 regression: re-hash on login does NOT change `valid_from`.
- Round-trip: self password change clears `change_required`, refreshes `valid_from`,
  clears the session flag.
- Recovery: login with recovery pair, forced change, recovery accepted as old password.

## Deployment / release notes

- **Enabling `password_expiry_days` immediately expires ALL passwords last changed before
  the upgrade** (2016-01-01 backfill). This is intended (those passwords are old), but it
  must be stated loudly in release notes — on the first login after enabling the policy,
  every such user is forced to change their password.
- Tip: a `{bcrypt}`-prefixed value can be stored in `elza.security.defaultPassword`
  (works already today) — a legacy-format default password otherwise logs the WARN on
  every login and can never self-heal.
- SQL to list users still on the legacy hash format:
  ```sql
  SELECT u.username FROM usr_user u
  JOIN usr_authentication a ON a.user_id = u.user_id
  WHERE a.auth_type = 'PASSWORD' AND a.auth_value NOT LIKE '{%';
  ```

## Phase 2 — documented options, explicitly NOT implemented now

- Per-group password policies (multiple `usr_policy` rows + priority semantics) — only if
  a real requirement appears; swap the resolver in `PasswordPolicyService`, callers unchanged.
- Server-side blocking of requests for sessions with an expired password:
  `OncePerRequestFilter` checking `UserDetail.needChangePassword`, whitelist
  `/api/user/detail`, `PUT /api/user/password`, `/logout`, websocket endpoint — returning
  **403, never 401** (the client auto-logs-out on 401).
- Password history ("not one of the last N") — new `usr_password_history` table.
- Login throttling / lockout — natural attachment points already exist:
  `DeferredFailureAuthenticationManager` brackets every attempt,
  `SiemAuditLogger.loginFailed` records failures.
- Relax the rename → `NEED_CHANGE_PASSWORD` rule for `{`-prefixed hashes (the username is
  only baked into the legacy salt).
- Legacy-format sunset: after N months set `change_required = true` for remaining
  `NOT LIKE '{%'` rows; eventually delete `Sha256Support` and the `elza.security.salt`
  property.

## Related follow-up task (separate)

First-run wizard + removal of the synthetic default admin: on an empty `usr_user` table
enter a setup mode and create the first superadmin (including its `ap_access_point` —
real scope: requires packages/AP types, a setup-mode UI outside normal auth, install docs
and distrib updates); then flip `allowDefaultUser` default to false and remove the
synthetic-admin mechanism (`createDefaultUser`, the fabricated row in
`findAuthentication`) together with the null-id guards introduced by this task. Admin
password recovery is already covered by `elza.security.recovery.*` from this task.
