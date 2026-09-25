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
(:file:`elza.yaml`) is described in :doc:`configuration`.

Manual installation
===================

Contents of the distribution
----------------------------

The binary distribution is a ZIP file containing:

- :file:`server/elza-tomcat-<version>.jar` - the application with the
  embedded web server,
- :file:`server/config/elza.yaml` - a sample configuration,
- :file:`server/config/csc-metrics.json` - the list of metrics reported
  to a supervision service (see :doc:`monitoring`),
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
  :doc:`backup`).
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
- the working directory (``elza.workingDir``).

.. code-block:: yaml

   elza:
     data:
       url: jdbc:postgresql://localhost/elza
       user: elza
       pass: secret
     workingDir: /opt/elza/work
     logFile: ${elza.workingDir}/log/elza.log
     siemLogFile: ${elza.workingDir}/log/siem.log

All other settings are described in :doc:`configuration`.

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
``systemctl enable --now elza``.

On Windows, ELZA can be run as a service with a service wrapper such as
WinSW or NSSM, running the same ``java -jar`` command. See also the
`Spring Boot deployment documentation
<https://docs.spring.io/spring-boot/how-to/deployment/installing.html>`_.

First start
===========

Default user
------------

A new installation can be accessed with the built-in default user
(``admin`` with the password ``admin``), which has administrator rights.
Use it only to create the administrator accounts, then disable it with
``elza.security.allowDefaultUser: false`` and restart the application.
See :doc:`security`.

.. _install-packages:

Loading the packages
--------------------

ELZA needs the code lists and description rules from the packages in
:file:`packages/`. The recommended way is to copy the ZIP files into the
:file:`dpkg/` subdirectory of the working directory: at every start, ELZA
loads each package from this directory whose version is newer than the
imported one. An upgrade then only replaces the files in :file:`dpkg/`
together with the JAR.

.. note::

   If :file:`dpkg/` contains an older version of a package than the one
   already imported, the application does not start. Remove the old file.

Packages can also be imported in *Administration* > *Packages*.

Importing institutions
----------------------

Import the list of archival institutions from
:file:`data/all-institutions-import.xml` with *Archival entity import*
in the *Archival entities* module.

Next steps
----------

- Set up the reverse proxy and HTTPS: :doc:`reverse-proxy`.
- Set up backups: :doc:`backup`.
- Connect the monitoring: :doc:`monitoring`.
- Configure authentication (LDAP, Kerberos, ...): :doc:`security`.
