# Security audit log (SIEM)

ELZA writes authentication, account and fund events to a separate JSON log that a
SIEM system (Wazuh, Elastic, Splunk, ...) reads through its agent. The
administrator's view (enabling the log, list of fields and events) is in
`admin-guide/source/05-security.rst`, section "Security audit log"; running
behind a proxy is in `admin-guide/source/06-reverse-proxy.rst`, section
"Address of the client".

## Where it is written

- `SiemAuditLogger` (`cz.tacr.elza.security`) is the only writer. Callers use
  its typed methods (`loginSuccess`, `loginFailed`, `apiKeyCreated`,
  `configAdminApplied`, ...) and never build an event themselves, so the field
  names stay consistent across the system.
- Logger `siem.audit`, appender `SIEM_FILE` in `logback-spring.xml`
  (`LogstashEncoder`, one JSON object per line, rotated daily, 90 days).
- The log is off unless `elza.siemLogFile` is set: the appender is defined
  only then, and `SiemAuditLogger.init()` sets the `siem.audit` logger to
  `OFF`, so no event is built in vain.
- Events are logged at `INFO`. Severity is decided by the SIEM rules, not by
  ELZA; `level` is only informative.

## Format

Field names follow the **OSSEC/Wazuh** schema. They come from a proposal
written by the SIEM administrator of an archive that runs ELZA ("Auditní
logovací schéma", ver. 1.0), which also covers events ELZA does not have
(self-registration, e-mail verification, GDPR consent).

| field | source | rule |
|---|---|---|
| `timestamp` | encoder (`@timestamp` renamed) | ISO 8601 |
| `msg` | encoder (`message` renamed) | short English sentence ending with a full stop, e.g. `User martin logged in from IP 192.168.1.10.` |
| `level` | encoder | always `INFO` |
| `program_name` | encoder custom field | always `elza` |
| `log_type` | event | `secure` = authentication, `audit` = changes of users and their access |
| `event_type`, `action` | event | `event_type` is the kind (`user_login`), `action` the operation (`login`); snake_case |
| `result` | event | `success` / `failed` |
| `srcuser` | event | who performed the action: the logged user (`auth.getName()`); `configuration` for changes from `elza.security.override`, `setup` for the initial setup dialog, `system` for a system security context (no principal) |
| `dstuser` | event | whom the action concerns; equals `srcuser` for a login |
| `srcip` | event | client address: passed by the caller (authentication providers), otherwise taken from the current HTTP request (`RequestContextHolder`); omitted outside a request (startup) |
| `reason` | event | only with `result` = `failed`; always a stable code, never an exception message (that goes to `details`), see "Reasons of a failed login" |
| `details` | event | optional technical English text of a failure, e.g. the message of a rejected JWT (never the token) |
| `dstgroup` | event | code of the group the action concerns; not an OSSEC field (OSSEC has none for a group) |
| others | event | event-specific, snake_case: `authentication_type`, `key_id`, `expire_date`, `permission`, `permission_target`, `fund_id`, `fund_name`, `fund_import`, `old_username` |

The encoder drops everything a SIEM cannot use: logback context properties
(`includeContext=false` — otherwise `LOG_FILE`, `SIEM_LOG_FILE` and `HOSTNAME`
appear in every line, because `logback-spring.xml` declares them with
`scope="context"`), `@version`, `logger_name`, `thread_name`, `level_value`.
The SIEM agent adds the host and the file location itself.

Null values are omitted rather than written as `null` (`addIfNotNull`).

### Why the OSSEC names

There is no field schema that every SIEM understands. Each has its own
(OSSEC/Wazuh `srcip`, Elastic ECS `source.ip`, Splunk CIM `src`, Sentinel ASIM
`SrcIpAddr`, OCSF `src_endpoint.ip`), and for any SIEM other than the "native"
one the administrator writes a field mapping anyway. The choice therefore only
decides who gets the log without mapping:

- Wazuh is the SIEM of the archive that asked for the change and is common in
  the Czech public sector (free, used for the cyber-security act).
- The Wazuh JSON decoder maps top-level `srcip`, `srcuser`, `dstuser` to its
  static fields, which the correlation rules use (`<same_srcip/>`,
  `<same_user/>` — e.g. repeated failed logins from one address). With other
  names those rules do not see the values.
- Flat names are as easy to map into ECS or CIM as any other names.

Rejected: **ECS** (the most widespread schema, a ready Java encoder exists) —
it would require a custom decoder in Wazuh, i.e. for the only actual user.
**Configurable schema** — extra complexity without a second user asking for
it.

Wazuh still needs a few ELZA-specific rules (matching `program_name: elza`
and assigning the groups `authentication_success` / `authentication_failed`);
the built-in rules do not classify unknown applications by themselves.

## Client address behind a proxy

`srcip` comes from `WebAuthenticationDetails.getRemoteAddress()`, which Spring
Security fills from `request.getRemoteAddr()` when it starts the
authentication. Behind a reverse proxy that is the proxy's address.

With `elza.security.acceptForwardedHeaders: true`, `ElzaWebApp` registers
Spring's `ForwardedHeaderFilter` through a `FilterRegistrationBean` with
`Ordered.HIGHEST_PRECEDENCE` (dispatcher types `REQUEST`, `ASYNC`, `ERROR`),
the same way Spring Boot does for `server.forward-headers-strategy=framework`.
It must run before the Spring Security filter chain (order `-100`); a plain
`Filter` bean gets `LOWEST_PRECEDENCE` and would rewrite the request only
after the address had been recorded. Spring Security also builds redirects
from the rewritten host, protocol and prefix.

**Trust of `X-Forwarded-For`.** `ForwardedHeaderFilter` takes the **first**
(leftmost) value of `X-Forwarded-For` (`ForwardedHeaderUtils.parseForwardedFor`).
A proxy that appends to the header (NGINX `$proxy_add_x_forwarded_for`, Apache
`mod_proxy` by default) lets the client choose the logged address by sending
its own header — which would defeat per-address brute-force rules. The proxy
therefore has to **replace** the header (NGINX `$remote_addr`, Apache
`RequestHeader unset X-Forwarded-For early`); the admin guide says so.

Rejected:

- **Tomcat `RemoteIpValve`** (`server.forward-headers-strategy=native`,
  `server.tomcat.remoteip.*`) — takes the rightmost untrusted address and is
  safe without proxy configuration, but `server.*` properties apply only to
  the embedded server; ELZA is also deployed as a WAR (`ElzaInit`), where the
  valve would have to be configured in Tomcat's `server.xml`.
- **Own filter picking the rightmost address not in a list of trusted
  proxies** — safe regardless of the proxy, but more code and a new setting;
  replacing the header in the proxy is a one-line configuration.

## Adding an event

- Add a method to `SiemAuditLogger` and build the event with `event(...)`,
  which sets `log_type`, `event_type`, `action`, `result`, `srcuser`,
  `dstuser` and `srcip`.
- Reuse an existing `event_type` / `action` pair when the meaning is the same
  (e.g. `user_create` regardless of who creates the user); prefer the names of
  the proposal above for events it lists (`user_logout`, `password_change`,
  `user_disable`, `permission_grant`, `permission_revoke`).
- Never log secrets: passwords, API-key secrets, tokens. The attempted user
  name of a failed login is logged (standard practice), although users
  sometimes type a password into it.
- Extend the event table in `05-security.rst`.
- Renaming a field or an `event_type` breaks parsers and rules of every
  installation that reads the log; mention it in the release notes.

## Reasons of a failed login

Every provider logs a failure with its `authentication_type` and one of these
codes in `reason`; the exception message, if useful, goes to `details`:

| code | meaning | providers |
|---|---|---|
| `INVALID_USERNAME` | no such user in ELZA (for AD/Kerberos: the external authentication succeeded, but ELZA has no user) | all except API key |
| `INVALID_PASSWORD` | wrong password of an existing user | password |
| `INACTIVE_USER` | the ELZA user is deactivated (`LockedException` from `UserService.createUserDetail`) | all except API key (`USER_INACTIVE` there) |
| `INVALID_CREDENTIALS` | the SSO header request does not carry the filter's credentials | SSO header |
| `INVALID_TOKEN`, `TOKEN_DECODING_FAILED` | see "Stateless authentication" | JWT |
| `MALFORMED_TOKEN`, `UNKNOWN_KEY`, ... | API-key codes, see [api-keys.md](api-keys.md) | API key |
| `AUTHENTICATION_ERROR` | unexpected error, message in `details` | AD, JWT |
| `OLD_PASSWORD_MISMATCH` | (`password_change`, not a login) | — |

**Each failed form login is logged once.** An AD or Kerberos-password
provider that cannot verify the credentials returns `null` without logging,
so that the next provider can try; `PasswordAutheticationProvider` is always
the last one in `ApplicationSecurity.authenticationManagerBean` and logs the
failure (`INVALID_USERNAME` for a user without password authentication).
An invalid Kerberos ticket (SPNEGO) is not logged at all: browsers send
unusable Negotiate tokens in normal operation (e.g. for a local-password
user), so it would only be noise.

`PasswordAutheticationProvider` throws `UsernameNotFoundException` for an
unknown user and `BadCredentialsException` for a wrong password, and logs
`INVALID_USERNAME` / `INVALID_PASSWORD`: for a SIEM these are different
attacks (user-name enumeration vs. password guessing). The client cannot tell
them apart — `ApiAuthenticationFailureHandler` answers `BAD_CREDENTIALS` for
both and `ProviderManager` treats both alike. A user without the password
authentication type (e.g. an AD user using the login form) is logged as
`INVALID_USERNAME` too.

## Stateless authentication (JWT, API key)

A JWT (`oauth2ResourceServer`) and an API key (`STATELESS` chain) are
authenticated on every request, so logging each success would turn every
request of an integration into a `user_login` event and bury the interactive
logins. `SiemAuditLogger` therefore logs a success at most once per hour for
the same credential and client address (`firstStatelessSuccess`):

- the credential is the `keyId` for an API key and the user name for a JWT —
  a JWT is refreshed regularly, so the token itself would log every refresh;
- the address is part of the key, so use of a credential from a new address
  (e.g. a leaked API key) is logged at once;
- failures are always logged;
- the memory is a Guava cache per node (at most 10 000 entries); after a
  restart or on another node the first success is logged again, which is
  harmless. The interval is a constant, not a setting.

Password, AD, Kerberos and SSO-header logins create a session and run once,
so they are not throttled.

A JWT that cannot be decoded is logged as `failed` with
`INVALID_TOKEN` (signature, expiry, format — `BadJwtException`) or
`TOKEN_DECODING_FAILED` (other `JwtException`, e.g. the JWKS of the identity
provider is unreachable), with the exception message in `details` and no
user (the token was not trusted).

## Logout and password change

- **`user_logout`** is written by `ApiLogoutSuccessHandler` when a logged-in
  user calls `logout`. Expiry of a session is not a logout and is not logged.
- **`password_change`** is written in `UserService`, so every path that writes
  a password is covered: `changePasswordPrivate` (own change and the
  administrator's reset) and `changeUser` with a password in `valuesMap`.
  `srcuser` is the logged user (`auth.getName()`, not `getLoggedUserDetail()`,
  which fails for non-standard details), `dstuser` the user whose password
  changed. A wrong old password in the own change is logged as `failed` with
  `OLD_PASSWORD_MISMATCH`; a password-policy violation is an input error, not
  a security event, and is not logged. A password from
  `elza.security.override` is not stored, so it is no `password_change`; see
  "Users from the configuration".

## User lifecycle

- **`user_create`** is written in `UserService.createUser`, which serves the
  REST API, SOAP (`UserServiceImpl`) and the automatic creation of a user on
  the first JWT login (`JwtUserDetailProvider`). The latter runs under
  `createSecurityContextSystem()`, whose authentication has no principal, so
  `loggedUsername()` maps the empty name to `system`. The first administrator
  of the initial setup dialog (`SetupService.createAdmin`, an anonymous
  request) is logged with `srcuser` = `setup`; a user from
  `elza.security.override` by `configAdminApplied` (`srcuser` =
  `configuration`).
- **`user_activate` / `user_disable`** are written in
  `UserService.changeActive` (REST and SOAP) only when the state really
  changes, so a repeated request (e.g. SOAP `setUserState` with the current
  state) adds nothing.
- **`user_rename`** is written in `UserService.changeUser`, the only place
  that changes `usr_user.username`. `dstuser` is the new name - the one all
  later events carry - and `old_username` links the history before the
  rename. It is written after the user is saved, so a validation error of
  the authentications in the same request leaves no rename in the log; the
  `password_change` that a rename of a password user requires therefore
  comes first, already with the new name.

## Users from the configuration

`elza.security.override` (see [password-policy.md](password-policy.md))
gives listed users a plain-text password valid next to their own. Since it
is a second valid password, its existence and every use are logged:

- **`override_active`** (`log_type` `secure`, `srcuser` = `configuration`,
  `dstuser` = the user) at every startup for each listed user, so a SIEM
  can alert on an entry left in the configuration.
- **`user_login`** with `authentication_type` = `OVERRIDE` when the
  configured password matched. When the user's own password matches, the
  login is `PASSWORD` even for a listed user. A wrong password is
  `INVALID_PASSWORD` with `PASSWORD` - nothing tells which of the two
  passwords was meant.
- `user_create` / `user_activate` (`configAdminApplied`) and
  `config_admin` (`configAdminFailed`) for the changes made at startup.

## Permissions and groups

The format is ELZA's own minimal choice; it was not agreed with a SIEM
operator, and extensions are discussed with the customer when needed.

- **`permission_grant` / `permission_revoke` / `permission_change`** are
  written in the private `UserService.changePermission(user, group, …)`
  after the changes are saved. Every change of permissions goes through it
  (REST and SOAP add/delete/synchronize, deletion by fund or scope,
  `copyPermissions`, the JWT synchronization `JwtUserDetailProvider` →
  `changeUserPermission`, the fund-admin permissions of the author of a new
  fund `addFundAdminPermissions`), and its `ChangedPermissionResult` holds
  exactly the added, deleted and updated rows. So one place covers all paths
  and only real changes are logged — important for JWT, whose
  synchronization runs every time the user detail cache expires.
- The holder is `dstuser` for a user permission and `dstgroup` (group code)
  for a group permission. `permission` is the enum name, `permission_target`
  is `type:id` by `Permission.getType()` (ids only, no names), none for
  `ALL`.
- `permission_change` is an edit of an existing row (`updatePermission`
  changes it in place), so only the new value is known and logged.
- The fund-admin permissions of a new fund are logged too (one event per
  permission of `FUND_ADMIN_PERMISSIONS`): fund creation is rare and it is a
  real grant.
- **Not logged:** permissions deleted together with their entity
  (`deletePermissionsByFund`, `deletePermissionByNodeIds`,
  `deletePermissionsByIssueList`) — a consequence of deleting the entity, not
  a decision about access.
- **`group_member_add`** is written in `joinGroup(user, group)`, used by
  `joinGroup` of the API and by `copyPermissions`; **`group_member_remove`**
  in `leaveGroup`. **`group_create` / `group_delete`** in `createGroup` /
  `deleteGroup`; deleting a group removes the permissions of all its members,
  which is not logged per member.

## Funds

- **`fund_create`** is written in `ArrangementService.createFund(name,
  internalCode, institution, …, imported)`, the one method that saves a new
  `ArrFund`: creation from the UI and SOAP (`createFundWithScenario`) passes
  `imported = false`, the XML import (`SectionsContext`, also the batch
  import) `true`, logged as `fund_import`.
- **`fund_delete` / `fund_history_delete`** are written in
  `ArrangementService.deleteFund` / `deleteFundHistory` (REST and SOAP). The
  name of the fund is read before `DeleteFundAction` /
  `DeleteFundHistoryAction` runs.
- These events are written **after the transaction commits**
  (`SiemAuditLogger.afterCommit`): an import often fails after the fund row
  was saved (an error further in the file) and rolls back, and deleting a
  large fund can fail half-way; an event written before the commit would
  report a fund that does not exist or still exists. Without a transaction
  the write runs at once. `srcuser` and `srcip` are taken when the event is
  built and when it is written, both on the thread of the request.

## Open work

- **Events other than those of funds are written before the transaction
  commits.** A change event (e.g. `password_change`) stays in the log even if
  the transaction rolls back later. Nothing is expected to fail after the
  write, so it is accepted; `SiemAuditLogger.afterCommit`, used for funds,
  would remove the gap (unit tests then need an active transaction
  synchronization).
