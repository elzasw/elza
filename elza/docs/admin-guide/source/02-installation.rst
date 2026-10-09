============
Installation
============

There are two ways to install ELZA:

- **Install scripts (recommended for Linux)** - a set of scripts that
  installs ELZA as a systemd service and automates deployment of new
  versions, database backups, health checks and unattended updates.
- **Manual installation** from the binary distribution, on Linux or
  Windows.

Both use the same binary distribution and the same configuration file.

.. _install-scripts:

Installation with the install scripts
=====================================

The scripts and their documentation are published at
https://get.lightcomp.com/elza/. They require Linux with systemd,
PostgreSQL including its client tools (``pg_dump``), ``bash`` and
``curl``. The installation is started with:

.. code-block:: bash

   curl -fsSL https://get.lightcomp.com/elza/elza-bootstrap.sh | bash -s -- --home /opt/elza

The bootstrap script downloads the script bundle (verified by a checksum)
into :file:`/opt/elza/scripts`, prepares the configuration file
:file:`elza-env` and installs the systemd units. After editing
:file:`elza-env` (at least the database connection), the first deployment
is run with ``elza-deploy``.

The scripts provide these commands:

- ``elza-deploy`` - installs or upgrades the application: stops the
  service, backs up the database, downloads the distribution, switches
  atomically to the new version, starts the service and checks that it
  runs.
- ``elza-backup`` - backs up the database with ``pg_dump`` and removes
  old backups.
- ``elza-health`` - checks the state of the application and optionally
  reports it to a supervision service (see :ref:`monitoring-reporting`).
- ``elza-update`` - unattended upgrade to a new version.

The database still has to be prepared as described in
:ref:`install-database`. The configuration of ELZA itself
(:file:`elza.yaml`) is described in :doc:`04-configuration`.

Manual installation
===================

Contents of the distribution
----------------------------

The binary distribution is a ZIP file containing:

- :file:`server/elza-tomcat-<version>.jar` - the application with the
  embedded web server,
- :file:`server/config/elza.yaml` - a sample configuration,
- :file:`server/config/csc-metrics.json` - the list of metrics reported
  to a supervision service (see :doc:`08-monitoring`),
- :file:`packages/` - the base packages: ``package-cz-base`` (code
  lists) and ``rules-cz-zp2015`` (description rules ZP2015),
- :file:`data/all-institutions-import.xml` - the list of accredited
  archival institutions in the Czech Republic,
- :file:`readme.txt` - a short installation overview.

.. _install-database:

Preparing the database
----------------------

#. Create a database user for ELZA, for example ``elza``. Do not use a
   superuser account for the application.

#. Create an empty database owned by this user:

   .. code-block:: bash

      createdb -O elza -E UTF8 -l cs_CZ.UTF-8 -T template0 elza

   The locale determines the sorting of texts done by the database. Use
   the locale of the archival description (Czech for Czech archives);
   available locales are listed by ``locale -a``. On Windows the name is,
   for example, ``Czech_Czechia.1250``. The locale cannot be changed after
   the database has been created.

#. Enable the PostGIS extension in the database. Creating an extension
   requires superuser rights, so run it as the ``postgres`` user:

   .. code-block:: bash

      psql -U postgres -d elza -c "CREATE EXTENSION postgis;"

The tables are created by ELZA at its first start.

Directory layout
----------------

ELZA needs an installation directory and a working directory, for
example:

- :file:`/opt/elza/server` - the JAR file and :file:`config/elza.yaml`,
- :file:`/opt/elza/work` - the working directory.

On Windows, for example :file:`D:\\Elza\\server` and
:file:`D:\\Elza\\work`.

Create a dedicated operating system user for the service, owning the
working directory with full access to it. On Linux, keep a symbolic link
to the current JAR, so that an upgrade only changes the link:
:file:`/opt/elza/server/elza-tomcat.jar` ->
:file:`elza-tomcat-3.4.7.jar`.

The working directory contains:

