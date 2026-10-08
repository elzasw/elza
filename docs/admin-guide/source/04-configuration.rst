=======================
Configuration Reference
=======================

The configuration file
======================

ELZA reads its configuration from :file:`config/elza.yaml` in the
working directory of the process (the installation directory), or from
:file:`elza.yaml` in that directory. The file is in YAML format. A sample
is part of the distribution (:file:`server/config/elza.yaml`).

Values not set in the file are taken from the defaults built into the
application. Any value can also be set by an environment variable, with
the key in upper case and dots replaced by underscores (for example
``ELZA_DATA_PASS`` for ``elza.data.pass``), or by a Java system property
(``-Delza.data.pass=...``). Environment variables and system properties
take precedence over the file.

A minimal configuration:

.. code-block:: yaml

   elza:
     data:
       url: jdbc:postgresql://localhost/elza
       user: elza
       pass: secret
     workingDir: /opt/elza/work
     logFile: ${elza.workingDir}/log/elza.log
     siemLogFile: ${elza.workingDir}/log/siem.log

Changes take effect after a restart.

Settings of external systems (CAM, digital repositories, map servers, AI
providers) are not part of the file; they are managed in the
administration (see :doc:`07-integrations`). Authentication is described in
:doc:`05-security`.

Scheduled jobs are configured with cron expressions with six fields:
``<second> <minute> <hour> <day-of-month> <month> <day-of-week>``, for
example ``0 0 4 ? * SAT`` for every Saturday at 4:00. The value ``-``
disables the job.

Database and working directory
==============================

.. list-table::
   :header-rows: 1
   :widths: 32 18 50

   * - Key
     - Default
     - Meaning
   * - ``elza.data.url``
     - (required)
     - JDBC URL of the database, for example
       ``jdbc:postgresql://localhost/elza``.
   * - ``elza.data.user``
     - (required)
     - Database user.
   * - ``elza.data.pass``
     - (required)
     - Password of the database user.
   * - ``elza.data.batchSize``
     - 1000
     - Maximum number of items in one database query with a list of
       values (``IN`` clause) and the flush size of batch operations.
   * - ``elza.workingDir``
     - ``./work``
     - Working directory: binary files (:file:`dms`), packages
       (:file:`dpkg`), search index and other working data. Use an
       absolute path.
   * - ``elza.locale``
     - ``cs``
     - Locale used for sorting texts and for formatting and parsing dates
       in the application. Its language is also the language of the user
       interface for users who have not chosen one (Czech when the client
       has no texts in that language), and the language of names from rules
       packages in requests that name no language.

The connection pool is set to 20 connections
(``spring.datasource.hikari.maximumPoolSize``); raise it only together
with the thread counts below.

Web server and uploads
======================

.. list-table::
   :header-rows: 1
   :widths: 32 18 50

   * - Key
     - Default
     - Meaning
   * - ``server.port``
     - 8080
     - HTTP port of the application.
   * - ``server.address``
     - all addresses
     - Network address the server binds to. Set ``127.0.0.1`` when the
       reverse proxy runs on the same host.
   * - ``elza.upload.max_file_size``
     - ``25MB``
     - Maximum size of one uploaded file. ``-1`` removes the limit.
   * - ``elza.upload.max_request_size``
     - ``100MB``
     - Maximum size of one upload request. ``-1`` removes the limit.
   * - ``elza.appName``
     - ``ELZA``
     - Application name shown in the user interface.
   * - ``elza.integrationScriptUrl``
     - (none)
     - URL of an integration script that adds a custom header and footer
       (see :doc:`07-integrations`).

Use exactly the keys ``elza.upload.*`` with underscores; the standard
``spring.servlet.multipart.*`` keys and the old ``multipart.maxFileSize``
have no effect.

Other settings of the embedded server are described in the `Spring Boot
server properties
<https://docs.spring.io/spring-boot/appendix/application-properties/index.html#appendix.application-properties.server>`_,
for example the session timeout (``server.servlet.session.timeout``), the
session cookie (``server.servlet.session.cookie.name``,
``server.servlet.session.cookie.secure``) or the maximum number of request
threads (``server.tomcat.threads.max``).

Access log
----------

The embedded Tomcat can write one line per HTTP request to an access log.
It is disabled by default. Set also the directory; otherwise Tomcat
writes into a temporary directory:

