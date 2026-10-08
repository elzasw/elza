===========================================
International entity description (ISAAR_CPF)
===========================================

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
   classes the package does not declare.

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
offered in ISAAR scopes. The kind of a CAM corporate body corresponds to
a value of ``ISAAR_CORP_TYPE`` (for example ``COMPANY`` to "Business
enterprise", ``CHURCH`` to "Religious body") and, for legal forms, of
``ISAAR_LEGAL_STATUS``; the mapping is in
:file:`docs/entity-framework-research.md`.

Installation without CZ_BASE
============================

With ISAAR_CPF alone the package owns the shared codes, and the only
entity rule set is assigned to scopes without one. Users created from
OAuth2 tokens get the class ``PERSON_INDIVIDUAL`` by default (see the
administration guide). When CZ_BASE is imported later, its declarations
join the existing definitions; the order of CAM's specifications of a
shared item type then follows the packages owning them.
