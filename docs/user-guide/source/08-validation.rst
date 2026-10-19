Validation
==========

What is checked
---------------

ELZA validates every unit of description after each change, in the
background, and shows the findings on the unit and in the list of
findings of the fonds. The bulk action *Validation of the description*
validates a whole branch or the whole fonds at once; run it before a
finding aid is generated.

The rule set reports:

- **Missing required elements.** The essential elements on the fonds -
  reference code, title, dates of creation, extent, creator - and the
  title on every unit below it. The level of description is required
  everywhere.
- **Missing recommended elements.** The reference code and the dates of
  creation below the fonds. A recommended element missing is a reminder,
  not an error.
- **The root is a fonds or a collection.** *The root must have the level
  Fonds or Collection (ISAD(G) 3.1.4).*
- **The level fits below the parent's level.** *The level of description
  does not fit below the level of the parent unit (ISAD(G) 3.1.4).*
  Reported when a level was changed or a unit moved so that, for
  example, a series sits below a file. The allowed hierarchy is in
  :doc:`03-levels`.
- **The dates lie within the dates of the parent.** *The dates of creation
  lie outside the dates of the parent unit (ISAD(G) 3.1.3).* The dates of
  a series contain the dates of its files; widen the dates of the parent
  or correct the unit.
- **The reference code is unique in the fonds.** *The reference code is
  not unique in the fonds (ISAD(G) 3.1.1).* Reported on every unit that
  carries the code; change the code of one of them (:doc:`03-levels`).
- **A file or item without a container next to one with a container.**
  *The unit has no container, while a neighbouring unit has one.* A
  container on a parent counts; where no neighbour has a container,
  nothing is reported (:doc:`05-containers`).

The messages are shown in the language of the user interface: the
English texts above, or their Czech translation.

Policies
--------

Every finding belongs to a policy, and the policies can be switched off
for a unit and the units below it in *Validation rules* on the toolbar of
the unit - for a branch that is not processed yet, or where the rule does
not apply:

- **Basic** - the elements and the hierarchy;
- **Reference codes** - the reference codes (recommended below the fonds,
  unique in the fonds);
- **Containers** - a container missing next to a unit that has one.

Switching a policy off hides its findings; it does not change the
description.
