# Keep three agent roots, type them with vocabularies

The international package should make the three ISAAR/EAC-CPF/RiC agent roots (person, family, corporate body) directly assignable and add **no agent subclasses**. The kind of a corporate body should be recorded as data on two separate axes: a corporate-body type and a legal status, each from a controlled vocabulary. This is option (a) of R-a. **No archival standard or major archival profile subclasses agents below the three roots.** ISAAR(CPF) records "the legal status and where appropriate the type of corporate body" as a dated description element. EAC-CPF fixes `entityType` to three values and puts finer typing in `otherEntityType` and `legalStatus`. RiC-O links a corporate body to a `CorporateBodyType` concept through a time-qualified property. The strongest production precedent, the Archives nationales de France, keeps its 64 corporate-body categories as a SKOS vocabulary, not as classes. CAM's twelve PARTY_GROUP subclasses mix legal forms (COMPANY, CHARITY, CLUB), organisational kinds (POLITICAL_PARTY, CHURCH, GUILD), functions (ARMY, HEALTH_AND_EDU) and a structural relation (BRANCH), and all of these come apart cleanly into vocabulary values and relations. For R-b, the package should also declare a minimal non-agent base of Place (shared code GEO) and Concept (shared code TERM), with Event (shared code EVENT) as an optional third. Functions, mandates and works should stay out of the first set. This gives a RiC-ready skeleton that coexists with CAM: the codes are shared where meanings match, and a CAM subclass maps onto a root plus a type value.

## Archival standards stop at three agent types and push "kind" into dated data

