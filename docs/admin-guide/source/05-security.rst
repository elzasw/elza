===========================
Authentication and Security
===========================

Authentication methods
======================

ELZA supports these methods of authentication:

- **Internal accounts** - users and passwords stored in ELZA (always
  available).
- **Active Directory (LDAP)** - the user's name and password are verified
  against an Active Directory domain.
- **Kerberos (SPNEGO)** - single sign-on in a Windows domain, and
  optionally verification of the name and password against the KDC.
- **SSO header** - a reverse proxy that has authenticated the user passes
  the user name in an HTTP header.
- **OAuth2 / JWT** - requests carry a bearer token issued by an identity
  provider.
- **Personal API keys** - for integrations calling the REST API.

The methods can be combined. When a user logs in with a name and a
password (the login form or HTTP Basic), they are tried in this order:
Active Directory, Kerberos, internal password. The first method that
accepts the password wins.

Except for OAuth2, every method only verifies the identity: **the user
must already exist in ELZA** with the same user name, and must be active.
Users are not created automatically, and groups or roles from the
directory are not used; permissions are always assigned in ELZA.

The first administrator
=======================

While the database contains no user, the application offers the
*Initial setup*, which creates the first administrator; see
:ref:`install-first-admin`. Its endpoints (``/api/v1/setup``) are
available without login and create a user only while no user exists.
Until then, anyone who can reach the application can create the first
administrator, so create it before the application is made available on
the network. Once any user exists, the setup is closed for good.

Users can also be set in the configuration, in the list
``elza.security.override``; see :ref:`install-users-from-config`. It is
applied at every startup: a missing user is created as an administrator
without a password in the database, a deactivated user is activated, and
the password of an existing user in the database does not change. A
listed user logs in with their own password or with the configured one.
The configured password is in plain text and valid next to the user's
own; remove the entry once it is no longer needed.

The default user
================

A new installation contains a built-in user ``admin`` with the password
``admin`` and the administrator permission. It exists only in the
configuration, not in the database, and it is available while no user of
the same name exists in the database.

.. warning::

   Disable the default user as soon as the administrator accounts exist:

   .. code-block:: yaml

      elza:
        security:
          allowDefaultUser: false

   The default user is recognised by its name alone. While it is enabled,
   the SSO header, Kerberos, Active Directory and OAuth2 methods accept
   the identity ``admin`` without a password and grant it administrator
   rights. Always disable the default user when any of these methods is
   used. Also avoid creating a database user named ``admin`` while the
   default user is enabled: its own password and permissions are then
   ignored.

.. list-table::
   :header-rows: 1
   :widths: 36 18 46

   * - Key
     - Default
     - Meaning
   * - ``elza.security.allowDefaultUser``
     - ``true``
     - Enables the default user.
   * - ``elza.security.defaultUsername``
     - ``admin``
     - Name of the default user.
   * - ``elza.security.defaultPassword``
     - hash of ``admin``
     - Password of the default user as an encoded hash (``{bcrypt}...``),
       not as plain text.

Internal accounts and passwords
===============================

Users are created in *Administration* > *Users*. Passwords are stored as
bcrypt hashes. Passwords from old versions stored with SHA-256 are
converted to bcrypt at the user's next login with the password.

