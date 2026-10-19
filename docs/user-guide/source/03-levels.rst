Levels of description
=====================

The six levels
--------------

ISAD(G) describes a fonds as a hierarchy. The rule set uses the six levels
of the standard and, for an artificial assemblage, the collection; every
unit of description carries one of them in the element *Level of
description*.

===========  ================================================================
Level        What it is
===========  ================================================================
Fonds        The whole of the records of one creator. The root of the
             description is a fonds or a collection.
Collection   An artificial assemblage of documents brought together on some
             common characteristic without regard to their provenance; a
             root like the fonds.
Subfonds     A subdivision of a fonds for records of an organisational unit
             or a major function. A subfonds may contain subfonds.
Series       Records kept together because they result from the same
             activity or have the same form. A series may contain series.
Subseries    A subdivision of a series. A subseries may contain subseries.
File         An organised unit of documents grouped together for current use
             or in the process of arrangement. A file may contain files.
Item         The smallest intellectually indivisible unit: a letter, a
             photograph, a map. An item may contain items (parts).
===========  ================================================================

What may be below what
----------------------

The scenarios offered when you add a unit follow the hierarchy of the
standard:

- below a **fonds** or a **collection**: subfonds, series, file, item;
- below a **subfonds**: subfonds, series, file, item;
- below a **series**: series, subseries, file, item;
- below a **subseries**: subseries, file, item;
- below a **file**: file, item;
- below an **item**: item.

A small fonds may thus consist of the fonds and its files or items
directly; a large one of subfonds, series and subseries.

Changing the level of a unit
----------------------------

The level is an element like any other and can be changed later - for
example when a file turns out to be a series. The validation then checks
that the unit still fits below its parent and that the units below it
still fit below the unit; a unit out of place is reported with
*The level of description does not fit below the level of the parent unit*
(:doc:`08-validation`). A new fonds is created with the level fonds on its
root; the root may be changed to a collection and to nothing else.

Reference codes
---------------

The reference code (ISAD(G) 3.1.1) identifies the unit within the fonds.
The standard prescribes no form for it, and ELZA takes the code as the
archivist assigns it, in the convention of the archive: the letters and
numbers of the fonds, a running number within the fonds, a series code
with the number of the file, or a code that extends the code of the
parent, such as ``F 7``, ``F 7/12`` and ``F 7/12/3``. Assign the codes
once the arrangement is settled and keep them; a unit moved later keeps
its code. A code that is given up is kept in *Other identifier* with the
kind *Former reference code*. The validation reports a code carried by
two units of the fonds (:doc:`08-validation`). The country and repository
codes that ISAD(G) puts in front of the local reference code are added by
the outputs from the institution of the fonds.