The three-way split is the only distinction every source agrees on. ISAAR(CPF) 5.1.1 asks only whether the entity "is a corporate body, person or family". The kind of corporate body goes to **5.2.4 Legal status**, whose rule reads "Record the legal status and where appropriate the type of corporate body together with the covering dates when this status applied". The examples there are national phrases such as "Department of State" and "Organismo de la Administración Central del Estado" ([ISAAR(CPF) 2nd ed.](https://www.archives.go.jp/thinktank/pdf/ISAAR_2nd_EN.pdf)). Functions go to 5.2.5, mandates to 5.2.6, and internal structure or genealogy to 5.2.7. In ISAAR, CAM's subclass content is therefore three or four repeatable, dated description elements, not an identity-level class.

EAC-CPF keeps the same shape in every version. `entityType` allows exactly corporateBody, person and family. From 2.0 on, `<otherEntityTypes>/<otherEntityType>` carries "a specification of the default entity types" as a `<term>` with `@valueURI` and `@vocabularySource` ([EAC-CPF 2.0 schema](https://raw.githubusercontent.com/SAA-SDT/eac-cpf-schema/master/xml-schemas/eac-cpf/eac.rng); [EAC-CPF 3.0.0 Tag Library](https://eac.staatsbibliothek-berlin.de/schema/v3/EAC-CPF3-TL-eng.html)). `<legalStatus>` is a separate term element that can carry a date and a place, and its terms are taken "in accordance with provisions of the controlling legislation" ([EAC-CPF 3.0.0 TL](https://eac.staatsbibliothek-berlin.de/schema/v3/EAC-CPF3-TL-eng.html)). Neither standard prescribes a vocabulary. The Archives Portal Europe profile is stricter still: it has no other-entity-type element at all, only `legalStatus`, `occupation` and `function` with URI-typed `@localType` ([apeEAC-CPF.xsd](https://raw.githubusercontent.com/ArchivesPortalEuropeFoundation/ape-dpt/master/DPTutils/src/main/resources/apeEAC-CPF.xsd)).

RiC is the most explicit on the point. Its agent tree is Agent → Person, Group → {Family, Corporate Body}, Position, Mechanism. Below that, kinds are expressed through **Type entities** linked by properties: `rico:hasOrHadCorporateBodyType`, `rico:hasFamilyType`, `rico:hasOrHadLegalStatus` (the domain of this one includes any Agent, not only corporate bodies), `rico:hasOrHadDemographicGroup` and `rico:hasOrHadOccupationOfType` ([RiC-O 1.1](https://raw.githubusercontent.com/ICA-EGAD/RiC-O/master/ontology/current-version/RiC-O_1-1.rdf)). The "hasOrHad" form matters. It lets a body change its type over time, for example when a state enterprise becomes a joint-stock company, and a fixed ELZA class cannot express that. RiC also names "Dynasty, Clan, House, Tribe" in its Family scope note as other kinds of familial groups, which are FamilyType values. It handles subdivisions through the relation `rico:hasOrHadSubdivision`, not through a type ([RiC-O 1.1](https://raw.githubusercontent.com/ICA-EGAD/RiC-O/master/ontology/current-version/RiC-O_1-1.rdf)).

Implementations follow the standards. SNAC uses only the three EAC types ([SNACSchool glossary](https://portal.snaccooperative.org/system/files/media/documents/Public/SNACSchool%20Training%20Glossary_1.pdf)). The Archives nationales de France publish **FRAN_RI_104 "Catégories des collectivités", 64 mostly flat concepts typed as `rico:CorporateBodyType`** ([FRAN_RI_104 CSV](https://raw.githubusercontent.com/ArchivesNationalesFR/Referentiels/master/concepts/csv/FRAN_RI_104_corporateBodyTypes.csv)). In RDF a corporate body points to one of these concepts through `rico:hasOrHadCorporateBodyType` ([FRAN_Agent_000001.rdf](https://raw.githubusercontent.com/ArchivesNationalesFR/Referentiels/master/agents/rdf/collectivites/FRAN_Agent_000001.rdf)). In EAC-CPF the category travels in `<legalStatus><term vocabularySource="…">` ([FRAN_NP_000001.xml](https://raw.githubusercontent.com/ArchivesNationalesFR/Referentiels/master/agents/eac-cpf/FRAN_NP_000001.xml)). AnF's list mixes legal forms (société anonyme, association loi 1901, EPIC) with organisational kinds (ministère, parti politique, juridiction), which is the same mix CAM has. The difference is that AnF holds it as data.

## Libraries subtype more, but only at the edges CAM does not stress

Library authority files are the one place where agent subtyping exists at scale, and even there it is partial. GND codes corporate bodies as companies (kif), musical bodies (kim), organs of territorial authorities (kio), projects (kip), religious bodies (kir/kiv), fictive bodies (kxz) and "all others" (kiz). Persons are coded as royals, pseudonyms, gods, literary characters and spirits, and families are a person sub-code ([DNB Entitätencodierung](https://wiki.dnb.de/download/attachments/90411323/entitaetenCodes.pdf?api=v2)). A DNB working paper says non-place-bound bodies are "in der Regel noch nicht codiert", so the coding is optional and incomplete ([DNB Koerperschaft.pdf](https://wiki.dnb.de/download/attachments/51744106/Koerperschaft.pdf?api=v2)). GND has no counterpart for ARMY, POLITICAL_PARTY, HEALTH_AND_EDU, CHARITY, GUILD, CLUB or ORGANIZATION. All of these fall into kiz. GND also files **territorial corporate bodies under places, not corporate bodies** (gndo:TerritorialCorporateBodyOrAdministrativeUnit) ([GND Ontology](https://d-nb.info/standards/elementset/gnd)). RDA/MARC keeps three agent types and records "Type of corporate body" and "Type of jurisdiction" as attributes in MARC 368 $a/$b, with a vocabulary source in $2 ([MARC 21 Authority 368](https://www.loc.gov/marc/authority/ad368.html)). Jurisdictions and meetings are flagged by tag and indicator (110 ind1=1, 111) ([MARC 21 Authority 110](https://www.loc.gov/marc/authority/ad110.html)). Wikidata separates the axes explicitly. The class says what kind of body an entity is, and **P1454 "legal form" is a separate single-value property** ([Wikidata P1454](https://www.wikidata.org/wiki/Property:P1454)).

The closest analogue to CAM's sector list is national fonds classification, not authority practice. Poland's SEZAM classification of record creators groups producers into sectors: general and special administration, justice, army, economic self-government, finance, enterprises, associations, political parties, science and education, culture, and health and social care ([AP Warszawa Otwock guide](https://warszawa.ap.gov.pl/container/publikacje/INF-OTWOCK-23-01-2015-FINALNY.pdf)). These groups line up almost one to one with CAM's PARTY_GROUP subclasses. That confirms CAM's list is a legitimate Central European archival tradition. It also confirms the list is a national classification of producers, which an international package should carry as a vocabulary that national layers can extend, not as its class tree.

Two distinctions have real international precedent for special treatment. The first is **fictitious and non-human beings**. RiC excludes "legendary, mythical or fictitious figures, characters or beings" from Agent and places them under Thing ([RiC-O 1.1](https://raw.githubusercontent.com/ICA-EGAD/RiC-O/master/ontology/current-version/RiC-O_1-1.rdf)). GND codes gods, spirits and literary characters separately, and LC-PCC policy describes fictitious and non-human entities as agents ([LC-PCC PS](https://access.rdatoolkit.org/PolicyStatement/PolicyById/0306f66f-686f-4bf1-ab90-8b0d0cd8356d)). GND types even an animal, Knut the polar bear, as an ordinary differentiated person ([lobid GND](https://lobid.org/gnd/search?q=Knut+Eisb%C3%A4r&format=json&size=3)). The archival and library worlds therefore disagree here. The second is the **jurisdiction or territorial body**, which every source treats as a place plus a governing agent, or as a flagged corporate heading. None treats it as an ordinary sector type.

| CAM subclass | What it really is | International carrier |
|---|---|---|
| REGION | territory acting as a body | Place + agent with jurisdiction relation; corporate-body type "territorial self-government" |
| PUBLIC_ADMINISTRATION | organisational kind with public-law status | corporate-body type; legal status |
| ORGANIZATION (association of organisations) | organisational kind, weak | corporate-body type "umbrella / international organisation" |
| ARMY, HEALTH_AND_EDU | function or sector | corporate-body type; function concepts |
| POLITICAL_PARTY, CHURCH, GUILD | organisational kind | corporate-body type (direct AnF equivalents) |
| COMPANY, CHARITY, CLUB | legal form | legal status (plus corporate-body type) |
| BRANCH, FAMILY_BRANCH | structural relation | subdivision relation to the parent; family type "branch" |
| FAMILY vs DYNASTY | family kind | family type |
| FICTIVE_INDIVIDUAL, PERSON_BEING, PERSON_ANIMAL, FICTIVE_DYNASTY | not agents in RiC or ISAAR | flag through EAC `otherEntityType`; `rico:Thing` on RiC export |

The answer to R-a is option (a): roots only, with type and legal form as items. Option (c) would import a national sector list and its axis confusion into an international package. It would also break the ISAAR-to-package direction, because a corporate body of unknown kind has no CAM subclass, as the repository comparison already notes ([isaar-cpf-cam-comparison.md](isaar-cpf-cam-comparison.md)). Option (b), a small international subclass set, has no standard to anchor it. The obvious candidates (fictitious, jurisdiction) are exactly the ones on which RiC and the libraries disagree. Subclasses would also freeze a type that RiC lets change over time.

## RiC defines only four non-agent families, and practice uses Place first

RiC-CM 1.0 defines **six non-record, non-agent entities: Event > Activity, Rule > Mandate, Date, and Place**, all under Thing ([RiC-CM 1.0](https://www.ica.org/app/uploads/2024/01/ric-cm-1.0_0.pdf)). It has **no Concept entity**. "Abstract concepts; cultural movements, named periods and events; named things, objects, and works" are left to Thing and to "allied communities". RiC-O adds `rico:Concept` as the superclass of `rico:Type`, so occupation, activity, place, corporate-body and legal-status types become SKOS-style concepts ([RiC-O 1.1](https://www.ica.org/standards/RiC/RiC-O_1-1.html)). Date has its own entity, but in ELZA dates are already item values (UNITDATE) and need no class. The candidates for ELZA classes are therefore Place, Event, Activity/Function, Rule/Mandate and a Concept vocabulary.

Practice ranks these clearly. The Archives nationales de France built their Garance RiC-O graph from three sets: agents, **places**, and vocabularies (activities and functions, occupations, place types, documentary forms, legal statuses). Places took "beaucoup de travail", with typing, links between places, coordinates, history notes and event lists ([Clavaud & Francart, SemWeb.Pro 2025](https://cms.semweb.pro/file/18411460/raw/SWEP25-16-clavaud-archives-garance.pdf); [Referentiels repo](https://github.com/ArchivesNationalesFR/Referentiels)). Events appear there as lists attached to places, not as a separate authority file. Matterhorn RDF (Switzerland) uses rico:Place, rico:Event and rico:Rule, but keeps functions, subjects and occupations as appellations ([docuteam Matterhorn RDF](https://docs.docuteam.ch/introduction/matterhornrdf/)). ISDF defines a full authority description for functions ([ISDF 2007](https://www.ica.org/app/uploads/2023/12/CBPS_2007_Guidelines_ISDF_First-edition_EN.pdf)), and AtoM ships a template for it ([AtoM ISDF template](https://accesstomemory.org/en/docs/2.1/user-manual/data-templates/isdf-template/)), but the notes found no evidence of wide production use. EAC-CPF 2.0 confirms the ranking from the exchange side. Place, function, occupation, mandate and legal status are term elements with an optional `@valueURI` inside the agent record, and among non-agent targets only `function` can be the target of a `relation` ([EAC-CPF 2.0 schema](https://raw.githubusercontent.com/SAA-SDT/eac-cpf-schema/master/xml-schemas/eac-cpf/eac.rng)).

Every place model the notes surveyed converges on one attribute set: names with language and dates, a type from a vocabulary, dated part-of links (TGN allows several parents), existence dates, optional approximate coordinates, and external links ([TGN](https://www.getty.edu/research/tools/vocabularies/tgn/about.html); [Linked Places Format](https://github.com/LinkedPasts/linked-places-format); [Wikidata P131](https://www.wikidata.org/wiki/Property:P131)). EAC-CPF 3.0 even provides a ready coarse type list. **`@placeTypeEncoding="EASList"` has nine values that equal the GeoNames feature classes**: administrativeDivision, area, bodyOfWater, landElevation, populatedPlace, roadOrRailroad, spot, undersea, vegetation ([EAC-CPF 3.0.0 TL](https://eac.staatsbibliothek-berlin.de/schema/v3/EAC-CPF3-TL-eng.pdf); [GeoNames codes](https://www.geonames.org/export/codes.html)). The dominant institutional pattern is hybrid: a local place record aligned to GeoNames or Wikidata. SNAC builds on GeoNames ([SNAC documentation](https://gitlab.iath.virginia.edu/snac/Documentation/commit/77f410dd970a7cca701b3e7bf93bfe4743fce6ce)), FranceArchives aligns its places to GeoNames ([data.gouv.fr](https://data.gouv.fr/es/datasets/francearchives-fr-les-lieux-des-archives)), and EHRI builds its own SKOS lists only for camps and ghettos that general gazetteers cover poorly ([EHRI vocabularies](https://portal.ehri-project.eu/vocabularies)). Concepts follow the same logic. Occupations and functions are "term + vocabulary URI", and ESCO/ISCO is the SKOS backbone for occupations ([ESCOpedia](https://esco.ec.europa.eu/en/about-esco/escopedia/escopedia/international-standard-classification-occupations-isco)).

RiC-O already provides every agent-to-entity link CAM expresses as relation parts. For places these are `hasBirthPlace`/`hasDeathPlace`, `agentHasOrHadLocation`, `isAgentAssociatedWithPlace` and `hasOrHadJurisdiction`, with `containsOrContained` between places. For concepts and activities they are `hasOrHadOccupationOfType` and `performsOrPerformed`. For events and mandates they are `isOrWasParticipantIn` and `authorizedBy` ([RiC-O 1.1](https://raw.githubusercontent.com/ICA-EGAD/RiC-O/master/ontology/current-version/RiC-O_1-1.rdf)). Each binary property has an n-ary `*Relation` class for dated relations, which is what an ELZA PT_REL part with begin and end dates is. On jurisdiction, RiC and GND agree: **the territory is a place, and the governing office is an agent linked by a dated jurisdiction relation** ([rico:hasOrHadJurisdiction](https://prefix.zazuko.com/rico:hasOrHadJurisdiction); [GND Ontology](https://d-nb.info/standards/elementset/gnd)). CAM already separates GEO_ADMIN_UNIT from the offices in PARTY_GROUP, so this costs nothing.

The answer to R-b is yes, but minimally. Declare Place and Concept, keep Event optional, and do not declare Activity/Function, Rule/Mandate or a works class in the first set. Functions start life as Concepts in a "function/activity" scheme and can be promoted to an ISDF-shaped class later. Mandates stay as text with an optional link, which is how EAC-CPF carries them. ARTWORK has no RiC class and splits between Rule, RecordResource, Place, Activity and plain Thing ([RiC-CM 1.0](https://www.ica.org/app/uploads/2024/01/ric-cm-1.0_0.pdf)), so it stays CAM-only.

## Shared codes let CAM subclasses and international roots meet in one class tree

The ELZA mechanics make the compatibility layer cheap. A class declared by several packages with the same code exists once, and assignability is set per rule set. The international package can therefore declare PERSON, DYNASTY, PARTY_GROUP, GEO, TERM and EVENT, make them assignable in its own rule set, and leave CAM's subclasses outside that rule set. In an installation with both packages, a CAM entity of class COMPANY still sits under the PARTY_GROUP root. A relation whose target is restricted to PARTY_GROUP can then reach it, because the target-class check walks the hierarchy (D8 below). CAM-to-international is a total function: subclass → root + derived CORP_TYPE value. International-to-CAM works wherever the CORP_TYPE value has a CAM counterpart, and otherwise the entity stays at the assignable root. A record of unknown kind therefore never needs a fake subclass. Item types shared by code with matching data types (the CZ_BASE set includes NM_MAIN STRING, NM_LANG ENUM, REL_ENTITY RECORD_REF, REL_BEGIN/REL_END UNITDATE, IDN_TYPE ENUM, IDN_VALUE STRING, COORD_POINT COORDINATES, HISTORY TEXT, SOURCE_LINK URI_REF, per `package-cz-base/src/rul_item_type.xml`) let an installation with both packages read one store. The international package redeclares these items and does not import them, so it stays independent of CZ_BASE.

## Proposal for the ELZA international package

This is the research proposal. The package as built (ISAAR_CPF) follows it with the changes
recorded under "Decisions" below and in [rules-customization-layer.md](rules-customization-layer.md),
section 3: the assignable person and family classes are `PERSON_INDIVIDUAL` and `FAMILY` (option D),
the own item types carry the prefix `ISAAR_`, there is no EVENT class and no OTHER_ENTITY_TYPE item.

**Classes and assignability** (international rule set):

| Code | Label (EN) | RiC / ISAAR | Assignable | Shared with CAM | Notes |
|---|---|---|---|---|---|
| PERSON | Person | rico:Person / ISAAR person | yes | yes (root) | no subclasses declared |
| DYNASTY | Family | rico:Family / ISAAR family | yes | yes (root) | kind via FAMILY_TYPE |
| PARTY_GROUP | Corporate body | rico:CorporateBody / ISAAR corporate body | yes | yes (root) | kind via CORP_TYPE + LEGAL_STATUS |
| GEO | Place | rico:Place | yes | yes (root) | CAM GEO subclasses are not members of this rule set |
| TERM | Concept | rico:Concept / skos:Concept | yes | yes (root) | schemes via CONCEPT_SCHEME |
| EVENT | Event | rico:Event | yes (optional, see D4) | yes (root) | |

None of CAM's subclasses (REGION … BRANCH, PERSON_INDIVIDUAL … PERSON_ANIMAL, FAMILY … FICTIVE_DYNASTY, GEO_*, EVENT_*, TERM_*), nor ARTWORK, belongs to the international rule set.

**New item types with vocabularies** (international package owns them; codes are proposals):

| Item | Data type | Part | Vocabulary (initial values) | Export |
|---|---|---|---|---|
| CORP_TYPE | ENUM, repeatable | PT_BODY of PARTY_GROUP | government/public administration body; territorial self-government; court/judicial body; legislative body; military or security body; political party; religious body; educational or research institution; health or social-care institution; cultural or heritage institution; business enterprise; financial institution; professional, trade or labour organisation; association or club; foundation or charitable body; international or umbrella organisation; conference or meeting; other / not determined | RiC hasOrHadCorporateBodyType; EAC otherEntityType; MARC 368 $a |
| LEGAL_STATUS | ENUM, repeatable, dated (see D2) | PARTY_GROUP | public-law body; private-law body; company with share capital; partnership; cooperative; association; foundation; religious legal person; sole trader; no legal personality; other. National layers add their own legal forms as specs | ISAAR 5.2.4; EAC legalStatus; RiC hasOrHadLegalStatus |
| FAMILY_TYPE | ENUM | PT_BODY of DYNASTY | family; dynasty / royal house; noble house / lineage; clan / tribe; branch | RiC hasFamilyType |
| OTHER_ENTITY_TYPE | ENUM | PT_BODY of PERSON, DYNASTY | fictitious character; legendary or mythical being; deity or spirit; animal; collective pseudonym | EAC otherEntityType; RiC export as rico:Thing (see D3) |
| PLACE_TYPE | ENUM | PT_BODY of GEO | the nine EAC EASList / GeoNames classes | EAC @placeType; RiC hasOrHadPlaceType |
| CONCEPT_SCHEME | ENUM | PT_BODY of TERM | occupation; function / activity; subject; other | RiC OccupationType / ActivityType; EAC occupation / function |
| EVENT_TYPE | ENUM | PT_BODY of EVENT | natural event; armed conflict; political or social event; organised event (conference, exhibition, festival); other | RiC hasEventType |

**CAM subclass → international mapping** (used by migration and conversion rules): REGION → CORP_TYPE "territorial self-government" + jurisdiction relation to a GEO; PUBLIC_ADMINISTRATION → "government/public administration body" + LEGAL_STATUS "public-law body"; ORGANIZATION → "international or umbrella organisation"; ARMY → "military or security body"; COMPANY → "business enterprise"; POLITICAL_PARTY → "political party"; CHURCH → "religious body"; HEALTH_AND_EDU → "educational or research institution" or "health or social-care institution" (ambiguous, needs editor choice); CHARITY → "foundation or charitable body"; GUILD → "professional, trade or labour organisation"; CLUB → "association or club"; BRANCH → no type, subdivision relation to the parent body; FAMILY → FAMILY_TYPE "family"; FAMILY_BRANCH → "branch" + subdivision relation; FICTIVE_INDIVIDUAL / PERSON_BEING / PERSON_ANIMAL / FICTIVE_DYNASTY → OTHER_ENTITY_TYPE values.

**Minimal non-agent classes and their parts:**

| Class | PT_NAME | PT_BODY | PT_IDENT | PT_CRE / PT_EXT | PT_REL |
|---|---|---|---|---|---|
| GEO (Place) | name, language, name type, used-from/to | PLACE_TYPE, optional fine type, COORD_POINT (WGS84, approximate), HISTORY, DESCRIPTION, SOURCE_LINK | GeoNames, Wikidata, TGN, GND, ISO 3166 country code, national register; value + match type | existence dates | is part of (dated, repeatable), preceded by / succeeded by |
| TERM (Concept) | preferred label per language, alternative labels | CONCEPT_SCHEME, scope note (DESCRIPTION) | ESCO/ISCO, Wikidata, GND, LCSH, AAT URIs with exact/close match | none | broader, related |
| EVENT | name, language | EVENT_TYPE, DESCRIPTION, SOURCE_LINK | Wikidata, GND | begin / end dates | took place at (GEO), part of (EVENT) |

**Relations from agents** (PT_REL with REL_ENTITY, relation spec, REL_BEGIN/REL_END):

| Target | Relation specs | RiC-O |
|---|---|---|
| GEO | place of birth; place of death; residence / seat; place of activity; jurisdiction; associated place | hasBirthPlace, hasDeathPlace, agentHasOrHadLocation, hasOrHadJurisdiction, isAgentAssociatedWithPlace |
| TERM | occupation (persons); function / activity; field of activity | hasOrHadOccupationOfType, performsOrPerformed (via ActivityType) |
| EVENT | participant in | isOrWasParticipantIn |
| agent | is subdivision of / has subdivision; superior / subordinate; predecessor / successor; family relations; member of; other identity; associated with | hasOrHadSubdivision, hasOrHadSubordinate, precedes / follows, family and membership relations |

**Codes shared with CAM where the meaning matches:** classes PERSON, DYNASTY, PARTY_GROUP, GEO, TERM, EVENT. Item types NM_MAIN, NM_MINOR, NM_TYPE, NM_LANG, NM_USED_FROM, NM_USED_TO, IDN_TYPE, IDN_VALUE, REL_ENTITY, REL_BEGIN, REL_END, CRE_DATE, EXT_DATE, COORD_POINT, HISTORY, BIOGRAPHY, GENEALOGY, CORP_STRUCTURE, DESCRIPTION, NOTE, SOURCE_LINK, all with the CZ_BASE data types. CORP_TYPE, LEGAL_STATUS, FAMILY_TYPE, OTHER_ENTITY_TYPE, PLACE_TYPE, CONCEPT_SCHEME and EVENT_TYPE are new. PLACE_TYPE deliberately does not reuse CAM's GEO_TYPE, because GEO_TYPE carries a Czech-specific spec set.

**Decision points raised by the research** (resolved under "Decisions" below):

| # | Decision | Options | Recommendation |
|---|---|---|---|
| D1 | Does the international rule set make the shared roots assignable while CZ_BASE keeps them read-only? | per-rule-set assignability as designed vs a generic subclass | per-rule-set assignability |
| D2 | Where are legal status and corporate-body type dated? | undated items in PT_BODY; dated via PT_EVENT of a "status change" type; a dedicated repeatable part | CORP_TYPE undated in PT_BODY; LEGAL_STATUS dated via events |
| D3 | Fictitious, legendary and animal entities | keep as PERSON/DYNASTY with OTHER_ENTITY_TYPE (EAC/LC practice); move to a non-agent class (RiC practice); exclude | keep with the flag; export flagged records as rico:Thing |
| D4 | Is EVENT in the first release? | include now; defer | include, low cost because the code is shared |
| D5 | Activity/Function and Rule/Mandate classes | Concept scheme + text now, ISDF class later; class now | Concepts and text now |
| D6 | Vocabulary governance | fixed ENUM specs; specs extendable by national addon layers; external SKOS URIs only | ENUM with layer extension, each spec carrying a SKOS URI |
| D7 | Can relation specs and enum specs be shared by code between CAM and the international package, as item types can? | share; keep separate with a mapping | verify the core behaviour first, then share where meanings match |
| D8 | Does the relation target check accept a CAM subclass when the target is the shared root? | verify in core | required for coexistence |
| D9 | Do RiC Position, Mechanism and generic Group get classes? | add; leave out | leave out; Position can be a Concept plus relation |

**D7 and D8 in ELZA's code:**
- *D8 - yes.* The target check of a RECORD_REF item (`ItemService.checkRecordRef`) expands the allowed
  classes with `findSubtreeIds`, so a target restricted to PARTY_GROUP accepts a CAM entity of class
  COMPANY. Coexistence through shared roots works without a core change.
- *D7 - yes.* Specifications, like classes, part types and item types, can be declared by several
  packages and assigned to shared item types (assignments are the union of the declarations), so
  ISAAR_CPF reuses NM_TYPE, IDN_TYPE and relation specifications of CAM by declaring the same codes.

## Decisions

Facts from ELZA's code and CZ_BASE that bound the decisions:
- **Vocabularies stay per rule set.** A specification is IMPOSSIBLE unless an `AVAILABLE_ITEMS` rule
  raises it, and CAM's rules raise the specifications of `NM_TYPE` (and most others) by explicit code
  lists per class (e.g. `available_items/DYNASTY/PT_NAME.drl`). Specifications that ISAAR_CPF adds to a
  shared item type are therefore not offered in CAM scopes, and CAM's are offered in ISAAR scopes only if
  ISAAR's rules raise them. Exceptions: CAM raises all specifications of `NM_LANG` (`PT_NAME.drl`) and of
  one body item for ARTWORK. These values do not leak into CAM scopes either, because a rule set
  sees only the specifications assigned by packages related to its own (its dependencies and
  dependents); ISAAR_CPF and CZ_BASE are not related.
- **Dated facts in CAM are event parts:** `PT_EVENT` with `EV_TYPE` (specification), `EV_BEGIN`,
  `EV_END` and further items; this is how CAM records dated jobs, memberships and changes.
- **Classes, part types, item types and specifications can all be shared by code**; a relation
  target restricted to a shared root accepts CAM subclasses (D8).

### A. Settled by the evidence

| # | Decision | Arguments | Resolution |
|---|---|---|---|
| D1 | Assignable classes | Assignability is per rule set (class default, overridden by the membership of a rule set); CAM keeps its roots read-only, ISAAR_CPF makes its classes assignable in its rule set. No change for CAM users. | Assignable in ISAAR_CPF: PARTY_GROUP and, by option D (section C), PERSON_INDIVIDUAL and FAMILY |
| D5 | Activity/Function and Rule/Mandate classes | EAC-CPF exchanges function, occupation, mandate as terms with an optional URI, not as entities; AnF Garance keeps activities as SKOS vocabularies; no evidence of wide ISDF use; a concept (TERM) can later be promoted to a class without changing stored relations' target class (TERM stays the target). | Concepts (TERM, scheme "function/activity", "occupation") and text items now; classes later on demand |
| D7 | Shared specifications | Specifications can be declared by several packages (above). | Reuse by code |
| D8 | Relation target vs subclasses | Verified in code (`findSubtreeIds`). | No core change needed |
| D9 | RiC Position, Mechanism, Group | No ISAAR element, no EAC-CPF entity type, no CAM class; Position can be a concept plus a relation. | Leave out |

### B. Decided on the arguments

1. **R-a option (a): roots only, kind as data - accepted.** *For:* every archival standard and
   profile; the kind and legal status change over time (ISAAR 5.2.4 "covering dates", RiC
   "hasOrHad"); an ISAAR record of unknown kind needs no fake subclass; CAM is unaffected because its
   subclasses stay members of CAM only and map to root + type on export. *Against:* users of both
   frameworks see two ways to classify a corporate body (CAM subclass vs ISAAR type). *Mitigation:*
   the mapping CAM subclass → corporate-body type value is defined once (implementation guide,
   chapter "International entity description (ISAAR_CPF)") and can be used for search across both.
2. **First non-agent set: GEO and TERM; no EVENT class.** *For GEO, TERM:* places are the one real
   non-agent authority in practice (AnF, SNAC, FranceArchives), concepts carry occupations and
   functions (D5); both codes are shared with CAM, so an ISAAR person can relate to a CAM place in a
   shared installation. *EVENT:* no ISAAR element needs event entities, RiC Event is optional, AnF
   keeps events as lists on places; dated facts of an agent are event *parts* (item 3). An EVENT class
   is cheap to add later (shared code).
3. **D2 + D4 - dated facts live in `PT_EVENT` parts, current values undated in PT_BODY.** ISAAR asks
   for covering dates of legal status (5.2.4), functions (5.2.5), occupations and places. The current
   corporate-body type and legal status are undated items in PT_BODY; dated changes are `PT_EVENT`
   parts, sharing CAM's part type and `EV_TYPE`, `EV_BEGIN`, `EV_END` (CAM's mechanism for dated jobs
   and memberships; ISAAR declares only the event types it needs, e.g. "legal status", "function",
   "occupation", "residence"). An EAC-CPF export maps such parts to dated `legalStatus`, `function`,
   `occupation`, `place` elements. PT_EVENT is in the ISAAR part list.
4. **D3 - fictitious, legendary and animal entities stay CAM-only.** *For keeping them as
   PERSON/DYNASTY with a flag:* CAM has them under the shared PERSON and DYNASTY roots, so they stay
   one class tree in a shared installation; EAC-CPF `otherEntityType` and LC-PCC practice carry them
   as agents. *For RiC's Thing:* RiC excludes them from Agent. CAM needs them, an ISAAR archive rarely
   does: ISAAR_CPF does not offer them and has no OTHER_ENTITY_TYPE item; an ISAAR archive that needs
   fictional persons lists FICTIVE_INDIVIDUAL as a member (same code, no migration).
5. **D6 - vocabularies are ENUM specifications, extendable by layers.** With the per-rule-set offer
   (facts above) the international vocabularies are specifications on shared item types where the
   meaning matches, and national layers (addon packages) add values without affecting other rule
   sets. An external URI (SKOS, GeoNames, Wikidata) per specification has no column; it is needed for
   EAC-CPF/RiC export (`@valueURI`) and is added as a `uri` attribute of specification declarations
   when the EAC-CPF export is built.
6. **Reuse of CAM item types and specifications where the meaning matches, new codes only for
   gaps.** Reused:
   - `NM_TYPE` with the name forms ISAAR needs (5.1.3-5.1.5): `NT_EQUIV`, `NT_TRANSLATED`,
     `NT_OTHERRULES`, `NT_ALIAS`, `NT_PSEUDONYM`, `NT_ACRONYM`, `NT_FORMER`; the authorized form is
     the preferred name part (no specification needed);
   - `NM_LANG` with CAM's language code form `LNG_*` (ISO 639-2 bibliographic code), so both
     frameworks share a language by code;
   - `IDN_TYPE`/`IDN_VALUE` with `VIAF`, `LCNAF`, `AUT`, `INTERPI`-like authority identifiers and
     ISAAR-specific ones (e.g. ISNI, Wikidata) added by ISAAR;
   - `REL_ENTITY` with a subset of CAM's relation specifications mapped to ISAAR 5.3.2 categories
     (hierarchical, temporal, family, associative) - the category is derived from the
     specification, not a separate item.
   *For:* one store, one search, values exchangeable between frameworks in a shared installation.
   *Against:* ISAAR depends on CAM's code names (Czech-origin codes such as `NT_*`), though not on
   the package.
7. **Place type: a new item type, not CAM's GEO_TYPE.** GEO_TYPE carries CAM's fine-grained Czech
   set; the EAC-CPF/GeoNames list is coarse (9 values). Sharing the item type would mix two levels
   in one field. CAM's GEO_TYPE values map to the coarse classes for export.

### C. Persons, beings and the shared root

**The problem.** CAM's root PERSON means "osoba / bytost" (person or being): subclasses
PERSON_INDIVIDUAL (fyzická osoba), FICTIVE_INDIVIDUAL, PERSON_BEING, PERSON_ANIMAL; the root is
read-only. ISAAR(CPF), EAC-CPF and RiC mean by "person" an individual human being - CAM's
PERSON_INDIVIDUAL, not CAM's root. DYNASTY ("rod / rodina") has the same gap in a smaller form;
PARTY_GROUP has none. CAM's logic must not change, and the user of an ISAAR scope should see
"Person" as a root class, not CAM's grouping with a subclass. Facts that bound the options: a class
has one parent agreed by all declarations; assignability and membership are per rule set; CAM's
relation specifications accept the roots (`register-type="PERSON"`, `"DYNASTY"`), so humans of
either class are valid relation targets; the CAM exchange accepts only CAM's assignable subclasses.

| | C - grouping visible | **D - members only, presented per rule set** | E - ISAAR uses the root |
|---|---|---|---|
| ISAAR declares | PERSON (member, not assignable, "Person or being"), PERSON_INDIVIDUAL (member, assignable, "Person") | PERSON (declared as the parent only, not a member), PERSON_INDIVIDUAL (member, assignable, "Person") | PERSON (member, assignable, "Person") |
| Class of a human | PERSON_INDIVIDUAL in both frameworks | PERSON_INDIVIDUAL in both frameworks | PERSON in ISAAR scopes, PERSON_INDIVIDUAL in CAM scopes |
| ISAAR scope tree | Person or being → Person (two levels) | Person (root) | Person (root) |
| CAM scope tree | unchanged | unchanged | unchanged |
| Meaning of the shared code | one meaning per code | one meaning per code | PERSON = "person or being" in CAM, "human" in ISAAR |
| Moving an entity between frameworks' scopes | no class change | no class change | class change (translation) |
| CAM exchange of an ISAAR person | works (CAM subclass) | works | needs translation PERSON → PERSON_INDIVIDUAL |
| Search by class over both frameworks | humans = PERSON_INDIVIDUAL | same | humans split over two classes; subtree of PERSON mixes in beings and animals |
| Names by language in a shared installation | consistent | consistent | a Czech reader sees ISAAR persons as "osoba / bytost" (CAM's name of the root) |
| Core change | none | small: the tree of a rule set shows its members only, a member whose ancestors are not members is a root (the hierarchy stops at the nearest member ancestor); the global tree (no scope) is the union of the trees of all entity rule sets, a rule set without members contributing all classes | none |
| Cosmetic cost | grouping visible to ISAAR users | ISAAR's assignable classes carry CAM-origin codes (PERSON_INDIVIDUAL, FAMILY) behind English names; PERSON and DYNASTY declared only as parents | ISAAR's codes read naturally |

**Rejected paths.** *Own root code for ISAAR* (no shared person class): loses relations across
frameworks. *Class equivalence / aliases* (two codes, one class): a new core concept touching every
lookup by code, for a presentation problem that D solves in one method. *Changing CAM's tree*:
excluded. *C*: shows ISAAR users a grouping class that is not part of their standard. *E*: a human
is then two classes, and the move of an entity between a CAM and an ISAAR scope inside one
installation and the CAM exchange of ISAAR persons need a class translation.

**Decision: D.** ISAAR shows Person, Family, Corporate body (and Place, Concept) as flat roots, CAM
and a shared installation show CAM's subclasses in CAM scopes - with one class per human and no
translation between frameworks within one installation. Showing non-member ancestors as
non-selectable nodes was a choice no package needed; a rule set that wants a visible grouping lists
it as a non-assignable member.

**Consequences.** In ISAAR_CPF the assignable classes are PERSON_INDIVIDUAL ("Person"), FAMILY
("Family"), PARTY_GROUP ("Corporate body"); PERSON and DYNASTY are declared as parents and not
offered. FICTIVE_INDIVIDUAL, PERSON_BEING, PERSON_ANIMAL, FAMILY_BRANCH, FICTIVE_DYNASTY stay
CAM-only (item 4).

### Open questions

- A `uri` attribute on specification declarations and the derivation of the ISAAR 5.3.2 relation
  category from the relation specification, both for the EAC-CPF/RiC export.
- Whether an EVENT class, Activity/Function or Rule/Mandate classes are needed once real data
  demands them.

## Conclusion

The research turns a question that looks like a choice of subclass list into a modelling correction. CAM's subclasses are three or four different kinds of fact (legal form, organisational kind, function, structure) packed into one immutable class slot. Every international model unpacks them into dated, vocabulary-backed data and relations. An international package that follows them is more faithful to the standards and also more expressive than CAM, because it can record a change of legal form and an entity of unknown kind, and it lets national layers add legal forms without new classes.

The non-agent side is less settled. RiC itself has no Concept entity, and the only large production graph (AnF Garance) treats places as a real authority and everything else as vocabularies. Starting with Place and Concept follows that evidence and leaves Activity, Mandate and Event to grow when real data demands them. The risks in ELZA's core - whether specifications can be shared across packages and whether target-class checks respect the shared root hierarchy (D7, D8) - are both answered in the affirmative.

## Source notes

The cited findings behind this summary, with links to the sources, are in
[research/entity-frameworks](research/entity-frameworks):
[archival standards](research/entity-frameworks/archival-standards-agent-types.md),
[national and library authority typologies](research/entity-frameworks/national-authority-typologies.md),
[place and concept authorities](research/entity-frameworks/place-concept-authorities.md),
[RiC non-agent entities](research/entity-frameworks/ric-non-agent-entities.md).
