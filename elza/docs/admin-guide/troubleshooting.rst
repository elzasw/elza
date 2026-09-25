===============
Troubleshooting
===============

Log files
=========

The log files are set in :file:`elza.yaml` (see :doc:`configuration`);
usually they are in the :file:`log` directory of the working directory:

- :file:`elza.log` - the application log (``elza.logFile``),
- :file:`siem.log` - the security audit log (``elza.siemLogFile``, see
  :doc:`security`),
- :file:`access_log.<date>.log` - HTTP requests, when the access log is
  enabled (see :doc:`configuration`).

When the application does not start, look for the first ``ERROR`` in
:file:`elza.log`; with systemd, also check ``journalctl -u elza``.

The database is locked
======================

The start stops at::

   Waiting for changelog lock....

The previous start did not finish a database migration and left the lock
behind. Make sure no other instance of ELZA runs against the same
database. Then release the lock and start again:

.. code-block:: sql

   DELETE FROM db_databasechangeloglock;

A package is older than the imported one
========================================

The start fails with ``Package is an older version than the one already
imported``. The :file:`dpkg` directory of the working directory contains
an older version of a package than the database. Replace it with the
package from the current distribution or remove it.

Wrong characters in texts
=========================

Czech or other non-ASCII characters are stored or displayed wrongly. The
JVM runs with a non-UTF-8 default encoding. Start Java with:

.. code-block:: bash

   java -Dfile.encoding=UTF-8 -Dsun.jnu.encoding=UTF-8 -jar elza-tomcat.jar

WebSocket does not work
=======================

The application loads, but changes made by other users or the progress
of background tasks do not appear until the page is reloaded. The
reverse proxy does not forward WebSocket connections on ``/stomp``. See
:doc:`reverse-proxy`.

Data fixes for old versions
===========================

Errors when upgrading old 2.x installations are described in
:doc:`legacy-upgrades`.