- :file:`dms/` - binary files (attachments, outputs, publications,
  import batches). Its structure is managed by ELZA and must not be
  modified by hand; it is backed up together with the database (see
  :doc:`09-backup`).
- :file:`dpkg/` - packages loaded at startup (see :ref:`install-packages`).
- :file:`log/` - log files, when configured as in the sample
  configuration.
- other working data, which ELZA recreates when missing.

Configuration
-------------

Copy :file:`server/config/elza.yaml` to :file:`config/elza.yaml` next to
the JAR file and set at least:

- the database connection (``elza.data.url``, ``elza.data.user``,
  ``elza.data.pass``),
- the working directory (``elza.workingDir``),
- the packages the installation uses (``elza.packages.enabled``, see
  :ref:`install-packages`).

.. code-block:: yaml

   elza:
     data:
       url: jdbc:postgresql://localhost/elza
       user: elza
       pass: secret
     workingDir: /opt/elza/work
     logFile: ${elza.workingDir}/log/elza.log
     siemLogFile: ${elza.workingDir}/log/siem.log
     packages:
       enabled: [CZ_BASE, ZP2015]

All other settings are described in :doc:`04-configuration`.

Running as a service
--------------------

Before setting up the service, start the application once from the
installation directory to check the configuration:

.. code-block:: bash

   java -jar elza-tomcat.jar

At the first start, ELZA creates the database schema. The application is
ready when the log reports that it has started; it listens on port 8080.

Example systemd unit (:file:`/etc/systemd/system/elza.service`):

.. code-block:: ini

   [Unit]
   Description=ELZA
   After=network.target postgresql.service

   [Service]
   User=elza
   WorkingDirectory=/opt/elza/server
   ExecStart=/usr/bin/java -Xmx4g -Dfile.encoding=UTF-8 -jar /opt/elza/server/elza-tomcat.jar
   SuccessExitStatus=143
   Restart=on-failure

   [Install]
   WantedBy=multi-user.target

Adjust the heap size (``-Xmx``) to the available memory. The working
directory of the process must be the installation directory, so that
:file:`config/elza.yaml` is found. Enable and start the service with
``systemctl enable --now elza``. With ``Restart=on-failure`` the restart
from the administration works as it is: the application exits with code
3, which the unit restarts on (see :doc:`04-configuration`).

On Windows, ELZA can be run as a service with a service wrapper such as
WinSW or NSSM, running the same ``java -jar`` command. See also the
`Spring Boot deployment documentation
<https://docs.spring.io/spring-boot/how-to/deployment/installing.html>`_.

First start
===========

.. _install-first-admin:

First administrator
-------------------

While the database contains no user, the application shows the
*Initial setup* dialog instead of the login. It creates the first
administrator: a user with a password and the administrator permission.

.. warning::

   Until the first administrator exists, anyone who opens the application
   can create it. Create the administrator right after the first start,
   before the application is made available on the network, or set it in
   the configuration (see :ref:`install-admin-from-config`).

After *Create administrator*, the application logs in as the new
administrator. The password must meet the password rules (see
:doc:`05-security`); after the installation, all rules are off.

The first administrator has no person (archival entity) assigned, because
the archival entities need the packages loaded first. The person can be
assigned later in *Administration* > *Users* > *Edit*. Other users are
created as usual, with a person.

.. _install-admin-from-config:

Administrator from the configuration
------------------------------------

An administrator can also be set in the configuration. When both keys
below are set, ELZA applies them at every startup, whether or not the
database contains users:

- If no user of this name exists, it is created as an administrator: a
  user without a person, with the password and the administrator
  permission. On an empty database, the *Initial setup* dialog is then
  not shown.
- If the user exists, it gets the configured password, and a deactivated
  user is activated. Its permissions do not change; a user without a
  password login gets one. When the user already has this password,
  nothing is written, so the validity of the password does not restart
  at every startup.

.. code-block:: yaml

   elza:
     security:
       admin:
         username: jan.novak
         password: a-password