.. code-block:: yaml

   server:
     tomcat:
       accesslog:
         enabled: true
         directory: ${elza.workingDir}/log
         prefix: access_log
         suffix: .log
         file-date-format: .yyyy-MM-dd
         pattern: "%h %{X-Forwarded-For}i %l %u %t \"%r\" %s %b %{ms}T"
         buffered: false
         rotate: true

The file is named :file:`access_log.<yyyy-MM-dd>.log` and rotated daily.
The fields of the pattern are: ``%h`` client address (the proxy's address
behind a reverse proxy), ``%{X-Forwarded-For}i`` the original client
address passed by the proxy, ``%l`` and ``%u`` identity and user
(normally ``-``; ELZA logins are not visible here), ``%t`` time, ``%r``
request line, ``%s`` status, ``%b`` response size in bytes, ``%{ms}T``
processing time in milliseconds. See the `Tomcat access log documentation
<https://tomcat.apache.org/tomcat-10.1-doc/config/valve.html#Access_Logging>`_
for other fields. ``buffered: false`` writes each line immediately;
``true`` performs better under high load.

Logging
=======

.. list-table::
   :header-rows: 1
   :widths: 32 18 50

   * - Key
     - Default
     - Meaning
   * - ``elza.logFile``
     - (none)
     - Path of the application log, for example
       ``${elza.workingDir}/log/elza.log``. Rotated daily, kept for 30
       days. The last lines are also shown in the administration.
       Without the setting, the application logs only to the console.
   * - ``elza.siemLogFile``
     - (none)
     - Path of the security audit log (authentication events in JSON),
       for example ``${elza.workingDir}/log/siem.log``. Rotated daily,
       kept for 90 days. Without the setting, no audit log is written.
       See :doc:`05-security`.

Log levels are set with the standard ``logging.level.*`` keys, for
example ``logging.level.cz.tacr.elza: debug``.

Background processing
=====================

Long-running work is processed in background queues. Each queue has a
maximum number of threads and, where it makes sense, a maximum number of
tasks running for one archival fund at a time. More threads speed up
processing of many funds but increase the load of the database and the
memory use.

.. list-table::
   :header-rows: 1
   :widths: 40 10 50

   * - Key
     - Default
     - Meaning
   * - ``elza.asyncActions.node.threadCount``
     - 4
     - Threads validating description units.
   * - ``elza.asyncActions.node.maxPerFund``
     - 2
     - Validations running at once for one fund.
   * - ``elza.asyncActions.bulk.threadCount``
     - 4
     - Threads running bulk actions (functions).
   * - ``elza.asyncActions.bulk.maxPerFund``
     - 1
     - Bulk actions running at once for one fund.
   * - ``elza.asyncActions.output.threadCount``
     - 2
     - Threads generating outputs, and also the threads of the export
       queue (see the note below). Output generation needs much memory.
   * - ``elza.asyncActions.output.maxPerFund``
     - 1
     - Outputs generated at once for one fund.
   * - ``elza.asyncActions.export.maxPerFund``
     - 1
     - Exports running at once for one fund.
   * - ``elza.asyncActions.ap.threadCount``
     - 4
     - Threads processing archival entities (validation, generating
       names).

.. note::

   ``elza.asyncActions.export.threadCount`` is read, but in 3.4 the export
   queue is sized by ``elza.asyncActions.output.threadCount``.

Search and caches
=================

.. list-table::
   :header-rows: 1
   :widths: 40 10 50

   * - Key
     - Default
     - Meaning
   * - ``elza.search.node.maxCount``
     - 10000
     - Maximum number of hits of a full-text search in description units.
   * - ``elza.search.node.maxCountPerFunds``
     - 1000
     - Maximum number of hits collected for one fund.
   * - ``elza.search.node.maxTimeMs``
     - 10000
     - Time limit in milliseconds for building a search result.
   * - ``elza.levelTreeCache.size``
     - 30
     - Number of fund version trees kept in memory. Raise it with many
       users working on different funds and enough memory.
   * - ``elza.levelTreeCache.display.daoId``
     - ``true``
     - Show information about linked digital objects in the tree.
   * - ``elza.ap.cache.batchsize``
     - 800
     - Batch size when rebuilding the cache of archival entities.
   * - ``elza.ap.cache.transsize``
     - 800
     - Number of archival entities processed in one transaction when
       rebuilding the cache.

