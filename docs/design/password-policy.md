# Password policy

Password age tracking, a configurable password policy (strength + expiry), a
required password change at login, recovery of a locked-out administrator,
and transparent re-hashing of legacy password hashes. The administrator's
view is in the administration guide, `admin-guide/source/05-security.rst`.

Design constraint: ELZA is not a banking application. The goal is a modest
upgrade of the password mechanism with clean extension points, not a complete
IAM subsystem.

## Background: hash formats

- **Current format:** `DelegatingPasswordEncoder` with `{bcrypt}` as default;
  `UserService.encodePassword` always emits `{bcrypt}`.
- **Legacy format (still recognized):** SHA-256 of
  `password + "{" + username + elza.security.salt + "}"` (`Sha256Support`),
  recognized by a stored value **not** starting with `{`. The username is part
  of the salt, so renaming a user invalidates the hash — this is why
  `UserService.changeUser` throws `NEED_CHANGE_PASSWORD` on a rename without a
  new password. `matchesPassword` logs a WARN for every legacy-format login.
- Password login goes through `PasswordAutheticationProvider` (last in the
  provider chain after SSO header / OAuth2 / LDAP / Kerberos); verification in
  `UserService.matchesPassword`.
- The default user (`elza.security.allowDefaultUser` + `defaultUsername` +
  `defaultPassword`) has **no DB row**: `UserService` fabricates unpersisted
  objects for it, identified by `authenticationId == null` /
  `UserDetail.getId() == null`.

## Re-hash on login

`UserService.upgradePasswordEncodingIfNeeded`, called by
`PasswordAutheticationProvider.authenticate` right after a successful password
match (inside its transaction), re-encodes the stored hash when it lacks the
`{` prefix or when `DelegatingPasswordEncoder.upgradeEncoding` asks for it
(covers future cost-factor changes). It skips the synthetic default user
(persisting it would insert a bogus row for a config-defined account) and
fires no `changeUserEvent` (nothing client-visible changed).

The re-hash changes only the encoding, not the password, so it does **not**
touch `valid_from`. Otherwise every legacy user would get a fresh expiry clock,
defeating expiry for exactly the population that needs it.

## Data model

Liquibase changeset `20260930100000` in `db.elza-3-part-03.xml`.

`usr_authentication`:

| column | notes |
|---|---|
| `valid_from` timestamptz NOT NULL | moment the current `auth_value` was set. Existing rows backfilled with 2016-01-01, the default then dropped (code sets it). Named `valid_from`, not `created_at`: the row is updated in place on every password change. |
| `change_required` boolean NOT NULL | operational state: the administrator requires a change at the next login (typical for temporary passwords) |
| `never_expire` boolean NOT NULL | per-account exemption from expiry (technical/integration accounts) |

`usr_user.created_at` timestamptz NOT NULL — audit only, not used by expiry.

`usr_policy` — a single row (`policy_id = 1`) seeded with everything off:
`password_expiry_days`, `password_min_length`, `password_min_char_groups`
(all nullable int; null or ≤ 0 turns the rule off; char groups 1–4).

SAML2 rows carry the NOT NULL columns too but are never evaluated.

## Strength model

`min_length` (counted in code points) + "characters from N of 4 groups"
(lowercase, uppercase, digits, everything else). Deliberately **not** a
mask/regex (opaque in the admin UI, cannot produce good error messages) and
**not** Passay (a needless dependency for ~40 lines). Each violation raises
`UserCode.PASSWORD_POLICY_VIOLATION` with the property `rule`
(`minLength` | `minCharGroups`) and the violated limit; the client selects its
message by `rule`. An empty password is always rejected.

## Expiry model

Computed at login, nothing expiry-related is stored per password
(`PasswordPolicyService.needsChange`):

```
needsChange = auth is a persisted PASSWORD row
           && ( change_required
             || ( !never_expire && expiry_days > 0
                  && valid_from + expiry_days < now ) )
```

No stored `valid_to`: it would have to be recomputed for all rows whenever the
policy changes, and "never expires" would need a sentinel date.
`never_expire` is a policy exception belonging to the account;
`change_required` is operational state set by the administrator.