.. list-table::
   :header-rows: 1
   :widths: 36 18 46

   * - Key
     - Default
     - Meaning
   * - ``elza.security.admin.username``
     - (none)
     - User name of the administrator.
   * - ``elza.security.admin.password``
     - (none)
     - Password of the administrator, as plain text. It must meet the
       password rules.

Every startup with the keys set writes its outcome to the application
log (the password itself is never logged): the user created, the
password set, the account activated, or that the user already matches
the configuration. Only one of the two keys set is logged as a warning
and nothing is applied. If the keys cannot be applied (for example the
password does not meet the rules, or the name is the name of the default
user while the default user is enabled), ELZA logs an error and starts
anyway. With ``elza.siemLogFile`` set, the changes and the failure are
also written to the security audit log (see :doc:`05-security`).

A database user named like the default user gets the password, but it
has no effect while the default user is enabled.

.. warning::

   The password is stored in plain text. While the keys are set, a
   password changed in the application returns to the configured one at
   the next restart, and a deactivated account is activated again. Remove both keys from :file:`elza.yaml` once they are
   no longer needed, for example after the first start.

Default user
------------

A new installation can also be accessed with the built-in default user
(``admin`` with the password ``admin``), which has administrator rights;
the *Sign in* button of the *Initial setup* dialog opens the login form for it.
Use it only to create the administrator accounts, then disable it with
``elza.security.allowDefaultUser: false`` and restart the application.
While the default user is enabled, the first administrator cannot be
named ``admin``. See :doc:`05-security`.

.. _install-packages:

Loading the packages
--------------------

ELZA needs the code lists and description rules from the packages in
:file:`packages/`. The recommended way is to copy the ZIP files into the
:file:`dpkg/` subdirectory of the working directory: at every start, ELZA
loads each package from this directory whose version is newer than the
imported one. An upgrade then only replaces the files in :file:`dpkg/`
together with the JAR.

Which packages of the directory the installation uses is set by
``elza.packages.enabled``, a list of package codes. At every start, ELZA
loads from :file:`dpkg/` the packages that are already imported and the
packages the list names, together with the packages they depend on; the
other files of the directory are left alone. A package that a new version
of ELZA adds to :file:`packages/` is therefore not imported into an
existing installation unless it is listed. The distribution contains:

- ``CZ_BASE`` - entity description according to the Czech CAM, and the
  shared code lists; required by ``ZP2015``,
- ``ZP2015`` - the Czech description rules,
- ``ISAAR_CPF`` - entity description according to the international
  standard ISAAR(CPF), in English; for installations outside the Czech
  practice or alongside ``CZ_BASE``.

Set the key before the first start. On an empty database without the
key, ELZA imports every package of the directory. The log of the start
lists the packages imported and skipped.

.. note::

   If :file:`dpkg/` contains an older version of a package than the one
   already imported, the application does not start. Remove the old file.

*Administration* > *Package management* lists the packages of
:file:`dpkg/` that are not loaded, with what the next start does with
each of them. *Load at the next start* marks a package: the mark is a
package record without content, and the next start imports the file (with
the packages it requires). *Cancel the mark* removes it. The page then
reminds that a restart is needed and offers *Restart the application*:
the application exits with the code ``elza.restart.exitCode`` (3 by
default) and the service manager starts it again; without a service
manager it only stops (see :doc:`04-configuration`).

Packages can also be imported there directly; an imported package is then
upgraded from :file:`dpkg/` like the others, and the search index registers
the fields of its new item types after a restart. A package deleted there
stays deleted at the next start unless the key lists it.

Importing institutions
----------------------

Import the list of archival institutions from
:file:`data/all-institutions-import.xml` with *Archival entity import*
in the *Archival entities* module.

Next steps
----------

- Set up the reverse proxy and HTTPS: :doc:`06-reverse-proxy`.
- Set up backups: :doc:`09-backup`.
- Connect the monitoring: :doc:`08-monitoring`.
- Configure authentication (LDAP, Kerberos, ...): :doc:`05-security`.
