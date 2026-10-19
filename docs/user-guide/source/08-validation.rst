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
- **The root is a fonds.** *The root of the fonds must have the level
  Fonds (ISAD(G) 3.1.4).*
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
- **Every file or item at the end of a branch is in a container.** *The
  unit and the units above it have no container.* A container on a
  parent counts (:doc:`05-containers`).

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
- **Containers** - the containers at the end of the branches.

Switching a policy off hides its findings; it does not change the
description.
