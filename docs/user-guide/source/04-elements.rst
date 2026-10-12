Elements of description
=======================

The form of a unit of description groups the elements by the seven areas
of ISAD(G). Every element of the standard has one element in the rule set;
a few elements beyond the standard (containers, digital objects, access
points, the finding aid) have their own groups and chapters. The number in
the first column is the number of the element in ISAD(G).

Which elements are required
---------------------------

ISAD(G) names six elements essential for the international exchange of
descriptions: reference code, title, creator, dates, extent and level.
They are **required on the fonds** (the root unit). Below the fonds the
**title** is required, the reference code and the dates are
**recommended**; everything else is possible at every level. The level of
description is required everywhere; the scenarios fill it.

Identity statement
------------------

=====  ===========================  ================================================================
3.1.1  Reference code               Identifies the unit within the fonds; the generator numbers the
                                    units by their position (:doc:`03-levels`). Country and
                                    repository codes are added in outputs.
\      Other identifier             A former reference code, an accession number or another
                                    identifier; choose the kind. Repeatable.
3.1.2  Title                        Formal or supplied title of the unit.
3.1.3  Date(s) of creation          The date or range of dates of creation, as a structured date:
                                    ``1921``, ``1921-1924``, ``Aug 1921``, ``21 Aug 1921``,
                                    ``20th century``, ``500 BC``, ``[1921]`` for an estimated date;
                                    Czech forms are accepted too. The validation checks that the
                                    dates lie within the dates of the parent unit.
\      Bulk dates                   Dates of the bulk of the material when they differ from the
                                    dates of creation.
\      Date(s) as text              The dates in words when a structured date cannot express them.
3.1.5  Extent and medium            The statement as it is printed in the finding aid:
                                    "128 boxes (12.5 linear metres)", "1 folder (45 leaves)".
\      Quantity                     The measured extent, one number with a unit of measure:
                                    linear metres, boxes, folders, volumes, items, sheets, pages,
                                    minutes, bytes. Repeatable, so "12.5 linear metres" and
                                    "128 boxes" are two quantities. Future statistics of the
                                    finding aid sum them.
\      Material type                The form of the material: textual records, registers and
                                    official books, manuscripts, printed matter, maps and plans,
                                    technical drawings, graphic material, photographs, sound
                                    recordings, moving images, electronic records, seals, coins and
                                    medals, three-dimensional objects, other. Repeatable.
\      Dimensions                   Dimensions of the item, e.g. "62 × 48 cm".
\      Scale                        Scale of a map, plan or drawing, e.g. "1:2880"; offered when
                                    the material type is maps and plans or technical drawings.
\      Duration (minutes)           Offered when the material type is sound recordings or moving
                                    images.
3.1.4  Level of description         Fonds, subfonds, series, subseries, file or item
                                    (:doc:`03-levels`).
=====  ===========================  ================================================================

A sample. The fonds *Smith family papers*: extent "128 boxes (12.5 linear
metres)", quantities 12.5 linear metres and 128 boxes. A file in it:
title *Correspondence with the town council*, dates 1921-1924, material
type textual records, extent "1 folder (45 leaves)", quantity 45 sheets,
container box 12. A map in that file: level item, material type maps and
plans, scale "1:2880", dimensions "62 × 48 cm", quantity 1 item.

Context
-------

=====  ===================================  ========================================================
3.2.1  Creator                              The person, family or corporate body that created or
                                            accumulated the unit: an entity of an ISAAR(CPF) scope
                                            (:doc:`06-creators-and-access-points`). Repeatable.
3.2.2  Administrative / biographical        The history of the creator when it is kept with the
       history                              description rather than with the entity.
3.2.3  Archival history                     The custody of the unit before it came to the archive.
3.2.4  Immediate source of acquisition      From whom, when and how the unit was acquired.
       or transfer
=====  ===================================  ========================================================

Content and structure
---------------------

=====  ===================================  ========================================================
3.3.1  Scope and content                    Scope, content and form of the unit.
3.3.2  Appraisal, destruction and           Appraisal and destruction actions and their basis.
       scheduling
3.3.3  Accruals                             Additions expected to the unit.
3.3.4  System of arrangement                Internal structure, order and classification.
=====  ===================================  ========================================================

Conditions of access and use
----------------------------

See :doc:`07-access-and-use` for the status of access and the languages.

=====  ===================================  ========================================================
3.4.1  Conditions governing access          Legal and other conditions of access, as text.
\      Access status                        Open, restricted or closed.
\      Open from                            The date from which a restricted or closed unit opens.
3.4.2  Conditions governing reproduction    Restrictions on reproduction and copyright.
3.4.3  Language of material                 The languages of the material (ISO 639-2). Repeatable.
\      Script of material                   The script or notation system, as text.
3.4.4  Physical characteristics and         Condition of the material and equipment needed to use
       technical requirements               it.
3.4.5  Finding aids                         Finding aids that exist for the unit.
=====  ===================================  ========================================================

Allied materials
----------------

=====  ===================================  ========================================================
3.5.1  Existence and location of originals  Where the originals are when the unit holds copies.
3.5.2  Existence and location of copies     Copies of the unit and where they are.
3.5.3  Related units of description         Related units in this or another repository, as text.
\      Link to related material             A link (URL) to a related unit or resource. Repeatable.
3.5.4  Publication note                     Publications about or based on the unit.
=====  ===================================  ========================================================

Notes
-----

=====  ===================================  ========================================================
3.6.1  Note                                 Information that fits no other element. Repeatable.
\      Internal note                        A note for the archivists; not published.
=====  ===================================  ========================================================

Description control
-------------------

=====  ===================================  ========================================================
3.7.1  Archivist's note                     How the description was prepared and by whom.
\      Archivist                            The person who prepared the description, as an entity.
                                            Repeatable.
3.7.2  Rules or conventions                 The rules the description follows, e.g. "ISAD(G) 2nd
                                            edition".
3.7.3  Date(s) of descriptions              When the description was prepared or revised, as text.
=====  ===================================  ========================================================

Beyond the standard
-------------------

- **Container** - the storage unit the material is kept in
  (:doc:`05-containers`).
- **Digital object** - a link to a digital copy or a born-digital object of
  the unit. Repeatable.
- **Access point** - an entity the unit is indexed by, with its role
  (:doc:`06-creators-and-access-points`). Repeatable.
- The items of the **finding aid** - its title, kind, compiler, date,
  language, note and attachments - belong to the output, not to a unit
  (:doc:`09-outputs`).