Scheduled maintenance
=====================

.. list-table::
   :header-rows: 1
   :widths: 30 22 48

   * - Key
     - Default
     - Meaning
   * - ``elza.reindex.cron``
     - ``0 0 4 ? * SAT``
     - Full rebuild of the search index, every Saturday at 4:00 by
       default. The index can also be rebuilt in the administration
       (*Rebuild indexes*).
   * - ``elza.cleanup.cron``
     - ``0 0 3 * * *``
     - Daily clean-up: unused data records, import batch files older than
       30 days and expired content of the DMS trash.
   * - ``elza.cleanup.maxBatch``
     - 150000
     - Number of unused data records deleted in one transaction of the
       clean-up. Lower it for shorter transactions.
   * - ``elza.dms.check.cron``
     - ``0 30 3 ? * SUN``
     - Consistency check of the binary file storage against the
       database. Files found orphaned are moved to the trash.
   * - ``elza.dms.orphanMinAgeMinutes``
     - 60
     - Minimum age of a file before the consistency check may treat it
       as orphaned.
   * - ``elza.dms.trashRetentionDays``
     - 30
     - Days after which content moved to :file:`dms/_trash/<date>/` is
       deleted. Until then a deletion can be undone.

Outputs
=======

.. list-table::
   :header-rows: 1
   :widths: 38 16 46

   * - Key
     - Default
     - Meaning
   * - ``elza.export.jasperFormat``
     - ``PDF``
     - Format of outputs generated from Jasper templates: ``PDF``,
       ``DOCX``, ``RTF`` or ``ODT``. With a format other than PDF, PDF
       attachments are not merged into the output.
   * - ``elza.export.jasperPageCacheSize``
     - 100
     - Pages of an output kept in memory during generation; other pages
       are swapped to a file in the temporary directory. ``0`` keeps the
       whole document in memory.
   * - ``elza.export.jasperSwapBlockSizeKb``
     - 1024
     - Block size of the swap file in kB.
   * - ``elza.export.jasperSwapMinGrowCount``
     - 100
     - Number of blocks by which the swap file grows.
   * - ``elza.export.outputRecordCacheSize``
     - 1000
     - Archival entities kept in memory while generating one output.
       ``0`` removes the limit.
   * - ``elza.export.mapviewer.url``
     - LightComp map viewer
     - Base URL of the map viewer used for links to coordinates in
       outputs.

Sending outputs to another system
---------------------------------

A generated finding aid can be sent to a system implementing the
FileTransfer SOAP service. Sending is enabled in the user interface by
``elza.output.allowSend`` and requires the sender to be set:

.. code-block:: yaml

   elza:
     output:
       allowSend: true
       senderName: FtOutputSender
     findingAid:
       upload:
         url: https://dms.archive.example/esm/cxf/ft
         username: elza
         password: secret
         soapLogging: false

``soapLogging`` logs the complete SOAP communication.

Attachments
===========

``elza.attachment.mimeDefs`` lists the types of files attached to the
archival description that users may edit in the application, and
optional generators converting them to other formats with an external
program:

.. code-block:: yaml

   elza:
     attachment:
       mimeDefs:
         - mimeType: text/plain
           editable: true
           generators:
             - outputMimeType: application/pdf
               command: txt2pdf {2} {4}
               outputFileName: result.pdf

``mimeType``
   The MIME type of the attachment.
``editable``
   Whether users may edit files of this type. Always set it.
``generators``
   External conversions. ``command`` is run in a temporary directory;
   ``{0}`` is replaced by the path of this directory, ``{1}`` by the name
   of the input file, ``{2}`` by its full path, ``{3}`` by the name of the
   output file and ``{4}`` by its full path. ``outputFileName`` is the
   name of the file the command creates (placeholders are not replaced
   in it), ``outputMimeType`` its type.

Import
======

