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
plan postponed (Phase 2), and the state of the first-run setup.


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


First-run setup
---------------

Implemented: while `usr_user` is empty, the client shows the *Initial setup*
dialog (`SetupWizard.tsx`, from `Login.tsx`) instead of the login, and
`SetupService` creates the first administrator through `GET /api/v1/setup`
and `POST /api/v1/setup/admin` (open without login). The administrator's
view is in `admin-guide/source/02-installation.rst` ("First administrator").

- The endpoints are open to anyone while no user exists; by the architect's
  decision there is no setup key, the administrator creates the first user
  before the application is made available on the network. The check that
  no user exists and the creation run under one lock in one transaction.
- `elza.security.admin.username` / `password` (plain text) are applied at
  every startup by `SetupService.applyAdminFromConfiguration`: a missing
  user is created as an administrator (`UserService.createInitialAdmin`),
  an existing user gets the password (`UserService.applyConfiguredPassword`;
  nothing is written when it already has it, so `valid_from` does not
  restart); a deactivated user is activated. Permissions of an existing
  user do not change. Every outcome is logged; the changes and a failure
  also go to the SIEM log (`SiemAuditLogger.configAdminApplied` /
  `configAdminFailed`, events `config_admin_*`).
- The first administrator has **no access point**: on an empty database
  there are no packages, so no access point types. `usr_user.access_point_id`
  was nullable in the database already; the JPA mapping, `changeUser`,
  `CamUserService`, `UserRepository.findOneWithDetail` and the user search
  (`UserRepositoryImpl`, left joins) were adapted. A regular user is still
  created with an access point.
- The default user stays as it is; the dialog offers *Log in as the default
  user* while it is enabled, and the first administrator cannot be named
  like the default user.

Remaining:

- **Integration test of the setup.** `SetupServiceTest` and
  `UserServiceInitialAdminTest` are unit tests with mocks; a test on an
  empty database (status, wrong key, creation, the user found in the search
  and able to load `GET /api/user/detail`) is missing.
- **Removal of the default user.** Switch the default of
  `elza.security.allowDefaultUser` to `false` (`UserService`,
  `ElzaWebController`, `elza-web/config/elza.yaml.template`, the guide) and
  remove the default user mechanism (`UserService.createDefaultUser`, the
  fabricated row in `findAuthentication`, `createAdminUserDetail`) together
  with the `authenticationId == null` / `userDetail.getId() == null` guards
  added by #9963 and the *Sign in* button of the setup.
  Installations that run only on the default user must create their
  administrator first - the release notes have to say so. Recovering an
  administrator's access is covered by `elza.security.recovery.*`.
- **Person of the first administrator.** It can be assigned later in
  *Administration* > *Users* > *Edit*; nothing reminds the administrator
  to do so.