## Backend

- `PasswordPolicyService` (`validate`, `needsChange`, `getPolicy`,
  `updatePolicy`). All callers go through it, so per-group policies would only
  change `getPolicy()`. The policy row is read at password login and on
  password writes; no caching.
- **One validation funnel:** `UserService.encodeNewPassword` = `validate` +
  `encodePassword`, used by the self change, the admin change and the
  PASSWORD branch of user create/edit. Passwords set by the administrator get
  no exemption — a weak temporary password should be paired with
  `change_required`, not bypass the policy.
- **`needChangePassword` flag** is evaluated **only** in
  `PasswordAutheticationProvider` after a successful password login. Users
  logging in via SSO/LDAP/Kerberos/SAML2 are therefore never asked to change a
  password they may not even know. The default user never gets the flag (its
  password lives in `elza.yaml`; requiring a change would be a lockout loop).
  The flag lives on the session `UserDetail` and reaches the client via
  `UserInfoVO` (`GET /api/user/detail`), not `UsrUserVO` — `UsrUserVO` is also
  used in user lists, where a session flag would be misleading.
- `UserInfoVO.passwordPolicy` (min length, char groups) lets the self-change
  form validate client-side; non-administrators cannot call the policy GET.
- **Self change** (`PUT /api/user/password`) sets `change_required = false`,
  `valid_from = now` and clears the session flag; the `changeUserEvent`
  WebSocket message makes the client reload the user detail, which closes the
  forced-change dialog. The default user cannot change its password through
  the application (menu item hidden, service and endpoint reject it).
- **Admin change** (`PUT /api/user/{userId}/password`) takes optional
  `changeRequired` and `neverExpire`. `UsrUserVO` carries
  `passwordChangeRequired` / `passwordNeverExpire` so the admin dialog shows
  the current state.
- Policy administration: `GET`/`PUT /admin/password-policy` (OpenAPI schema
  `PasswordPolicy`), permission `USR_PERM`.

## Administrator recovery

For an administrator who forgot the password when no other administrator can
reset it. `elza.security.recovery.username` + `.password` (plain text, both
required, unset by default) — a transient break-glass mirroring the
`defaultPassword` pattern.

- The check is in `UserService.matchesPassword`, whose two callers — password
  login and the old-password check of the self change — are exactly where
  recovery must apply. The comparison is constant-time; it never applies to the
  default user's name.
- A recovery login gets `needChangePassword = true`, so the forced-change
  dialog opens at once; the recovery password is accepted as the old password
  and the new one goes through the normal funnel.
- A WARN is logged at startup and on each use while the option is set.
- Works only for users that already have a PASSWORD row (SAML2-only accounts
  are managed by their IdP).
- Procedure: set the options → restart → log in → change the password in the
  forced dialog → remove the options → restart.

## Frontend

- **Policy administration:** ribbon button on `AdminUserPage` →
  `PasswordPolicyForm.tsx` (Fluent dialog).
- **Forced change:** `ForcedPasswordChange.tsx`, a non-dismissible Fluent
  `Dialog` (`modalType="alert"`) shown from `Layout.jsx` when the user is
  logged in and `needChangePassword` is set. Login itself always succeeds — the
  flag never produces 401/403, which also avoids the client's automatic logout
  on 401 (`AjaxUtils.resolveException`). Enforcement is therefore client-only.
- **`PasswordForm.jsx`** validates via `checkPasswordPolicy`
  (`components/admin/passwordPolicy.ts`); the admin variant has the
  "require change at next login" and "never expires" checkboxes.
- `UserController` is outside the OpenAPI pipeline: `UserInfoVO` and the user
  detail store are mirrored by hand on the client (`src/api/UserInfoVO.ts`,
  `src/typings/store/UserDetail.types.ts`,
  `src/stores/app/user/userDetail.jsx`). A missing field there fails silently.

## First administrator

