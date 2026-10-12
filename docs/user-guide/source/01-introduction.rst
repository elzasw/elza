Introduction
============

What the rule set is
--------------------

ISAD(G), the *General International Standard Archival Description* of the
International Council on Archives, defines twenty-six elements of
description in seven areas and a hierarchy of levels from the fonds down to
the item. The rule set **ISAD(G) archival description** (the rules package
``ISADG``) brings the standard into ELZA:

- one element of the description for every ISAD(G) element, grouped by the
  seven areas of the standard (:doc:`04-elements`);
- the six levels of description - fonds, subfonds, series, subseries, file,
  item - and the rules of their hierarchy (:doc:`03-levels`);
- containers, the storage units the material is kept in (:doc:`05-containers`);
- creators and access points as archival entities described with ISAAR(CPF)
  (:doc:`06-creators-and-access-points`);
- validation of the description against the standard (:doc:`08-validation`)
  and the finding aid built from it (:doc:`09-outputs`).

The texts of the rule set - the names of the elements, the vocabularies,
the validation messages - are English. A Czech translation of the names
is part of the package; an archivist working with the Czech user interface
sees the elements in Czech.

Who it is for
-------------

Archives that describe their holdings by ISAD(G) in English, and
institutions that need an international description next to a national
one. A Czech installation can run the rule set next to the Czech rules
ZP2015: every fonds is described under one rule set, chosen when the
fonds is created, and the two kinds of fonds live in the same
installation.

How it fits in ELZA
-------------------

**Rules packages.** The rule set comes with ELZA as the package ``ISADG``.
It needs the package ``ISAAR_CPF``, the international description of
entities, because creators and access points of a fonds are entities. An
administrator enables both packages in the configuration of the
installation; see "Loading the packages" in the administration guide.

**Fonds and rule sets.** A fonds is described under exactly one rule set.
The rule set decides which elements a unit of description offers, which of
them are required, which levels can be added where, what the validation
reports and which outputs can be generated.

**Scopes of entities.** Entities live in scopes; a scope describes its
entities under one entity rule set. Creators and access points of an
ISAD(G) fonds come from scopes that use the rule set ISAAR(CPF).

**Languages.** The language of the user interface is a setting of the user
(and of the login dialog). The names of the elements and the validation
messages follow it; the texts the archivist writes are the archivist's own,
in whatever language the material calls for. Dates are accepted in English
and Czech forms whatever the language of the interface.

What version 1 does not cover
-----------------------------

- Outputs other than the XML export: the EAD3 and PDF finding aids are
  planned (:doc:`09-outputs`).
- Labels, lists of storage units and the registration of holdings by
  categories of archival material; these belong to the Czech rules ZP2015.
- Structured conditions of access with publication events; the rule set
  records access as a text, a status and an opening date
  (:doc:`07-access-and-use`).
