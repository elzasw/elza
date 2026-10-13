# ISAAR(CPF) / EAC-CPF against CAM (CZ_BASE) — comparison

Analysis behind the entity description frameworks of
[rules-customization-layer.md](rules-customization-layer.md); the decisions taken on it are in
section 3 there and in [entity-framework-research.md](entity-framework-research.md). Paths are
relative to the repository root.

Both questions have a positive core answer, with specific gaps.

**Short answers**

- **Parts.** CAM's part model carries everything ISAAR(CPF) asks for. The areas of ISAAR are a
  presentation order, not a data model, and the repeatable things in ISAAR and EAC-CPF correspond one
  to one to CAM part kinds. Three ISAAR elements have no CAM carrier, and CAM events have no ISAAR
  counterpart.
- **Classification.** CAM classes map onto the ISAAR entity type without loss in the CAM to ISAAR
  direction. The reverse direction fails for corporate bodies, because CAM has no generic corporate
  subclass and roots cannot be assigned. Four of the seven CAM classes lie outside ISAAR altogether.

Note on counting: CZ_BASE defines seven part codes in `package-cz-base/src/rul_part_type.xml`. They
are five kinds, since creation and extinction are events with fixed semantics and a nested relation
part, exactly like a generic event.

## ISAAR(CPF) elements against CAM parts

| ISAAR(CPF) | CAM carrier in CZ_BASE | Fit |
|---|---|---|
| 5.1.1 Type of entity | root entity class (PERSON, PARTY_GROUP, DYNASTY) | direct, see below |
| 5.1.2 Authorized form of name | preferred PT_NAME part; NM_MAIN, NM_MINOR, supplements, degrees | direct; the name parts become EAC-CPF `part` elements |
| 5.1.3 Parallel forms | further PT_NAME parts with NT_EQUIV or NT_TRANSLATED and NM_LANG | direct |
| 5.1.4 Forms by other rules | PT_NAME with NT_OTHERRULES | exact |
| 5.1.5 Other forms | PT_NAME with NT_ALIAS, NT_PSEUDONYM, NT_FORMER, NT_ACRONYM, NT_NATIV …, with use dates | direct, richer than ISAAR |
| 5.1.6 Identifiers for corporate bodies | PT_IDENT with IC, VAT, RUIAN, ISO 3166 … | direct, but see the split below |
| 5.2.1 Dates of existence | PT_CRE and PT_EXT dates; CRE_CLASS and EXT_CLASS say what the date means | direct, richer |
| 5.2.2 History | HISTORY, BIOGRAPHY, DESCRIPTION | direct |
| 5.2.3 Places | no text item; relations to GEO entities (residence, place, geographic scope, location), standalone or inside events | derived; export renders the GEO entity's preferred name |
| 5.2.4 Legal status | none; partly implied by the corporate subclass | gap |
| 5.2.5 Functions, occupations, activities | events of type job or membership with nested function or activity-field relations to TERM entities; CORP_PURPOSE text | derived |
| 5.2.6 Mandates | FOUNDING_NORMS, SCOPE_NORMS; document relations to ARTWORK_CHARTER | direct |
| 5.2.7 Internal structures, genealogy | CORP_STRUCTURE, GENEALOGY | direct |
| 5.2.8 General context | DESCRIPTION, NOTE; BRIEF_DESC has no ISAAR element, EAC-CPF has `abstract` | direct |
| 5.3.1 Related entity | standalone PT_REL, REL_ENTITY | direct |
| 5.3.2 Category of relationship | the relation specification, about a hundred values | needs a mapping table to hierarchical, temporal, family, associative; EAC-CPF adds identity, which fits "other entity for the same person" |
| 5.3.3 Description of relationship | NOTE, available in every part by the global rule | direct |
| 5.3.4 Dates of relationship | REL_BEGIN, REL_END | direct |
| 5.4 Control area | record metadata, not parts: uuid and CAM binding, approval state, change records and revisions, NOTE_INTERNAL, SOURCE_INFO and SOURCE_LINK in the body | mostly direct |
| 5.4.5 Level of detail, 5.4.7 language of the record | none | gap, could be constants or derived |
| 6 Related archival resources | fund side only: description items that reference the entity | reverse lookup, EAC-CPF `resourceRelation` |

Three things to watch:

- **PT_IDENT mixes two ISAAR areas.** Registry numbers such as IC or RUIAN are identifiers of the
  entity. VIAF, LCNAF, AUT and INTERPI are identifiers of other authority records, which ISAAR puts in
  the control area. An export has to split them by the identifier type.
- **Events have no ISAAR counterpart.** EAC-CPF offers a chronology list inside the history, and dated
  place, function, occupation, legal-status and mandate elements. A CAM event with its nested
  relations flattens into a chronology item plus dated relations. That is lossy, but every fact
  survives.
- **Places and functions are links, not text.** This is stronger than ISAAR, and it means the class of
  the target entity decides the EAC-CPF element. A relation to a GEO entity becomes a place, to a TERM
  a function or occupation, to a CPF entity a relation. Classification is therefore an input to the
  export, not just a label.

Parts per ISAAR area or per element: parts per repeatable EAC-CPF element, which is the CAM part
model, not parts per ISAAR area. The identity area mixes one non-repeatable field with two repeatable
sets. A part per area would push names and identifiers down to items and lose the per-name attributes
such as type, language and use dates. The areas are a parts-order setting in the UI.

## Classification

| CAM class | Subclasses | ISAAR type | Note |
|---|---|---|---|
| PERSON | individual, fictional individual, being, animal | person | being and animal fall outside ISAAR's definition; EAC-CPF practice still uses person with a local type |
| DYNASTY | family, family branch, fictional family | family | clean |
| PARTY_GROUP | twelve subclasses | corporate body | subclasses mix legal form, function and territory |
| GEO, EVENT, ARTWORK, TERM | | none | outside ISAAR; places and events are RiC-CM territory, functions ISDF |

Assessment:

- **CAM to ISAAR is a total function.** Subclass to root class to ISAAR type, lossless. The two-level
  hierarchy with read-only roots in `package-cz-base/src/ap_type.xml` is harmless, because an exported
  record only shows the root.
- **ISAAR to CAM is not.** Person and family have natural default subclasses. A corporate body with
  unknown legal form has none. ORGANIZATION means an association of organizations, not a generic
  corporation. An ISAAR framework must either make its root types assignable or add a generic
  subclass.
- **The corporate subclass is overloaded.** COMPANY, CHARITY and POLITICAL_PARTY are legal forms.
  PUBLIC_ADMINISTRATION, ARMY, CHURCH, GUILD and CLUB are functions. REGION is a territory acting as a
  body. ISAAR expresses these as the description elements legal status and functions. Compatible, but
  on export the subclass has to feed those elements rather than the entity type.
- **Scope.** CAM's seven classes form a RiC-like model of which ISAAR covers three. For the R1
  framework this means three assignable roots, optional local subtypes, and no dependence on the
  other four classes. Reusing the codes PERSON and DYNASTY needs classes bound to a rule set
  (members and assignability per rule set), which ELZA provides; the chosen presentation of persons
  and families is option D in [entity-framework-research.md](entity-framework-research.md).
