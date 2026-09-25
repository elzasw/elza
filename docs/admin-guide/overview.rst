=========================
Overview and Requirements
=========================

Architecture
============

ELZA is a web application with a layered architecture:

- **Server** - a Java application (Spring Boot) with an embedded Tomcat
  web server, distributed as a single executable JAR file. No separate
  application server is needed.
- **Database** - PostgreSQL with the PostGIS extension. The database
  schema is created and upgraded automatically at startup (Liquibase).
- **Working directory** - a directory on the server's file system for
  binary files (attachments, generated outputs, publications, import
  batches), rules packages, logs and other working data.
- **Client** - a single-page web application (React) running in the
  user's browser. It communicates with the server over a REST interface
  and receives notifications over WebSocket (STOMP, path ``/stomp``).

ELZA can be integrated with other systems, in particular with the Central
Archival Module (CAM) of the National Archival Portal for archival
entities, with digital archives and digital object repositories, and with
publication systems. See :doc:`integrations`.

Typical deployment
------------------

Users access ELZA through a web server acting as a reverse proxy, which
terminates HTTPS and forwards requests to ELZA's HTTP port. ELZA itself
never needs to be reachable from outside the server.

.. code-block:: text

   users --HTTPS/443--> Apache HTTPD / NGINX --HTTP/8080--> ELZA --> PostgreSQL
                         (certificates,                      |
                          reverse proxy)                     +--HTTPS--> CAM, digital archive, ...

The web server can run on the same host or on a separate one. See
:doc:`reverse-proxy`.

Server requirements
===================

Hardware
--------

================================= ======= ===========
Resource                          Minimum Recommended
================================= ======= ===========
RAM                               4 GB    8 GB
CPU cores                         2       4
Disk space for the application    4 GB    16 GB
Disk space for the database       15 GB   60 GB
================================= ======= ===========

The minimum values are suitable for testing and development. The
recommended values are for a production instance of a specialised archive
with up to about ten concurrent users. The load depends mainly on:

- the number of concurrent users,
- the size of the largest archival fund (number of description units),
- the total number of archival funds,
- the number of archival entities (particularly with a CAM connection),
- the size of generated outputs and attached files.

Software
--------

======================= ====================================
Component               Version
======================= ====================================
Operating system        Linux (recommended) or Windows
Java                    17
PostgreSQL              12 or newer, with the PostGIS extension
======================= ====================================

PostgreSQL with PostGIS is the only supported database for production.
The embedded H2 database is used only by automated tests.

Client requirements
===================

- A current version of Google Chrome, Mozilla Firefox or Microsoft Edge.
  Apple Safari is supported partially.
- A screen resolution of at least 1280 x 800; 1920 x 1080 is recommended.
- About 1 GB of RAM for the browser tab with the application.