While `usr_user` is empty, the client shows the setup dialog
(`SetupWizard.tsx`) instead of the login, and `SetupService` creates the first
administrator via `GET /api/v1/setup` and `POST /api/v1/setup/admin`.

- The endpoints are open while no user exists; there is deliberately no setup
  key — the first user is created before the application is exposed on the
  network. The emptiness check and the creation run under one lock in one
  transaction.
- `elza.security.admin.username` / `.password` are applied at every startup
  (`SetupService.applyAdminFromConfiguration`): a missing user is created as
  an administrator, an existing one gets the password (nothing is written when
  it already matches, so `valid_from` does not restart), a deactivated one is
  activated; permissions of an existing user are not changed. Outcomes are
  logged, changes and failures also to the SIEM log (`config_admin_*`).
- The first administrator has **no access point** (an empty database has no
  packages, hence no access point types); `usr_user.access_point_id` is
  nullable and user loading/search use left joins. Regular users are still
  created with an access point.

## Deployment notes

- **Enabling `password_expiry_days` immediately expires every password last
  changed before the upgrade** (2016-01-01 backfill). This is intended, but
  release notes must say so: those users are forced to change the password at
  their next login.
- A `{bcrypt}`-prefixed value can be stored in `elza.security.defaultPassword`;
  a legacy-format default password logs the WARN on every login and never
  self-heals.
- Users still on the legacy hash format:
  ```sql
  SELECT u.username FROM usr_user u
  JOIN usr_authentication a ON a.user_id = u.user_id
  WHERE a.auth_type = 'PASSWORD' AND a.auth_value NOT LIKE '{%';
  ```

## Open work

Gaps:

- **Test of the session flag:** `UserServicePasswordChangeTest` covers the
  service part of a self change; that `UserController.changePassword` clears
  `UserDetail.needChangePassword` has no integration test (`UserControllerTest`).
- **Client-side check when creating/editing a user:** `AddUserForm.jsx` does
  not validate the password against the policy (the server rejects it with a
  toast); `checkPasswordPolicy` can be reused.
- **Setting the flags without a password change:** `change_required` and
  `never_expire` can only be set together with a new password; a separate
  endpoint and UI action are needed.
- **Requiring a change when creating a user:** the create dialog lacks the
  option; the administrator has to set the password once more.
- **Integration test of the first-administrator setup** on an empty database
  (`SetupServiceTest`, `UserServiceInitialAdminTest` are mock-based).
- **Person of the first administrator** can be assigned later in
  Administration > Users > Edit; nothing reminds the administrator.
- **Removal of the default user:** switch the default of
  `elza.security.allowDefaultUser` to `false` (`UserService`,
  `ElzaWebController`, `ApiKeyController`, `elza.yaml.template`, admin
  guide), remove
  `createDefaultUser`, the fabricated row in `findAuthentication`,
  `createAdminUserDetail`, the `authenticationId == null` /
  `getId() == null` guards and the setup dialog's default-user sign-in.
  Installations running only on the default user must create an administrator
  first — release notes must say so.

Options, to be done only when a real requirement appears:

- **Per-group policies:** several `usr_policy` rows with priority semantics;
  only `PasswordPolicyService.getPolicy()` changes.
- **Server-side blocking of sessions with an expired password:** an
  `OncePerRequestFilter` checking `UserDetail.isNeedChangePassword()`,
  allowing only `GET /api/user/detail`, `PUT /api/user/password`, `/logout` and
  the WebSocket endpoint. It must answer **403, never 401** (the client logs
  out on 401).
- **Password history** ("not one of the last N"): a `usr_password_history`
  table, checked in `UserService.encodeNewPassword`.
- **Login throttling / lockout:** `DeferredFailureAuthenticationManager`
  brackets every attempt and `SiemAuditLogger.loginFailed` records failures —
  the natural places to count attempts.
- **Rename without a new password** for `{`-prefixed hashes (only the legacy
  hash bakes the username into the salt).
- **Sunset of the legacy hash format:** after some months set
  `change_required = true` for the remaining `NOT LIKE '{%'` rows; later
  remove `Sha256Support` and `elza.security.salt`.
