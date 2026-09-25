============
Integrations
============

Connections to other systems are set up in *Administration* >
*External systems*. Each external system has a code, a name, a URL and,
depending on its class, access credentials and further settings. The
classes are:

- **archival entity systems** - CAM and systems with the same interface,
  for sharing archival entities (see :ref:`integrations-cam`),
- **digital repositories** - repositories of digital objects and digital
  archives (AIP),
- **GIS systems** - map servers for displaying and editing coordinates
  (see :ref:`integrations-gis`),
- **AI providers** - the service behind the AI assistant.

Publication systems, which receive published archival description, are
managed separately in *Administration* > *Publication systems*.

.. todo::

   Digital repositories and the digital archive (AIP synchronisation,
   download modes, file-system repositories), AI providers and
   publication systems: settings and their meaning.

.. _integrations-cam:

CAM
===

ELZA shares archival entities with the Central Archival Module (CAM) of
the National Archival Portal and with other systems implementing its
interface.

Connection types
----------------

The type is chosen when the external system is created:

- **CAM** - the standard CAM interface; entities are paired by the
  numeric CAM ID. Only entities used in ELZA are copied locally.
- **CAM complete** - like CAM, but all entities stored in CAM are
  downloaded automatically and kept as local copies. Requires a scope for
  the downloaded entities.
- **CAM UUID** - for other information systems implementing the CAM
  interface that use a UUID as the primary identifier.

Each type exists for version 1 and version 2 of the CAM interface
(``CAM_V2``, ``CAM_COMPLETE_V2``, ``CAM_UUID_V2``). Version 2 also
provides the warnings and errors reported by CAM and the revision
history of an entity.

The type cannot be changed later, because the stored bindings depend on
it. The only exception is the change from CAM to CAM complete (below).

Synchronisation interval
------------------------

The interval between checks for changes in CAM is set on the external
system (in seconds) and applies without a restart. Five minutes (300
seconds) is a suitable value for both test and production instances of
CAM. The former setting in :file:`elza.yaml`
(``elza.accesspoints.sync``) is no longer used; its values were copied to
the external systems during the upgrade to 3.4.

The state of the synchronisation queue can be monitored with the metrics
described in :doc:`monitoring`.

Changing CAM to CAM complete
----------------------------

#. In the settings of the external system, select the scope for storing
   the entities.
#. Stop ELZA.
#. Change the type in the database, for example when there is only one
   external system:

   .. code-block:: sql

      UPDATE ap_external_system SET type = 'CAM_COMPLETE';

   Use ``CAM_COMPLETE_V2`` for a system with interface version 2.
#. If the table ``ap_binding_sync`` contains a row for the external
   system, set its last transaction to the initial value
   ``91812cb8-3519-4f78-b0ec-df6e951e2c7c``, so that all entities are
   downloaded. Without a row, nothing has to be changed.
#. Start ELZA.

Java and older certificates
---------------------------

Current Java distributions reject certificates signed with older
algorithms. If the connection to CAM fails with::

   java.security.cert.CertPathValidatorException: Algorithm constraints check failed on signature algorithm: SHA1withRSA

allow the algorithm in the Java security settings: in the file
:file:`java.security` of the Java installation, or in a local copy passed
with ``-Djava.security.properties=<file>``. On distributions with
system-wide crypto policies (Red Hat and derivatives, openSUSE; the
directory :file:`/etc/crypto-policies/back-ends/` exists), the algorithm
is usually allowed with ``update-crypto-policies --set DEFAULT:SHA1``;
check the documentation of the distribution.

.. _integrations-gis:

Map servers
===========

Coordinates of archival entities and in the archival description are
shown on a map. A basic preview uses OpenStreetMap. More advanced
functions are provided by a map server implementing the ELZA map
interface (https://geoedit.lightcomp.cz/doc), in two modes: display of a
map with coordinates, and editing of coordinates with saving them back to
ELZA. Each mode is set up as a separate external system of the GIS
class:

========= =============================================
Field     Value
========= =============================================
Type      Display or Edit
Name      for example ``mapview``
URL       for example ``https://geoedit.lightcomp.cz``
API key   optional, as provided by the map server
========= =============================================

Map layers offered in the coordinate editor are configured in
``elza.map.layers`` (see :doc:`configuration`). Map data from mapy.cz are
available only as a separate service, for licensing reasons.

REST API
========

The REST API is available at ``<ELZA_URL>/api/v1``. Its OpenAPI
definition can be browsed in Swagger UI at ``<ELZA_URL>/swagger``.

Integrations should authenticate with a personal API key sent in the
``X-API-Key`` header rather than with a user's password (see
:doc:`security`).

SOAP web services
=================

The SOAP services are available at ``<ELZA_URL>/services/<service>``; the
WSDL is returned by ``<ELZA_URL>/services/<service>?wsdl``. The services
are:

- ``DaoCoreService`` - digital objects,
- ``ExportService`` - export of archival description,
- ``ImportService`` - import,
- ``FundService`` - archival funds and their administrators,
- ``StructuredObjectService`` - structured objects,
- ``UserService`` - users.

A digital object sent to ELZA declares in the ``daoType`` attribute how
it relates to the description:

``attachment``
   The object is attached to an existing description unit.
``level``
   The object is a description unit itself. Attaching it to another unit
   creates a new child unit, which may carry description items sent with
   the object.

When the ``FundService`` updates a fund, the supplied fund
administrators are added to the existing ones
(``elza.webservice.fonds.adminPermissionMode``, see :doc:`configuration`).

Entry URLs
==========

Records can be opened directly by a URL:

``<ELZA_URL>/node/<UUID>``
   A description unit, by its UUID.
``<ELZA_URL>/entity/<UUID>`` or ``<ELZA_URL>/entity/<ID>``
   An archival entity, by its UUID or database ID (recognised
   automatically).
``<ELZA_URL>/entity/<EXT_SYSTEM_CODE>-<EXT_ID>``
   An archival entity by its identifier in an external system, for
   example ``/entity/CAM-100``.

Another application can ask ELZA to create a new archival entity:

.. code-block:: text

   <ELZA_URL>/entity-create?response=<return URL>&entity-class=<class code>

``response``
   The URL to which the browser returns when the user finishes. It may
   contain the variables ``{status}`` (``SUCCESS`` or ``CANCEL``),
   ``{entityUuid}`` and ``{entityId}`` (empty when no entity was
   created). The value must be URL-encoded.
``entity-class``
   Optional; restricts the new entity to a class, for example
   ``PARTY_GROUP``.

Example (before URL encoding):

.. code-block:: text

   https://elza.archive.example/entity-create?entity-class=PARTY_GROUP&response=https://is.archive.example/entity-response?status={status}&entity={entityUuid}

Integration script
==================

A custom header and footer can be added to every page of the application
with an integration script:

.. code-block:: yaml

   elza:
     integrationScriptUrl: https://intranet.archive.example/elza/integration.js

The script may define the functions
``renderIntegrationHeader(headerElement)`` and
``renderIntegrationFooter(footerElement)``. ELZA calls them with an empty
``div`` element into which the function renders its content. The global
variables ``versionNumber`` (the ELZA version) and ``serverContextPath``
(the path of the application) are available to the script.
