===============
Changelog 3.4.x
===============

The changes of every released build of the 3.4 line, newest first. The
categories and markers are explained in :doc:`release-notes`. The date is
the release date of the build.

Unreleased
==========

- **Changed** - **upgrade**, **config** - Storage of binary files (#10028).
  Attachments, generated outputs, publication exports and import batch
  files in :file:`<workingDir>/dms` are moved once, at the first start,
  from the flat layout ``dms/<id>`` to ``dms/<year>/<month>/<block>/<id>.<ext>``.
  The directory must not be modified by hand. Deleted content is first
  moved to ``dms/_trash/<date>/`` and physically removed after
  ``elza.dms.trashRetentionDays`` (default 30); until then a deletion can
  be undone. A new consistency check runs at ``elza.dms.check.cron``
  (default Sunday 03:30, ``-`` disables it) and treats a file as orphaned
  only when it is older than ``elza.dms.orphanMinAgeMinutes`` (default 60).

  Before the upgrade, back up the database and the :file:`dms` directory
  from the same moment. Returning to an older version after the migration
  requires restoring both, because an older version does not find files
  in the new layout. See :doc:`backup`.
- **Removed** - **config**, **api**, **upgrade** - The SOAP fund service
  only adds fund administrators (``ADD_ONLY``); administrators missing
  from the request are no longer removed. The value ``FULL_SYNC`` of
  ``elza.webservice.fonds.adminPermissionMode`` was removed, and the
  application does not start with it: remove the key from
  :file:`elza.yaml` or set ``ADD_ONLY``. ``NO_SYNC`` ignores the
  administrators in the request.
- **Changed** - **api** - The administrative endpoints for asynchronous
  requests moved from ``/api/admin/asyncRequests`` and
  ``/api/admin/asyncRequests/{requestType}`` to
  ``/api/v1/admin/async-requests`` and
  ``/api/v1/admin/async-requests/{requestType}`` (#10054).

3.4.7 (2026-09-21)
==================

- **Changed** - Linking an AIP from the digital archive to a fund and
  unlinking it requires the arrangement permission for that fund.

3.4.6 (2026-09-15)
==================

- **New** - **config**, **api** - Personal API keys (#10011). Integrations
  can call the REST API with a key sent in the ``X-API-Key`` header instead
  of a user's password. The feature is enabled by default; the settings
  are in ``elza.security.api-keys``: ``enabled`` (default ``true``),
  ``header-name`` (default ``X-API-Key``), ``default-validity-days``
  (default 365) and ``max-validity-days`` (default 730). See
  :doc:`security`.
- **New** - **config** - Batch import of archival funds (#9991). Files for
  import from a folder on the server are taken from the directory set in
  ``elza.import.batchInputDir``; users can browse only its subdirectories.
  Without the setting, import from a server folder is disabled (upload
  from the browser still works). Batch files are kept for 30 days and
  removed by the regular clean-up (``elza.cleanup.cron``).
- **Changed** - **config** - Generating large outputs no longer keeps the
  whole document in memory. Pages are swapped to a file in the temporary
  directory: ``elza.export.jasperPageCacheSize`` (pages kept in memory,
  default 100, ``0`` keeps everything in memory),
  ``elza.export.jasperSwapBlockSizeKb`` (default 1024) and
  ``elza.export.jasperSwapMinGrowCount`` (default 100). The number of
  archival entities cached per output is limited by
  ``elza.export.outputRecordCacheSize`` (default 1000, ``0`` means no
  limit). The default of ``elza.asyncActions.output.threadCount`` changed
  from 4 to 2, because output generation is memory-intensive.

3.4.5 (2026-08-30)
==================

- **Removed** - **config** - The synchronisation timer of the digital
  archive in :file:`elza.yaml` (``elza.da.sync``, including ``syncAt`` and
  ``resetAt``) is no longer read. The synchronisation interval is set on
  the digital repository in *External systems* (default 300 seconds,
  ``0`` disables synchronisation). Remove the section from
  :file:`elza.yaml` and check the interval after the upgrade.
- **Changed** - The script that links the digital archive (``IMPORT_DA``)
  moved from the ``CZ_BASE`` package to the ZP2015 rules. Upgrade both
  packages together (CZ_BASE 88 and ZP2015 343 or later); the packages
  bundled with the distribution satisfy this.

3.4.4 (2026-08-20)
==================

No changes that require the administrator's attention.

3.4.3 (2026-08-16)
==================

- **New** - File-system repositories of digital objects (#9944) are set
  up and tested in *External systems*.
- **Changed** - Passwords stored in an older hash format are converted to
  the current format when the user logs in (#9962).

3.4.2 (2026-08-04)
==================

No changes that require the administrator's attention.

3.4.1 (2026-07-23)
==================

- **Changed** - API keys and passwords in the settings of external systems
  are masked in the administration.

3.4.0 (2026-07-21)
==================

- **Changed** - **upgrade** - Java 17 is required.
- **New** - **config** - Health and Prometheus metrics are published on a
  separate management port, by default ``8081`` bound to ``127.0.0.1``
  (``management.server.*``). The LDAP health check is active only when
  LDAP authentication is configured (override with
  ``management.health.ldap.enabled``). See :doc:`monitoring`.
- **New** - **config** - Observability of the task scheduler:
  ``elza.monitoring.scheduler.poolSize`` (default 4),
  ``elza.monitoring.scheduler.heartbeatMs`` (default 30000) and
  ``elza.monitoring.scheduler.stuckThresholdSeconds`` (default 1800; a task
  running longer is logged with its stack trace).
- **Changed** - **config** - The interval for downloading archival
  entities from CAM moved from :file:`elza.yaml`
  (``elza.accesspoints.sync[].syncDelay``) to the settings of the external
  system (#9925). Existing values are copied to the external system once,
  at the first start of 3.4; afterwards the value in *External systems* is
  used and a change applies without a restart.
- **New** - **config**, **api** - ``elza.webservice.fonds.adminPermissionMode``
  controls how the SOAP fund service updates fund administrators:
  ``FULL_SYNC`` makes them match the supplied list (administrators not in
  the request are removed), ``ADD_ONLY`` only adds them. The distribution
  sets ``FULL_SYNC`` (removed after 3.4.7, see above).
- **New** - Integration with a digital archive: a new external system type
  (digital repository), overview of AIPs and linking AIPs to the
  description. See :doc:`integrations`.
- **New** - **config** - Integration with an AI provider as a new external
  system type. Timeouts: ``elza.ai.poll-failure-timeout-seconds`` (default
  300) and ``elza.ai.request-lifetime-timeout-seconds`` (default 1800).
- **New** - Publishing archival description through publication systems
  (*Publication systems* in the administration), with the new permissions
  ``FUND_PUBLISH`` and ``FUND_PUBLISH_ALL``. Assign them to the users who
  publish.
- **New** - **config** - A separate queue for exports, with
  ``elza.asyncActions.export.maxPerFund`` (default 1). Its threads follow
  ``elza.asyncActions.output.threadCount``;
  ``elza.asyncActions.export.threadCount`` is read but has no effect.
