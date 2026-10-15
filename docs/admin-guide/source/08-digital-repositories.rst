============================================
Digital Repositories and the Digital Archive
============================================

Digital objects - scans, born-digital files, archival information
packages (AIP) - are not stored in ELZA. They stay in a repository, and
ELZA links them to the units of description. Each repository is set up in
*Administration* > *External systems* as an external system of the class
*Digital object repository*. Its settings are stored in the database and
apply without a restart; nothing about repositories is configured in
:file:`elza.yaml`.

Creating and changing repositories requires the administrator permission.

Repository types
================

The type is chosen when the repository is created and cannot be changed
later.

================ ===========================================================
Type             Use
================ ===========================================================
Digital archive  A digital archive (DA) storing AIPs. ELZA reads the
                 packages over the REST API of the archive and sends
                 changes back to it (see :ref:`da-repository`).
File system      A directory on the ELZA server or a mounted share. ELZA
                 serves the files itself (see :ref:`fs-repository`).
WSDL             A repository connected over the older SOAP interface,
                 which sends digital objects to ELZA (see
                 :ref:`wsdl-repository`).
================ ===========================================================

Common settings
===============

Code
   Unique identifier of the repository. It cannot be changed later. For a
   digital archive it is part of the entry URL of an AIP
   (``<ELZA_URL>/da/<code>/aip/<AIP_ID>``, see :doc:`07-integrations`); a
   WSDL repository identifies itself by it.
Name
   Name shown to the users.
URL
   Address of the repository; its meaning depends on the type.
ELZA code
   Identifier of this ELZA instance towards the repository. It is sent to a
   WSDL repository and can be used in paths and URL templates as
   ``{repoCode}``.
Multiple links
   Whether one digital object may be linked to more than one unit of
   description at the same time. With *No*, a second link is refused. In a
   digital archive the object is the whole AIP or a part of it.

.. _da-repository:

Digital archive
===============

ELZA communicates with a digital archive over its REST API and over the
File Transfer protocol, which is suitable for large packages:

- the REST API reports changed AIPs, prepares packages for download and
  reports the result of a received export,
- File Transfer carries every export and, when set, the downloads.

Connection
----------

URL
   Base address of the REST API of the digital archive, for example
   ``https://da.archive.example/api``. A trailing slash is ignored.
File Transfer URL
   Address of the File Transfer endpoint. When empty, it is derived from
   the URL as ``<URL>/ws/ft``; the form shows the derived address as a
   hint, and the detail of the repository shows the address in use.
   Fill it in only when the archive runs File Transfer elsewhere.
Username, Password
   Credentials for File Transfer (HTTP Basic). Without a username, File
   Transfer connects without authentication.
FileURL
   Address of the component viewer of the archive. ELZA opens it in a
   frame when a user views a linked digital object. Other URL fields
   (*DaoURL*, *ThumbnailURL*) and *Send notifications* are not used by a
   digital archive.

The button *Test configuration* in the detail of the repository asks the
REST API for the first changed AIP and reports whether the archive
responds. It also shows the File Transfer address, but it does not test
the File Transfer connection.

Synchronisation
---------------

ELZA asks the digital archive periodically which AIPs were added, changed
or invalidated since the last question, and queues a download for each of
them. An invalidated AIP is withdrawn: its links are closed and its local
copy is deleted.

Synchronization interval (s)
   Seconds between two questions; the default is 300 (five minutes).
   ``0`` stops the synchronisation. Saving the repository starts a
   synchronisation at once.

Downloading packages
--------------------

The digital archive prepares the requested packages as a batch; ELZA asks
for the state of the batch until it is ready and then downloads it.

AIP download method
   *Standard (HTTP)* downloads the batch over the REST API. *File
   Transfer* uses the File Transfer endpoint and suits large packages. An
   archive may refuse a standard download of a large batch with HTTP 413;
   the download then fails with a message asking to switch the repository
   to File Transfer.
File Transfer download request type
   Offered only with the File Transfer method. Optional value sent with
   every download request as the type of the request (``type`` of
   ``GenericDataType``, next to the batch identifier in ``id``). Archives
   that serve several kinds of downloads on one endpoint use it to choose
   the handler; agree on the value with the operator of the archive. When
   empty, no type is sent.
