============================================
International entity description (ISAAR_CPF)
============================================

The package ``ISAAR_CPF`` (module :file:`package-isaar-cpf`) describes
archival entities according to ISAAR(CPF): persons, families and corporate
bodies, with places and concepts they relate to. Its texts are English. It
is delivered with ELZA and imported at the first start, next to the Czech
entity description CZ_BASE (methodology CAM); the two packages do not
depend on each other, so an installation may run either one or both.

A scope of archival entities uses the package by choosing its rule set
``ISAAR_CPF`` in the scope form. Entities of a scope are created, offered
items, validated and named by the rules of that rule set only.

What the package declares
=========================

The package shares its definitions with CZ_BASE wherever the meaning is
the same, by declaring the same codes (see *Classes shared by packages*,
*Part types shared by packages*, *Item types shared by packages* and
*Specifications shared by packages* in :doc:`02-package-files`). In an
installation with both packages each definition exists once; in an
installation with ISAAR_CPF alone the package owns them.

Classes
   ``PERSON_INDIVIDUAL`` (Person), ``FAMILY`` (Family), ``PARTY_GROUP``
   (Corporate body), ``GEO`` (Place) and ``TERM`` (Concept) are the members
   of the rule set, all assignable. ``PERSON`` and ``DYNASTY`` are declared
   only as the parents of the person and family classes: CAM's root "person
   or being" is wider than ISAAR's person (it includes beings and animals),
   so the class whose meaning equals the standard's is used. The class tree
   of an ISAAR scope therefore shows five root classes; CAM scopes keep
   CAM's tree. CAM's subclasses (kinds of corporate bodies, fictional
   persons, family branches ...) are not members: ISAAR records the kind of
   a corporate body as data (type and legal status), as the standard does.

Parts
   ``PT_NAME``, ``PT_IDENT``, ``PT_BODY``, ``PT_CRE``, ``PT_EXT``,
   ``PT_EVENT``, ``PT_REL``, in this order (the ISAAR(CPF) areas). Dated
   facts of an entity (an occupation, a membership, a change of legal
   status) are event parts; places and concepts are relation parts.

Item types and specifications
   Shared with CZ_BASE: names (``NM_MAIN``, ``NM_MINOR``, ``NM_TYPE``,
   ``NM_LANG``, dates of use), identifiers (``IDN_TYPE``, ``IDN_VALUE``),
   relations (``REL_ENTITY`` with dates), dates of existence (``CRE_DATE``,
   ``EXT_DATE`` with their kinds), events (``EV_TYPE`` with dates), texts
   (``HISTORY``, ``BRIEF_DESC``, ``GENEALOGY``, ``CORP_STRUCTURE``,
   ``FOUNDING_NORMS``, ``SCOPE_NORMS``, ``SOURCE_INFO``, ``SOURCE_LINK``,
   ``NOTE``) and places (``COORD_POINT``, ``GEO_ADMIN_CLASS``). Of CAM's
   specifications the package declares the forms of names, languages,
   identifier types, kinds of beginning and end, event types and relation
   types whose meaning ISAAR(CPF) needs, with English texts; their classes
   of related entities are CAM's. Own item types carry what CAM has as
   classes or lacks: ``ISAAR_CORP_TYPE``, ``ISAAR_LEGAL_STATUS``,
   ``ISAAR_FAMILY_TYPE``, ``ISAAR_PLACE_TYPE`` (the nine EAC-CPF place
   types), ``ISAAR_CONCEPT_SCHEME``, and the texts ``ISAAR_FUNCTIONS``,
   ``ISAAR_PLACES``, ``ISAAR_GENERAL_CONTEXT``. The relation
   ``ISAAR_RT_ASSOCIATED`` replaces CAM's ``RT_RELATED``, whose related
   classes the package does not declare. The languages of a name
   (``NM_LANG``) are the ISO 639-2 list without the range reserved for
   local use: 486 languages with English names, coded ``LNG_`` plus the
   bibliographic ISO 639-2 code as in CAM. 142 of them are CAM's languages
   too and are shared, the others only ISAAR scopes
   offer; CAM's own codes outside ISO 639-2 (``LNG_0as``, ``LNG_hbo`` ...)
   are offered only in CAM scopes.

Rules
   ``AVAILABLE_ITEMS`` rules per part and class, a ``VALIDATION`` rule set
   (at least one name, one description, one beginning and end of
   existence) and an ``INDEX`` script per part build the names of parts:
   a person is "Main part, Other part", the short name is the main part.
   There is no ``AUTO_ITEMS`` script.

Mapping of CAM classes
======================

An entity moved from a CAM scope to an ISAAR scope keeps its class when
the class is a member of ISAAR's rule set (``PERSON_INDIVIDUAL``,
``FAMILY``, ``PARTY_GROUP``, ``GEO``, ``TERM``); CAM subclasses of
``PARTY_GROUP``, ``GEO`` and the fictional or non-human classes are not
offered in ISAAR scopes. What CAM expresses as a subclass, ISAAR_CPF
expresses as the root class plus a value of its own item types; the
table gives the correspondence used when an entity is translated between
the frameworks (the ISAAR value is a specification of the item type named
in the column head, its code is the item type code plus the value).