.. list-table::
   :header-rows: 1
   :widths: 30 16 54

   * - Key
     - Default
     - Meaning
   * - ``elza.import.batchInputDir``
     - (none)
     - Directory on the server from which users can import archival
       funds in batches (*Import from a server folder*). Users can browse
       only its subdirectories. Without the setting, import from a server
       folder is disabled; upload from the browser works regardless.
   * - ``elza.packages.enabled``
     - (none)
     - Codes of the packages to load from :file:`dpkg/` at startup besides
       the packages already imported, for example ``[CZ_BASE, ZP2015]``;
       the packages they depend on are loaded with them. A package of the
       directory that is neither imported nor listed is skipped. Without
       the key, an empty database imports every package of the directory.
       See :ref:`install-packages`.
   * - ``elza.package.testing``
     - ``false``
     - Re-import a package with the same version as the imported one.
       For developing packages only.

Restart from the administration
===============================

A Java process cannot restart itself. The restart offered in
*Administration* > *Package management* stops the application with the
exit code below after the response, and the service manager starts it
again: systemd with ``Restart=on-failure`` (see :doc:`02-installation`)
or a Windows service wrapper. Without a service manager the application
only stops; the confirmation of the restart says so.

.. list-table::
   :header-rows: 1
   :widths: 30 16 54

   * - Key
     - Default
     - Meaning
   * - ``elza.restart.exitCode``
     - ``3``
     - Exit code of the restart from the administration. Change it only
       when the service manager restarts on another code; the unit of
       :doc:`02-installation` restarts on any code other than 0 and 143.

Archival entities
=================

.. list-table::
   :header-rows: 1
   :widths: 34 12 54

   * - Key
     - Default
     - Meaning
   * - ``elza.scope.deleteWithEntities``
     - ``false``
     - Allow deleting a scope that still contains archival entities,
       together with them. Enable it only temporarily to remove scopes
       that are no longer used.

Map layers
==========

``elza.map.layers`` lists the base layers offered in the coordinate
editor. Each layer has a ``name``, a ``type`` (``OSM`` or ``WMS``), a
``url``, for WMS the ``layer`` name, and ``initial: true`` for the layer
shown first:

.. code-block:: yaml

   elza:
     map:
       layers:
         - name: GR_ZM25
           type: WMS
           url: https://geoportal.cuzk.cz/WMS_ZM25_PUB/WMService.aspx
           layer: GR_ZM25
           initial: false

Web services
============

.. list-table::
   :header-rows: 1
   :widths: 42 14 44

   * - Key
     - Default
     - Meaning
   * - ``elza.webservice.fonds.adminPermissionMode``
     - ``ADD_ONLY``
     - How the ``FundService`` updates fund administrators: ``ADD_ONLY``
       adds the supplied users and groups and keeps the others;
       ``NO_SYNC`` ignores the administrators in the request.

AI assistant
============

The AI provider itself is set up as an external system.

.. list-table::
   :header-rows: 1
   :widths: 42 10 48

   * - Key
     - Default
     - Meaning
   * - ``elza.ai.poll-failure-timeout-seconds``
     - 300
     - A request fails when the provider cannot be reached for this long
       without interruption.
   * - ``elza.ai.request-lifetime-timeout-seconds``
     - 1800
     - Maximum age of an unfinished request.

Monitoring
==========

``management.*`` (the management port) and ``elza.monitoring.scheduler.*``
are described in :doc:`08-monitoring`.

Diagnostics
===========

Settings for diagnosing problems, not for normal operation:

.. list-table::
   :header-rows: 1
   :widths: 34 12 54

   * - Key
     - Default
     - Meaning
   * - ``elza.debug.clientLog``
     - ``true``
     - Log messages of the web client to the browser console.
   * - ``elza.debug.performanceLogger``
     - ``false``
     - Log the processing time of every REST request.
   * - ``elza.debug.requests``
     - ``false``
     - Log every HTTP request including its payload, at level DEBUG of
       ``org.springframework.web.filter.CommonsRequestLoggingFilter``.
   * - ``elza.validate.unitdate.enabled``
     - ``true``
     - Validate dates of description units.
   * - ``elza.validate.stringfield.enabled``
     - ``true``
     - Reject empty or whitespace-only text values.

Keys that are no longer used
============================

These keys can be removed from :file:`elza.yaml`:

- ``spring.jpa.properties.hibernate.dialect`` with a PostGIS dialect from
  2.x configurations,
- ``multipart.maxFileSize`` (use ``elza.upload.*``),
- ``elza.accesspoints.sync`` (moved to the external system settings in
  3.4),
- ``elza.da.sync`` (moved to the digital repository settings in 3.4.5),
- ``elza.ap.checkDb``, ``elza.hibernate.index.*``.