Batch status check interval (s)
   Pause between two questions about the state of a batch, for downloads
   and exports alike; the default is 2 seconds, the minimum 1 second.
On AIP received
   *Do nothing* - ELZA downloads only the basic information about the
   package (PACKAGE-INFO); users link the AIP themselves.
   *Download metadata* - ELZA also downloads the metadata of every received
   AIP whose fund it knows, builds its structure and links it to the
   description automatically: by the identifiers (UUID) of the package or
   its parts, and when none matches, by the placement script of the rules
   of the fund, if the rules have one. An AIP that cannot be placed stays
   unlinked, and a problem is recorded on it.

Exports
-------

A user sends the changes of linked AIPs to the digital archive from the
AIP list (*Generate change AIPs*). ELZA builds a change package for each AIP and uploads
it over File Transfer; the archive reports whether it accepted the
package. AIPs not linked to a unit of description are not exported.
Nothing is exported automatically.

The packages being downloaded and exported are kept in the
:file:`aip` directory under ``elza.workingDir``.

Request queue
-------------

Every download and export is a request in the queue of the digital
archive, *Administration* > *Digital archive queue* (also linked from the
detail of the repository). The queue survives a restart: a batch the
archive is preparing is taken up again.

========================= ==================================================
State                     Meaning
========================= ==================================================
To download, To update    Waiting to be requested from the archive.
DA is preparing the       The archive is preparing the batch; ELZA checks
download                  its state.
Updated/Downloaded        Done.
Download error            The package arrived but could not be processed;
                          the problem is also recorded on the AIP.
To export                 Waiting to be uploaded.
Sent, DA is processing    Uploaded; the archive is processing it.
Exported                  Accepted by the archive.
Export error              Refused or not confirmed by the archive.
========================= ==================================================

A request replaced by a newer one for the same AIP, or cancelled, is
marked *inactive*. Requests are not deleted.

When the archive cannot be reached or answers with a server error, the
request is retried after a growing pause: the batch status check interval,
doubled after each failure, at most five minutes. The number of attempts
and the time of the next one are shown in the queue. Downloads are never
given up.

Actions on the selected requests:

Retry now
   Takes a waiting request at once, without waiting for the next attempt.
Cancel request
   Cancels a request the archive has not received yet.
Repeat
   Requests a failed download again; for a failed export, builds a new
   change package from the current description and sends it.

.. _fs-repository:

File-system repository
======================

The URL is a path on the ELZA server, for example
``/mnt/digi/{fundCode}``. It may contain variables, so that each fund has
its own directory:

===================== ==========================================
Variable              Value
===================== ==========================================
``{repoId}``          ID of the repository
``{repoCode}``        ELZA code of the repository
``{fundId}``          ID of the fund
``{fundNumber}``      number of the fund
``{fundCode}``        internal code of the fund
``{fundMark}``        mark of the fund
``{institutionId}``   ID of the institution of the fund
``{institutionCode}`` internal code of the institution
===================== ==========================================

The ELZA service account must be able to read the directory. *Test
configuration* checks that the path exists and can be read and lists its
first entries; for a path with variables only the fixed part is checked.

.. _wsdl-repository:

WSDL repository
===============

A WSDL repository sends digital objects to ELZA through the SOAP service
``DaoCoreService`` (see :doc:`07-integrations`) and identifies itself by
the code of the repository. ELZA calls back the services ``DaoRequests``
and ``DaoNotifications`` of the repository at the URL followed directly
by the service name, so the URL must end with a slash, for example
``https://dms.archive.example/ws/``. Username and password are used for
these calls.

Send notifications
   *Yes* - ELZA notifies the repository when a user links or unlinks one
   of its objects.
DaoURL, FileURL, ThumbnailURL
   Templates of links to a digital object, to a file and to a thumbnail in
   the viewer of the repository. Variables: ``{code}`` (code of the object
   or file), ``{repoId}``, ``{repoCode}``, ``{id}``, ``{label}``,
   ``{nodeId}`` and ``{nodeUuid}`` (the linked unit of description) for an
   object, ``{fileName}`` for a file.

New installations connect a digital archive instead of a WSDL repository.
