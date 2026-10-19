Getting started
===============

Before you start
----------------

- The administrator has enabled the packages ``ISADG`` and ``ISAAR_CPF``
  (administration guide, "Loading the packages"). The rule set *ISAD(G)
  archival description* then appears among the rule sets a new fonds can
  use.
- A scope of entities uses the rule set *ISAAR(CPF) entity description*.
  Creators and access points of the fonds are chosen from such scopes
  (:doc:`10-entities-overview`).
- The institution that keeps the fonds exists in ELZA; every fonds belongs
  to one.

Creating a fonds
----------------

In the archival description, create a new fonds: give it a name, the
institution and the rule set *ISAD(G) archival description*. ELZA creates
the fonds with its root unit of description, which is the **fonds** level
(:doc:`03-levels`). The name of the fonds is the name of the record in
ELZA; the title of the fonds as an element of the description
(:doc:`04-elements`) is filled on the root unit.

The description is a tree of units of description. The tree on the left
shows the reference code, the title and the dates of every unit; the form
on the right shows the elements of the selected unit, grouped by the areas
of ISAD(G).

Adding units of description
---------------------------

A unit is added below, before or after the selected unit. ELZA offers the
**scenarios** of the rule set, named by the level they create: below a
fonds you can add a *Subfonds*, a *Series*, a *File* or an *Item*; below a
series a *Series*, a *Subseries*, a *File* or an *Item*, and so on
(:doc:`03-levels`). The scenario fills the level of the new unit; the title
and the other elements are yours to fill.

Filling the elements
--------------------

The form of a unit shows the elements the rule set offers at its level.
Elements are marked as **required**, **recommended** or possible; a
required element that is empty is reported by the validation. To add an
element that is not yet on the form, open the list of elements of the
unit: it offers every element possible at the level, and every value of a
vocabulary (the kinds of material, the units of measure, the languages,
the access statuses). Repeatable elements - creators, languages,
quantities, kinds of material, access points, containers, notes - can be
added several times.

Changes are saved as you make them. A unit changed by you or by a bulk
action is validated again in the background; the result shows on the unit
and in the list of validation findings of the fonds.

The next steps
--------------

- Describe the fonds itself first: the essential elements of ISAD(G) are
  required on the root (:doc:`04-elements`).
- Build the hierarchy with the scenarios (:doc:`03-levels`), then describe
  the files and items.
- Put files and items into containers (:doc:`05-containers`) and link the
  creators and the access points (:doc:`06-creators-and-access-points`).
- Assign the reference codes (:doc:`03-levels`) and run the validation of
  the whole fonds (:doc:`08-validation`).
