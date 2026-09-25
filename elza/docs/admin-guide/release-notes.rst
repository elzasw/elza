=============
Release Notes
=============

These release notes are written for administrators and integrators. They
list only the changes that an administrator has to act on or will notice
when operating an instance: configuration, upgrade steps, the search
index, interfaces used by other systems, platform requirements and
permissions.

Changes to the user interface, the description rules (ZP2015) and the
bundled packages are described in the Czech release notes of the user
documentation (*Co je nového v Elze* and *Změny v jednotlivých
sestaveních*). A build that brings nothing for the administrator is still
listed here, so that the absence of an entry is a statement, not an
omission.

How to read an entry
====================

Every entry starts with a category:

- **New** - a feature or a setting that did not exist before.
- **Changed** - existing behaviour that now works differently.
- **Fixed** - a defect and what now works.
- **Removed** - a feature or a setting that is gone, with its successor
  when there is one.

An entry that needs the administrator's attention carries one or more
markers after the category. Scan for them before an upgrade:

- **config** - a configuration key was added, changed its default or its
  meaning, or was dropped. The entry names the key.
- **reindex** - the search index has to be rebuilt after the upgrade.
- **api** - a change of the REST or SOAP interfaces that a client may
  notice.
- **upgrade** - a step the administrator has to take before or after the
  upgrade. See :doc:`upgrade`.

An entry without a marker needs nothing from the administrator.

Numbers in an entry refer to the project's issue tracker. Pre-release
builds (BETA, RC) get no heading of their own; their changes appear under
the final version they lead to.

.. toctree::
   :maxdepth: 1

   changelog-3.4
