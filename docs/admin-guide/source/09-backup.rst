==================
Backup and Restore
==================

What to back up
===============

ELZA keeps its data in two places that form one whole:

- the **database**,
- the **dms directory** of the working directory
  (:file:`<workingDir>/dms`) with attachments, generated outputs,
  publications and import batch files. The database refers to each file
  by its path in this directory.

Both must be backed up and restored together, from the same moment. A
database restored without the matching :file:`dms` directory refers to
missing files; files without a database record are treated as orphans.

The rest of the working directory does not need to be backed up: packages
are part of the distribution and other data are recreated by the
application. Back up the configuration (:file:`config/elza.yaml`) with
the other server configuration.

The whole virtual machine can also be backed up, provided the snapshot
of the database and the file system is consistent.

Database
========

The tables ``ap_cached_access_point`` and ``arr_cached_node`` are caches
rebuilt automatically at startup; their content can be excluded from the
backup (their definition cannot). Example:

.. code-block:: bash

   pg_dump -Fc --exclude-table-data=ap_cached_access_point \
               --exclude-table-data=arr_cached_node \
               -U elza -f elza.dump elza

The size of a backup of a medium-sized installation is in the order of
one to several gigabytes. With the install scripts, ``elza-backup`` and
its timer back up the database regularly (see
https://get.lightcomp.com/elza/).

The dms directory
=================

Back up the directory after the database, or back up both with the
application stopped. A file created between the two backups is then only
an orphan, which is harmless. For example:

.. code-block:: bash

   rsync -a --delete /opt/elza/work/dms/ /backup/elza/dms/

Deleted files are first moved to :file:`dms/_trash/<date>/` and removed
after ``elza.dms.trashRetentionDays`` (default 30 days); the trash does
not have to be backed up.

Schedule and retention
======================

The frequency depends on the organisation and the amount of work done in
the application. A typical schedule:

========================= ==================================
Item                      Frequency
========================= ==================================
Full database backup      daily or weekly, for example 2:00
dms directory             after the database backup
Before every upgrade      always, both
========================= ==================================

Keep at least the last eight weekly backups, and selected backups for a
longer time, for example yearly backups for ten years. ELZA holds the
archival description long-term, so an error discovered late must still
be recoverable.

Restore
=======

#. Stop the application.
#. Create an empty database with the same owner and locale as described
   in :ref:`install-database`, but without creating the PostGIS
   extension; the backup contains it. Restore as a superuser, because
   creating the extension requires it:

   .. code-block:: bash

      pg_restore -U postgres -d elza elza.dump

#. Restore the :file:`dms` directory from the same moment.
#. Start the version of ELZA the backup was made with. A newer version
   migrates the data at startup; an older version cannot read them.
#. Wait until the application has started; the caches are rebuilt at
   startup.
