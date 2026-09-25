=========
Upgrading
=========

Before every upgrade, read the :doc:`release-notes` of all versions
between the installed and the new one. Entries marked **upgrade** or
**config** may require an action before or after the upgrade.

The database schema and the data are migrated automatically at the first
start of the new version. A migration cannot be undone: returning to the
previous version requires restoring the backup made before the upgrade.

Upgrading within the 3.x line
=============================

With the install scripts
------------------------

``elza-deploy`` performs all the steps below, including the database
backup before the upgrade. ``elza-deploy --auto`` installs the newest
version of the configured channel, and the ``elza-update`` timer can run
upgrades unattended outside working hours. See
https://get.lightcomp.com/elza/.

The script backs up the database only; back up the :file:`dms` directory
as well (see :doc:`backup`).

Manually
--------

#. Back up the database and the :file:`dms` directory of the working
   directory from the same moment, ideally with the application stopped.
   See :doc:`backup`.
#. Stop the application.
#. Replace the JAR file (on Linux, point the symbolic link to the new
   file).
#. Replace the packages in the :file:`dpkg/` directory of the working
   directory with those of the new distribution (see
   :ref:`install-packages`). A new version of ELZA may require new
   versions of the packages, so upgrade them together.
#. Apply the configuration changes listed in the release notes.
#. Start the application and watch the log until the application has
   started. The first start after an upgrade can take considerably longer
   because of data migrations.

Upgrading from 2.x
==================

An installation of version 2.x can be upgraded directly to the current
version. Before the upgrade, check that:

- Java 17 is installed,
- the database server meets the :doc:`requirements <overview>`.

Changes in the configuration:

- Remove ``spring.jpa.properties.hibernate.dialect`` from
  :file:`elza.yaml`; the dialect is detected automatically.
- Since 3.0, packages are loaded at startup from the :file:`dpkg/`
  directory and are upgraded together with the application.

After the first start:

#. Check in *Administration* > *Packages* that the ZP2015 rules package
   has a version higher than 300.
#. Rebuild the search index with *Rebuild indexes* in the administration.

Older 2.x installations may need data fixes before the upgrade; see
:doc:`legacy-upgrades`.

Upgrading from 1.x and 0.x
==========================

Versions 1.x ran as a WAR file in a separate Tomcat. Install the current
version as described in :doc:`installation`, reuse the database and the
working directory, and move the old configuration file
:file:`elza-ui.yaml` to :file:`config/elza.yaml`, removing the settings
listed above for 2.x. Upgrading from versions older than 0.17.1 is not
supported; upgrade to 0.17.1 first.