When a deactivated user logs in with the correct password, the login form
says that the account is deactivated. With a wrong password, the form
shows the same message as for an unknown user ("Invalid user name or
password"), so it does not reveal whether an account exists or is
deactivated.

``elza.security.salt`` is used only to verify these old SHA-256 hashes.
Do not change it: users whose password has not been converted yet could
no longer log in, and an administrator would have to reset their
passwords.

Users who still have a password in the old format (they have not logged
in with the password since the upgrade) are listed by:

.. code-block:: sql

   SELECT u.username FROM usr_user u
   JOIN usr_authentication a ON a.user_id = u.user_id
   WHERE a.auth_type = 'PASSWORD' AND a.auth_value NOT LIKE '{%';

Password rules
--------------

The rules for internal passwords are set in *Administration* > *Users* >
*Password rules*; the button requires the permission *User and permission
management* (or the administrator permission). The rules apply to the
whole instance:

*Password validity (days)*
   How long a password is valid after it was set.
*Minimum password length*
   The minimum number of characters.
*Minimum number of character groups*
   From how many of the four groups - lowercase letters, uppercase
   letters, digits, other characters - the password must contain at least
   one character (1-4).

An empty value or 0 turns the rule off; a new installation has all rules
off. An empty password is never accepted.

The length and character groups are checked whenever a password is set:
when a user changes their own password, when an administrator sets a
password of a user, and when a user is created or edited with a new
password. Administrators are not exempt; a weak temporary password is not
accepted either. Existing passwords are not checked until they are
changed.

The validity is counted from the moment the password was last set. The
conversion of an old SHA-256 hash at login does not change the password
and does not restart the period.

.. warning::

   Passwords set before the upgrade to the version with password rules
   count as set on 1 January 2016. Turning the validity on therefore makes
   every password not changed since the upgrade expired at once: each such
   user has to change the password at the next login.

Expired and required password change
------------------------------------

When an administrator sets a password of a user (*Administration* >
*Users* > *Change password*), two options are available:

*Require a password change at next login*
   Typical for a temporary password handed over to the user.
*Password never expires*
   Exempts the account from the validity, for example a technical
   account.

After a login with an internal password that has expired or whose change
is required, the login succeeds and the application opens a dialog that
cannot be closed until the user sets a new password. The new password
must meet the rules. After the change, the requirement is cleared and the
validity period starts again.

The password change is enforced by the web client only. Requests to the
REST API made in such a session are not blocked.

The validity and the required change apply only to logins with the
internal password. Users logged in through Active Directory, Kerberos,
the SSO header, OAuth2 or an API key are never asked to change their
password; with Active Directory or Kerberos verifying passwords, the
internal password is used only when the domain rejects the password. The
default user is never asked either: its password is set in the
configuration only (``elza.security.defaultPassword``) and cannot be
changed in the application.

Recovering access to an account
-------------------------------

When an administrator has forgotten their password and no other
administrator can set a new one, access is recovered with the users from
the configuration (see :ref:`install-users-from-config`):

1. Add the user with a temporary password to ``elza.security.override``
   in :file:`elza.yaml` and restart ELZA. A deactivated user is activated;
   the user's own password in the database does not change.
2. Log in with the temporary password and change the password in the
   application.
3. Remove the entry from :file:`elza.yaml` and restart ELZA. Until then the
   temporary password works next to the new one.

The security audit log records that the temporary password is active
(``override_active``) and every login with it (``authentication_type``
``OVERRIDE``). The default user is not recovered this way; its password
is set in the configuration (``elza.security.defaultPassword``).

Active Directory
================

.. code-block:: yaml

   elza:
     security:
       ldap:
         ad-domain: archive.example
         ad-server: ldap://dc1.archive.example/

``ad-domain``
   The Active Directory domain. Setting it enables the method.
``ad-server``
   URL of a domain controller.

The user logs in with the domain user name and password. After the
domain has accepted the password, ELZA looks up the user by the name as
entered; create the ELZA users with the same names as in the domain. When
the domain rejects the password, the internal password is tried.

With Active Directory configured, the health check (see
:doc:`09-monitoring`) also checks the connection to the domain controller.

Kerberos (SPNEGO)
=================

.. code-block:: yaml

   elza:
     security:
       kerberos:
         service-principal: HTTP/elza.archive.example@ARCHIVE.EXAMPLE
         keytab-location: /etc/elza/elza.keytab
         authenticate-with-password: true

``service-principal``
   The service principal name of ELZA. Setting it enables the method.
``keytab-location``
   Path to the keytab file with the key of the service principal. The
   file must be readable by the service user only.
``authenticate-with-password``
   Also verify names and passwords from the login form against the KDC
   (default ``true``).
``kerberos-client-debug``, ``ticket-validator-debug``
   Debug output of the Kerberos client and of the ticket validation
   (default ``false``).

The Kerberos configuration of the JVM (the realm and the KDC,
:file:`krb5.conf`) is taken from the operating system or from the Java
settings.

With Kerberos configured, the login dialog offers single sign-on. The
browser then authenticates with the user's domain ticket; the browser
must be allowed to use integrated authentication for the ELZA address.
The realm is removed from the principal and the user is looked up by the
rest of the name (``jan.novak@ARCHIVE.EXAMPLE`` becomes ``jan.novak``).
The realm itself is not checked. When single sign-on fails, the user can
log in with the form.

SSO header
==========

A reverse proxy or an access gateway that authenticates users can pass
the user name in an HTTP header:

.. code-block:: yaml

   elza:
     security:
       allowDefaultUser: false
       sso-header:
         user-header: X-SSO-User

ELZA looks up the user by the value of the header. When the header names
another user than the current session, the session is replaced.

.. warning::

   ELZA trusts the header completely. It is secure only when:

   - ELZA is reachable exclusively through the proxy (bind it to
     ``127.0.0.1`` or restrict the port by a firewall),
   - the proxy removes or overwrites the header on every request, so a
     client cannot send it,
   - the default user is disabled.

Set ``elza.security.logoutUrl`` to the logout address of the gateway, so
that logging out of ELZA also ends the session at the gateway.

OAuth2 / JWT
============

ELZA can accept bearer tokens (``Authorization: Bearer ...``) issued by
an identity provider. It acts only as a resource server; there is no
login redirect to the provider.

.. code-block:: yaml

   elza:
     security:
       o-auth2:
         key-url: https://idp.archive.example/oauth/token_key
         permissions:
           - authority: ELZA_ADMIN
             permissions: [ADMIN]

``key-url``
   URL returning the public key for verifying token signatures, as JSON
   with the PEM-encoded RSA key in the field ``value``. The key is read
   once at startup; the application does not start when it cannot be
   read. Tokens must be signed with RS256.
``permissions``
   Mapping of values of the token's ``authorities`` claim to ELZA
   permissions. ``scope`` restricts a permission to a scope of archival
   entities, by its code.
``user-scope``, ``user-ap-type``
   Scope and class of the archival entities of users created from tokens;
   default ``JWT_USERS`` and ``PERSON_INDIVIDUAL``. The class must be
   offered by the rule set of the scope.

The token's ``sub`` claim is the user name and ``name`` the display name.
Unlike the other methods, a user who does not exist is created, together
with an archival entity of the person in the scope ``user-scope``. The
user's directly assigned permissions are replaced by those mapped from
the token. User data are cached for five minutes, so a change of
permissions or deactivation takes effect within five minutes.

Personal API keys
=================

Integrations call the REST API with a personal API key instead of a
user's password. A key belongs to a user and acts with that user's
permissions; create a dedicated user for each integration.

Users create and revoke their own keys in their user settings (category
*Elza*). The full
key is shown only once, when it is created; ELZA stores only its hash.
Administrators can list and revoke the keys of other users. Keys can be
created and revoked only after an interactive login, not with another
API key. A request authenticated by an API key cannot change passwords
or other login credentials of any user either; it is rejected with 403.

The key is sent in a header:

.. code-block:: bash

   curl -H "X-API-Key: elza_..." https://elza.archive.example/api/v1/...

.. list-table::
   :header-rows: 1
   :widths: 44 12 44

   * - Key
     - Default
     - Meaning
   * - ``elza.security.api-keys.enabled``
     - ``true``
     - Accept API keys. With ``false`` the header is ignored.
   * - ``elza.security.api-keys.header-name``
     - ``X-API-Key``
     - Name of the header.
   * - ``elza.security.api-keys.default-validity-days``
     - 365
     - Validity of a new key when the user does not choose one.
   * - ``elza.security.api-keys.max-validity-days``
     - 730
     - Maximum validity of a key.

A rejected key is answered with HTTP 401 and a JSON body with the reason
(``MALFORMED_TOKEN``, ``UNKNOWN_KEY``, ``INVALID_SECRET``, ``EXPIRED``,
``REVOKED``, ``USER_INACTIVE``).

Security audit log
==================

With ``elza.siemLogFile`` set (see :doc:`04-configuration`), ELZA writes
authentication, account and fund events to a separate log in JSON, one event per line,
suitable for a SIEM system. Field names follow the OSSEC/Wazuh schema, so
the built-in rules of such a SIEM can use them. Every event contains:

.. list-table::
   :header-rows: 1
   :widths: 25 75

   * - Field
     - Meaning
   * - ``timestamp``
     - Time of the event (ISO 8601).
   * - ``program_name``
     - Always ``elza``.
   * - ``log_type``
     - ``secure`` for authentication, ``audit`` for changes of users and
       their access.
   * - ``event_type``, ``action``
     - Kind of the event and the operation, see the table below.
   * - ``result``
     - ``success`` or ``failed``.
   * - ``srcuser``
     - Who performed the action: the logged-in user, or ``configuration``
       for changes made from ``elza.security.override``, ``setup`` for the
       first administrator created in the initial setup dialog, ``system``
       for a user created automatically on the first JWT login.
   * - ``dstuser``
     - Whom the action concerns; the same as ``srcuser`` for a login.
   * - ``srcip``
     - Source address of the request; missing for events outside a
       request (startup).
   * - ``reason``
     - Reason of the failure (only with ``result`` = ``failed``).
   * - ``msg``
     - Short description in English, e.g. ``User martin logged in from IP
       192.168.1.10.``
   * - ``details``
     - Optional technical description of a failure in English, e.g. the
       reason a JWT was rejected.
   * - ``dstgroup``
     - Code of the group the action concerns (permissions and groups).
   * - ``permission``, ``permission_target``
     - Changed permission (e.g. ``FUND_ARR``) and the entity it applies
       to as ``type:id`` (``fund:123``, ``node:456``, ``scope:5``,
       ``user:7``, ``group:3``, ``issue_list:9``); no target for a global
       permission.

Events:

.. list-table::
   :header-rows: 1
   :widths: 22 22 56

   * - ``event_type``
     - ``action``
     - Description
   * - ``user_login``
     - ``login``
     - Login of a user, successful or failed. ``authentication_type`` holds
       the method (``PASSWORD``, ``ACTIVE_DIRECTORY``, ``KERBEROS``,
       ``SSO_HEADER``, ``JWT``, ``API_KEY``, ``OVERRIDE`` for the password
       from ``elza.security.override``), ``key_id`` the public id of an
       API key; both are present also for a failure. ``reason`` of a
       failure: ``INVALID_USERNAME`` (no such user in ELZA),
       ``INVALID_PASSWORD``, ``INACTIVE_USER``, ``INVALID_CREDENTIALS``
       (SSO header), ``INVALID_TOKEN`` / ``TOKEN_DECODING_FAILED`` (JWT),
       the API-key codes listed above, or ``AUTHENTICATION_ERROR`` for an
       unexpected error with its description in ``details``. A failed
       API-key or JWT login has no ``srcuser``.
       A JWT and an API key are checked on every request, so their
       successful login is logged at most once per hour for the same key
       (or user) and address; failures are always logged.
   * - ``user_logout``
     - ``logout``
     - Logout of a user. Expiry of a session is not logged.
   * - ``api_key_create``
     - ``create_api_key``
     - An API key was created; ``key_id`` and ``expire_date``.
   * - ``api_key_revoke``
     - ``revoke_api_key``
     - An API key was revoked (by its owner or an administrator); ``key_id``.
   * - ``user_create``
     - ``create_user``
     - A user was created by an administrator (UI, REST, SOAP), in the
       initial setup, automatically on the first JWT login, or at startup
       from ``elza.security.override``.
   * - ``password_change``
     - ``change_password``
     - A password was changed by the user (``srcuser`` = ``dstuser``) or by
       an administrator. A change by the user fails with ``reason`` =
       ``OLD_PASSWORD_MISMATCH`` when the old password does not match.
   * - ``user_activate``
     - ``activate_user``
     - An account was activated by an administrator or at startup from
       ``elza.security.override``. Logged only when the state changes.
   * - ``user_rename``
     - ``rename_user``
     - A user was renamed by an administrator; ``dstuser`` is the new name,
       ``old_username`` the previous one.
   * - ``override_active``
     - ``enable_override``
     - Written at every startup for each user in
       ``elza.security.override``: the configured password is valid for
       the user (``dstuser``) next to their own.
   * - ``user_disable``
     - ``disable_user``
     - An account was deactivated by an administrator. Logged only when
       the state changes.
   * - ``permission_grant``, ``permission_revoke``, ``permission_change``
     - ``grant_permission``, ``revoke_permission``, ``change_permission``
     - A permission of a user (``dstuser``) or of a group (``dstgroup``)
       was granted, revoked or edited, by an administrator, through SOAP,
       when copying permissions, to the author of a new fund, or from the
       token of the identity provider (JWT, ``srcuser`` = ``system``; only
       real changes are logged).
   * - ``group_member_add``, ``group_member_remove``
     - ``add_group_member``, ``remove_group_member``
     - A user was added to or removed from a group.
   * - ``group_create``, ``group_delete``
     - ``create_group``, ``delete_group``
     - A group was created or deleted. Deleting a group takes its
       permissions from all its members; this is not logged per member.
   * - ``fund_create``
     - ``create_fund``
     - An archival fund was created, by hand or by an import
       (``fund_import`` = ``true``); ``fund_id`` and ``fund_name``.
   * - ``fund_delete``
     - ``delete_fund``
     - An archival fund was deleted with all its data; ``fund_id`` and
       ``fund_name``.
   * - ``fund_history_delete``
     - ``delete_fund_history``
     - The history of an archival fund (old versions and changes) was
       deleted; ``fund_id`` and ``fund_name``.
   * - ``config_admin``
     - ``apply_config_admin``
     - A user from ``elza.security.override`` could not be applied (always
       ``failed``).

The log is rotated daily and kept for 90 days. Behind a reverse proxy,
``srcip`` is the address of the client only with
``elza.security.acceptForwardedHeaders`` enabled and the proxy set up as
described in :ref:`reverse-proxy-client-address`; otherwise it is the
address of the proxy.

Other settings
==============

.. list-table::
   :header-rows: 1
   :widths: 40 12 48

   * - Key
     - Default
     - Meaning
   * - ``elza.security.acceptForwardedHeaders``
     - ``false``
     - Accept ``X-Forwarded-*`` headers from a reverse proxy; required
       when ELZA runs under a path and for the address of the client in
       the security audit log (see :doc:`06-reverse-proxy`).
   * - ``elza.security.logoutUrl``
     - (none)
     - Address the browser opens after logging out, for example the
       logout page of an SSO gateway.
   * - ``elza.security.displayUserInfo``
     - ``true``
     - Show the user menu in the application header.

Sessions expire after the standard timeout of the embedded server (30
minutes of inactivity), which can be changed with
``server.servlet.session.timeout``. One user can have at most ten
sessions at a time; logging in an eleventh time ends the oldest one.

The monitoring endpoints do not require authentication and must stay
reachable only locally (see :doc:`09-monitoring`).
