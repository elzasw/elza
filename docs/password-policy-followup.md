Password policy - remaining work
================================

Task #9963 implemented the password policy described in
[PASSWORD-POLICY-PLAN.md](PASSWORD-POLICY-PLAN.md): password age tracking,
password rules (minimum length, character groups, validity), a required
password change at the next login, accounts whose password never expires,
and recovering access with `elza.security.recovery.*`. The administrator's
view is documented in the administration guide,
`admin-guide/source/05-security.rst`.

This document lists what was left out: small gaps of #9963, the options the
plan postponed (Phase 2), and the related follow-up task.


Differences from the plan
-------------------------

What was implemented differently from the plan, so that the plan is not
read as the description of the code:

- `needChangePassword` and `passwordPolicy` are carried by `UserInfoVO`
  (`GET /api/user/detail`), not by `UsrUserVO`. `UsrUserVO` is also used
  for user lists, where a session flag would be misleading.
- `UsrUserVO` carries `passwordChangeRequired` and `passwordNeverExpire`
  (filled in `ClientFactoryVO.createUser`), so the administrator's password
  dialog shows the current state of both flags.
- `UserCode.PASSWORD_POLICY_VIOLATION` carries the property `rule`
  (`minLength` | `minCharGroups`) next to the violated limit; the client
  message selects its text by it.
- The password rules dialog (`PasswordPolicyForm.tsx`) is a Fluent dialog
  with local state, not react-final-form. The forced change dialog
  (`ForcedPasswordChange.tsx`) is a Fluent `Dialog` with
  `modalType="alert"`, not `ModalDialogWrapper`.
- The default user (no DB row) cannot change its password through the
  application: the menu item is hidden, and the service and the self-change
  endpoint reject the request with an explicit message.


Gaps of #9963
-------------

- **Test of the session flag.** `UserServicePasswordChangeTest` covers the
  service part of a self password change (`change_required` cleared,
  `valid_from` refreshed). That `UserController.changePassword` clears
  `UserDetail.needChangePassword` in the session is verified manually only;
  an integration test in `UserControllerTest` would cover it.
- **Client-side check when creating or editing a user.** `AddUserForm.jsx`
  does not check a new password against the rules; the server rejects a
  weak password and the error is shown as a toast. `PasswordForm.jsx` uses
  `checkPasswordPolicy` from `components/admin/passwordPolicy.ts`, which
  can be reused.
- **Setting the flags without a password change.** `change_required` and
  `never_expire` can be set only together with a new password
  (`PUT /api/user/{userId}/password`). Marking an existing password as
  never expiring, or requiring a change without handing over a new
  password, needs a separate endpoint and UI action.
- **Requiring a change when creating a user.** The create user dialog has
  no *Require a password change at next login* option; the administrator
  has to set the password once more with the option.


Phase 2 - postponed options
---------------------------

Not implemented on purpose; each is to be done only when a real
requirement appears.

- **Password rules per user group.** Several `usr_policy` rows with
  priority semantics. All callers go through `PasswordPolicyService`
  (`validate`, `needsChange`), so only the resolution of the policy in
  `getPolicy()` changes; the callers stay as they are.
- **Server-side blocking of a session with an expired password.** Today
  the change is enforced by the web client only; the REST API is not
  blocked. An `OncePerRequestFilter` would check
  `UserDetail.isNeedChangePassword()` and let through only
  `GET /api/user/detail`, `PUT /api/user/password`, `/logout` and the
  websocket endpoint. It must answer **403, never 401**: the client logs
  out automatically on 401 (`AjaxUtils.resolveException`).
- **Password history** ("not one of the last N passwords"). A new table
  `usr_password_history`; checked in `UserService.encodeNewPassword`.
- **Login throttling / lockout.** `DeferredFailureAuthenticationManager`
  brackets every login attempt and `SiemAuditLogger.loginFailed` records
  failures - the natural places to count attempts.
- **Renaming a user without a new password.** `UserService.changeUser`
  throws `NEED_CHANGE_PASSWORD` on a rename, because the user name is part
  of the salt of the old SHA-256 hash. For `{`-prefixed (bcrypt, scrypt)
  hashes the rule can be relaxed.
- **Sunset of the old hash format.** After some months, set
  `change_required = true` for the remaining rows with
  `auth_value NOT LIKE '{%'`; later remove `Sha256Support` and the
  `elza.security.salt` property.


Related follow-up task (separate)
---------------------------------

**First-run setup and removal of the default user.** On an empty `usr_user`
table, enter a setup mode and create the first administrator, including
its `ap_access_point`. The real scope is larger than it looks: it needs
packages and access point types, a setup-mode UI outside the normal
authentication, installation documentation and distribution updates.
Then switch the default of `elza.security.allowDefaultUser` to `false` and
remove the default user mechanism (`UserService.createDefaultUser`, the
fabricated row in `findAuthentication`) together with the
`authenticationId == null` / `userDetail.getId() == null` guards added by
#9963. Recovering an administrator's access is already covered by
`elza.security.recovery.*`.