.. list-table::
   :header-rows: 1
   :widths: 28 22 50

   * - CAM class
     - ISAAR_CPF class
     - ISAAR_CPF values
   * - ``REGION``
     - ``PARTY_GROUP``
     - ``ISAAR_CORP_TYPE`` TERRITORIAL; the territory itself is a ``GEO``
       entity related by ``RT_GEOSCOPE``
   * - ``PUBLIC_ADMINISTRATION``
     - ``PARTY_GROUP``
     - ``ISAAR_CORP_TYPE`` GOVERNMENT (JUDICIAL, LEGISLATIVE where it
       fits); ``ISAAR_LEGAL_STATUS`` PUBLIC_LAW
   * - ``ORGANIZATION``
     - ``PARTY_GROUP``
     - ``ISAAR_CORP_TYPE`` INTERNATIONAL (umbrella organisation)
   * - ``ARMY``
     - ``PARTY_GROUP``
     - ``ISAAR_CORP_TYPE`` MILITARY
   * - ``COMPANY``
     - ``PARTY_GROUP``
     - ``ISAAR_CORP_TYPE`` BUSINESS or FINANCE; ``ISAAR_LEGAL_STATUS``
       COMPANY, PARTNERSHIP, COOPERATIVE or SOLE_TRADER
   * - ``POLITICAL_PARTY``
     - ``PARTY_GROUP``
     - ``ISAAR_CORP_TYPE`` PARTY
   * - ``CHURCH``
     - ``PARTY_GROUP``
     - ``ISAAR_CORP_TYPE`` RELIGIOUS; ``ISAAR_LEGAL_STATUS`` RELIGIOUS
   * - ``HEALTH_AND_EDU``
     - ``PARTY_GROUP``
     - ``ISAAR_CORP_TYPE`` EDUCATION, HEALTH or CULTURE
   * - ``CHARITY``
     - ``PARTY_GROUP``
     - ``ISAAR_CORP_TYPE`` FOUNDATION; ``ISAAR_LEGAL_STATUS`` FOUNDATION
   * - ``GUILD``
     - ``PARTY_GROUP``
     - ``ISAAR_CORP_TYPE`` PROFESSIONAL
   * - ``CLUB``
     - ``PARTY_GROUP``
     - ``ISAAR_CORP_TYPE`` ASSOCIATION; ``ISAAR_LEGAL_STATUS`` ASSOCIATION
   * - ``PERSON_INDIVIDUAL``
     - ``PERSON_INDIVIDUAL``
     - same class
   * - ``FICTIVE_INDIVIDUAL``, ``PERSON_BEING``, ``PERSON_ANIMAL``,
       ``FICTIVE_DYNASTY``
     - none
     - not agents in ISAAR(CPF) and RiC; CAM only
   * - ``FAMILY``
     - ``FAMILY``
     - ``ISAAR_FAMILY_TYPE`` FAMILY, DYNASTY, HOUSE or CLAN
   * - ``FAMILY_BRANCH``
     - ``FAMILY``
     - ``ISAAR_FAMILY_TYPE`` BRANCH; the parent family related by
       ``RT_GENUSMEMBER``
   * - ``GEO_UNIT``, ``GEO_ADMIN_UNIT``
     - ``GEO``
     - ``ISAAR_PLACE_TYPE`` ADMINISTRATIVE, POPULATED or AREA
   * - ``GEO_NATURE_RES``
     - ``GEO``
     - ``ISAAR_PLACE_TYPE`` AREA or VEGETATION
   * - ``GEO_FORMATION``
     - ``GEO``
     - ``ISAAR_PLACE_TYPE`` ELEVATION
   * - ``GEO_WATERS``
     - ``GEO``
     - ``ISAAR_PLACE_TYPE`` WATER
   * - ``GEO_SEA_FORMATION``
     - ``GEO``
     - ``ISAAR_PLACE_TYPE`` UNDERSEA
   * - ``GEO_SHAPES``
     - ``GEO``
     - ``ISAAR_PLACE_TYPE`` SPOT or ROAD
   * - ``GEO_CLIMATIC_PHEN``, ``GEO_SPACE``
     - ``GEO``
     - no place type (outside the EAC-CPF list)
   * - ``TERM_GENERAL``, ``TERM_TAXONOMY``
     - ``TERM``
     - ``ISAAR_CONCEPT_SCHEME`` SUBJECT, OCCUPATION, FUNCTION or OTHER
   * - ``EVENT_*``, ``ARTWORK_*``
     - none
     - CAM only (an event of an entity's life is a ``PT_EVENT`` part)

The translation from CAM to ISAAR is total; from ISAAR to CAM it works
where the value has a CAM counterpart, otherwise the entity keeps the
root class. Nothing in the code applies the table yet; it is the
reference for a future EAC-CPF or CAM export of entities of the other
framework.

Installation without CZ_BASE
============================

With ISAAR_CPF alone the package owns the shared codes, and the only
entity rule set is assigned to scopes without one. Users created from
OAuth2 tokens get the class ``PERSON_INDIVIDUAL`` by default (see the
administration guide). When CZ_BASE is imported later, its declarations
join the existing definitions; the order of CAM's specifications of a
shared item type then follows the packages owning them.
