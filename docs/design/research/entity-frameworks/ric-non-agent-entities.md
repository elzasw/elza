# RiC non-agent entities (Place, Event, Activity, Rule/Mandate, Date, Thing/Concept) and their relations to agents

Primary sources used (all fetched/parsed 2026-10-08):
- RiC-CM 1.0 (ICA/EGAD, November 2023), PDF: https://www.ica.org/app/uploads/2024/01/ric-cm-1.0_0.pdf (full text extracted and read for sections 2.2, 4.x)
- RiC-O 1.1 (released 2025-05-22) HTML doc: https://www.ica.org/standards/RiC/RiC-O_1-1.html ; RDF parsed from https://raw.githubusercontent.com/ICA-EGAD/RiC-O/master/ontology/current-version/RiC-O_1-1.rdf (class hierarchy, domains/ranges and RiC-CM correspondences below come from a direct parse of this file)
- EAC-CPF 2.0 RelaxNG schema (SAA TS-EAS + Staatsbibliothek zu Berlin): https://raw.githubusercontent.com/SAA-SDT/eac-cpf-schema/master/xml-schemas/eac-cpf/eac.rng (parsed)
- ISDF 1st ed. (2007): https://www.ica.org/app/uploads/2023/12/CBPS_2007_Guidelines_ISDF_First-edition_EN.pdf ; ISDIAH 1st ed. (2008): https://www.archives.go.jp/thinktank/pdf/ISDIAH_EN.pdf (both text-extracted)

## 1. RiC-CM 1.0: non-agent entities, hierarchy, attributes, relations to agents

### Takeaway
RiC-CM 1.0 defines exactly six non-record, non-agent entity families: Event (E14) > Activity (E15), Rule (E16) > Mandate (E17), Date (E18, with E19-E21 subtypes) and Place (E22), all under Thing (E01). There is NO Concept entity in RiC-CM; "concepts, named things, works, movements, periods" are left to Thing and to "allied communities". Occupation, place type, event type, activity type are *attributes* (A-numbers), not entities.

### Cited Findings
- Entity hierarchy table "RiC Entities Hierarchy": RiC-E01 Thing at the root; second level RiC-E02 Record Resource, E06 Instantiation, E07 Agent, E14 Event, E16 Rule, E18 Date, E22 Place; Activity E15 under Event, Mandate E17 under Rule; Agent subtypes E08 Person, E09 Group (E10 Family, E11 Corporate Body), E12 Position, E13 Mechanism. — [RiC-CM 1.0, p.~18](https://www.ica.org/app/uploads/2024/01/ric-cm-1.0_0.pdf)
- RiC-CM version 1.0 is dated November 2023 (released with RiC-FAD and RiC-O 1.0). — [RiC-CM 1.0](https://www.ica.org/app/uploads/2024/01/ric-cm-1.0_0.pdf); [ICA release note](https://www.ica.org/release-of-the-first-three-parts-of-the-records-in-contexts-archival-description-standard-version-1-0-ric-fad-ric-cm-and-ric-o/)
- Four "core" entities are Record Resource, Instantiation, Agent and Activity. — [RiC-CM 1.0 §2.1](https://www.ica.org/app/uploads/2024/01/ric-cm-1.0_0.pdf)
- **Thing (E01)**: "Any idea, material thing, or event within the realm of human experience". Scope note: examples of entities NOT explicitly addressed in RiC-CM: "abstract concepts; cultural movements, named periods and events; named things, objects, and works; legendary, mythical or fictitious figures"; examples: Airplane [material thing], Impressionism [art movement], Puck [character], Renaissance [art period], Slavery [concept], French Revolution [event]. Such entities "are commonly the responsibility of allied cultural heritage communities ... RiC-CM is designed to be extensible to allow the incorporation of other such entities." — [RiC-CM 1.0 §2.2.1](https://www.ica.org/app/uploads/2024/01/ric-cm-1.0_0.pdf)
- **Event (E14)**: "Something that happens or occurs in time and space." May be caused by nature, an agent or both; events have temporal and spatial boundaries, can have events as parts, multiple agents may participate in different roles. Examples: Eruption of Mount Vesuvius (79 CE), San Francisco Earthquake (1906), Women's March (2017), Second World War, Arno River Flood (1966). Compared with LODE Event. — [RiC-CM 1.0 §2.2.4](https://www.ica.org/app/uploads/2024/01/ric-cm-1.0_0.pdf)
- **Activity (E15)**: "The doing of something for an agent designed purpose"; kind of Event; "In a corporate or government context an activity may also be called a 'function'"; may be composed of other activities. Examples: Marketing, Research and development, Writing poetry, Describing archives. RiC-CM notes it is appropriate to describe corporate bodies, positions, mechanisms functionally, "this is not the case for all of the activities of persons and families". — [RiC-CM 1.0 §2.2.4](https://www.ica.org/app/uploads/2024/01/ric-cm-1.0_0.pdf)
- **Rule (E16)**: "Conditions that govern the existence, responsibility, or authority of an agent; or the performance of an activity by an agent; or that contribute to the distinct characteristics of things created or managed by an agent." Written rules include constitutions, legislation, statutes, charters, regulations, policies, procedures, standards; "Rule should not be confused with the one or more documentary sources that serve as evidence of its identity. A documentary source is a record." Examples: Constitución Española 1978, Fuero de Guadalajara 1219, The Manorial Documents Rules 1959. — [RiC-CM 1.0 §2.2.5](https://www.ica.org/app/uploads/2024/01/ric-cm-1.0_0.pdf)
- **Mandate (E17)**: "Delegation of responsibility or authority by an agent to another agent to perform an activity"; kind of Rule; commonly limits the place (jurisdiction) and the time period. Examples: "Authority of a Mayor of New York City to grant building authorisations"; mandate of Australian Hearing Services per Act 1991 s7. — [RiC-CM 1.0 §2.2.5](https://www.ica.org/app/uploads/2024/01/ric-cm-1.0_0.pdf)
- **Date (E18)**: "Chronological information associated with an entity that contributes to its identification and contextualization"; treated as an entity "because chronological description is inherently complex"; natural language and/or ISO 8601 / EDTF. Attributes: General Description A43, Identifier A22, Name A28, Date Qualifier A13, Date Type A42, Expressed Date A19, Normalized Date A29. — [RiC-CM 1.0 §2.2.6, §4.7](https://www.ica.org/app/uploads/2024/01/ric-cm-1.0_0.pdf)
- **Place (E22)**: "Bounded, named geographic area or region." "A place may be a jurisdiction, a man-made structure, or a natural feature. A man-made structure or natural feature may also be a jurisdiction." Places are historical entities with beginning/ending dates and changing boundaries; may be referenced to geographic coordinates. Intro text: "Agents come into existence in a particular place, and subsequently ... may be associated with one or more places. Further, the authority of an agent to perform an activity may be constrained by place, a mandated jurisdiction." Examples: Nova Scotia, North Lanarkshire District. — [RiC-CM 1.0 §2.2.7](https://www.ica.org/app/uploads/2024/01/ric-cm-1.0_0.pdf)
- Attribute sets (RiC-CM §4.5–4.8): — [RiC-CM 1.0 §4](https://www.ica.org/app/uploads/2024/01/ric-cm-1.0_0.pdf)
  - Event: A43 General Description, A22 Identifier, A28 Name, A18 Event Type, A21 History
  - Activity: same + A02 Activity Type
  - Rule: A43, A22, A28, A21 History, A45 Rule Type
  - Mandate: same + A44 Mandate Type
  - Place: A43, A22, A28 Name, A11 Coordinates, A21 History, A27 Location, A32 Place Type
- Occupation Type is attribute RiC-A30 (of Person), not an entity. — [RiC-CM 1.0 §3, §4](https://www.ica.org/app/uploads/2024/01/ric-cm-1.0_0.pdf)

### Inferences
- In RiC-CM the "types" (place type, event type, activity type, occupation type, corporate body type, rule/mandate type) are attributes whose values come from controlled vocabularies; only Place, Event/Activity, Rule/Mandate and Date are first-class entities. For an ELZA package, this argues for a small set of entity classes plus vocabulary (term) lists — which is exactly the CAM split between GEO/EVENT entities and TERM entities.
- RiC-CM explicitly delegates "works, named things, movements, periods, concepts" to Thing/allied communities, so CAM ARTWORK has no RiC-CM entity counterpart.

### Gaps
- I did not transcribe the full RiC-CM relation table (R001–R085); relation numbers below are taken from RiC-O's RiCCMCorrespondingComponent annotations, which reference CM relation IDs.

## 2. RiC-O 1.1: classes and agent-linking properties

### Takeaway
RiC-O (current version 1.1, 2025-05-22) implements all RiC-CM entities as classes and adds rico:Concept (superclass of rico:Type, rico:Appellation, rico:Language) — the type attributes become classes (PlaceType, EventType, ActivityType, OccupationType, CorporateBodyType, RuleType, MandateType, DemographicGroup, LegalStatus) under rico:Type ⊂ rico:Concept. Place has a rich sub-model (PlaceName, PhysicalLocation, Coordinates, containment/adjacency/overlap). Agent↔Place, Person↔birth/death place, Agent→Activity, Mandate→Agent, Event participation are all direct object properties.

### Cited Findings
Class hierarchy (direct parse of RiC-O_1-1.rdf, with RiCCMCorrespondingComponent) — [RiC-O 1.1 RDF](https://raw.githubusercontent.com/ICA-EGAD/RiC-O/master/ontology/current-version/RiC-O_1-1.rdf); [RiC-O 1.1 doc](https://www.ica.org/standards/RiC/RiC-O_1-1.html):
- rico:Thing (= RiC-E01) root.
- rico:Place ⊂ Thing (= E22). Definition "Bounded, named geographic area or region." Scope: jurisdiction; jurisdictions, man-made structures, natural features are historical entities.
- rico:PlaceName ⊂ rico:Name ⊂ rico:Appellation ⊂ rico:Concept ("specialization of RiC-A28").
- rico:PhysicalLocation ⊂ Thing ("Class implementation of RiC-A27 Location"): "A delimitation of the physical territory of a Place" — address, cadastral reference, less precise info; links to rico:Coordinates (⊂ Thing; implementation of A11) with datatype props latitude, longitude, altitude, geodesicSystem, altimetricSystem. Also simple datatype props rico:geographicalCoordinates and rico:location on Place.
- rico:PlaceType ⊂ rico:Type (= A32). Scope: "Broadly, a Place may be a member of three broad categories: jurisdiction, manmade structure, or a natural feature. Each ... can be subdivided."
- rico:Event ⊂ Thing (= E14); rico:EventType ⊂ Type (= A18).
- rico:Activity ⊂ rico:Event (= E15), definition in RiC-O: "The doing of something for some human purpose"; rico:ActivityType ⊂ Type (= A02).
- rico:OccupationType ⊂ rico:ActivityType AND ⊂ rico:DemographicGroup (= A30). Definition: "Categorization of a profession, trade, or craft pursued by a Person in fulfilment of an Activity." Scope: "Should not be confused with Position where, for example, an agent with the occupation type 'lawyer' holds the position ..."
- rico:Rule ⊂ Thing (= E16); rico:Mandate ⊂ rico:Rule (= E17); rico:RuleType (= A45), rico:MandateType ⊂ RuleType (= A44).
- rico:Date ⊂ Thing (= E18) (SingleDate/DateRange/DateSet subclasses).
- rico:Concept ⊂ Thing — "An idea, unit of thought, abstract cultural object or category" — NO RiC-CM correspondence (RiC-O addition). Subclasses: rico:Type, rico:Appellation (Name, Identifier, PlaceName...), rico:Language.
- rico:Type ⊂ Concept: "A superclass for any type of some thing." Subclasses include CorporateBodyType (A12), DemographicGroup (A15), LegalStatus (A26), PlaceType, EventType, ActivityType, RuleType, etc.
- rico:Position ⊂ rico:Agent (E12) — relevant because CAM "function/office" may be either a Position (agent) or an OccupationType (concept).

Agent ↔ Place properties (domain → range; CM relation):
- rico:hasOrHadLocation Thing → Place (RiC-R075i) / isOrWasLocationOf; specialised rico:agentHasOrHadLocation Agent → Place / isOrWasLocationOfAgent.
- rico:isAssociatedWithPlace Thing → Place (R074i); specialised rico:isAgentAssociatedWithPlace Agent → Place / isPlaceAssociatedWithAgent. n-ary: rico:PlaceRelation (R074).
- rico:hasBirthPlace / hasDeathPlace Person → Place (inverse isBirthPlaceOf / isDeathPlaceOf) — no CM number (RiC-O shortcut).
- rico:hasOrHadJurisdiction Agent → Place (R076i) / isOrWasJurisdictionOf.
- Place↔Place: containsOrContained / isOrWasContainedBy (R007), directlyContains, transitive variants, isOrWasAdjacentTo (R077), overlapsOrOverlapped (R078).
- Place → PlaceType: hasOrHadPlaceType; Place → PlaceName: hasOrHadPlaceName; Place → PhysicalLocation: hasOrHadPhysicalLocation; PhysicalLocation → Coordinates: hasOrHadCoordinates.
- rico:history datatype property applies to Agent | Event | Instantiation | Place | RecordResource | Rule (= A21).

Agent ↔ Activity / Occupation:
- rico:performsOrPerformed Agent → Activity (R060i) / isOrWasPerformedBy (R060); n-ary rico:PerformanceRelation ⊂ EventRelation.
- rico:hasActivityType Activity → ActivityType.
- rico:hasOrHadOccupationOfType Person → OccupationType / isOrWasOccupationTypeOf.
- rico:hasOrHadDemographicGroup (Group|Person) → DemographicGroup.
- rico:hasOrHadCorporateBodyType CorporateBody → CorporateBodyType.
- rico:occupiesOrOccupied Person → Position (R054); hasOrHadPosition Group → Position (R056i).
- Provenance: rico:hasOrganicOrFunctionalProvenance RecordResource|Instantiation → Activity|Agent; rico:documents RecordResource → Activity (R033).

Agent ↔ Rule / Mandate:
- rico:authorizes Mandate → Agent (R067) / rico:authorizedBy Agent → Mandate (R067i); n-ary rico:MandateRelation ⊂ RuleRelation (R067); activityIsContextOfRelation Activity → (AgentTemporalRelation|MandateRelation).
- rico:issuedBy Rule → Agent (R065) / isResponsibleForIssuing; rico:isOrWasEnforcedBy Rule → Agent (R066); rico:regulatesOrRegulated Rule → Thing (R063); rico:isAssociatedWithRule Thing → Rule (R062i); rico:isOrWasExpressedBy Rule → RecordResource (R064).

Agent ↔ Event / Date:
- rico:isOrWasParticipantIn Thing → Event (R058i) / hasOrHadParticipant; rico:isAssociatedWithEvent Thing → Event (R057i), n-ary rico:EventRelation (R057); rico:isOrWasAffectedBy Thing → Event (R059i); rico:resultsOrResultedFrom Thing → Event (R061i).
- Event structure: hasOrHadSubevent (R006) and transitive/direct variants; hasEventType; occurredAtDate Event → Date (R084i).

### Inferences
- RiC-O already provides the exact "relation part" patterns CAM uses: place of birth/death (hasBirthPlace/hasDeathPlace), place of activity/seat (agentHasOrHadLocation), general association (isAgentAssociatedWithPlace), jurisdiction, occupation (hasOrHadOccupationOfType → a SKOS-like concept), function/activity (performsOrPerformed), mandate (authorizedBy), event participation (isOrWasParticipantIn). Each binary property has an n-ary *Relation class for dated/qualified relations — analogous to CAM relation parts with dates.
- Because OccupationType/PlaceType/ActivityType are rico:Type ⊂ rico:Concept, a "TERM" class in ELZA maps cleanly to rico:Concept/rico:Type (with SKOS for the vocabulary), whereas GEO maps to rico:Place and EVENT to rico:Event.

### Gaps
- RiC-O 1.1 release date (2025-05-22) comes from the WebFetch summary of the HTML doc; I did not independently confirm against the GitHub release page.

## 3. ISDF and ISDIAH: entity descriptions defined and practical use

### Takeaway
ISDF (2007) defines a full authority-like description for a *function* (incl. subfunction, business process, activity, task, transaction) with its own identity/context/relationships/control areas and relations to corporate bodies and archival materials — i.e. it is the ICA precursor of RiC Activity. ISDIAH (2008) describes *institutions with archival holdings* (repositories) — it is an agent (corporate body) description specialised for repositories, not a new entity class. In practice ISDF is implemented as a separate entity in AtoM and Matterhorn RDF, but rarely populated; ISDIAH is used for repository directories.

### Cited Findings
- ISDF scope: "The term 'function' is used throughout this standard to include not only function but also any of the subdivisions of a function such as subfunction, business process, activity, task, transaction" (ISDF 1.2); functions "are generally ... more stable than administrative structures" and suited as basis for arrangement, appraisal and retrieval (1.3). — [ISDF 2007](https://www.ica.org/app/uploads/2023/12/CBPS_2007_Guidelines_ISDF_First-edition_EN.pdf)
- ISDF elements: 5.1 Identity (5.1.1 Type, 5.1.2 Authorised form(s) of name, 5.1.3 Parallel, 5.1.4 Other forms, 5.1.5 Classification); 5.2 Context (5.2.1 Dates, 5.2.2 Description, 5.2.3 History, 5.2.4 Legislation); 5.3 Relationships to other functions (5.3.1 name/identifier of related function, 5.3.2 Type, 5.3.3 Category of relationship, 5.3.4 Description, 5.3.5 Dates); 5.4 Control (9 elements); chapter 6 "Relating functions to corporate bodies, archival materials and other resources" (6.1 identifier/name of related resource, 6.2 Nature of relationship, 6.3 Dates). — [ISDF 2007](https://www.ica.org/app/uploads/2023/12/CBPS_2007_Guidelines_ISDF_First-edition_EN.pdf)
- ISDF defines 23 elements in four areas and is a companion standard to ISAAR(CPF) and ISAD(G). — [SAA Standards Committee page on ISDF](https://www2.archivists.org/groups/standards-committee/international-standard-for-describing-functions-isdf)
- ISDIAH elements: Identity (5.1.1 Identifier, names, 5.1.5 Type of institution), Contact (5.2.1 Location and address(es), telephone, contact persons), Description (5.3.1 History, 5.3.2 Geographical and cultural context, 5.3.3 Mandates/Sources of authority, 5.3.4 Administrative structure, 5.3.5 Records management and collecting policies, 5.3.6 Building(s), 5.3.7 Holdings, 5.3.8 Finding aids), Access, Services, Control; ch. 6 links to archival material (6.1–6.3) and to related authority records (6.4). — [ISDIAH 2008](https://www.archives.go.jp/thinktank/pdf/ISDIAH_EN.pdf)
- AtoM provides ISDF and ISDIAH data entry templates (function type taxonomy: function, subfunction, etc.). — [AtoM ISDF template](https://accesstomemory.org/en/docs/2.1/user-manual/data-templates/isdf-template/); [AtoM ISDIAH template](https://www.accesstomemory.org/docs/2.2/user-manual/data-templates/isdiah-template/)
- Matterhorn RDF (docuteam / Valais State Archives, used with Swiss Federal Archives) "covers the three standards ISAD(G), ISAAR(CPF) and ISDF" and combines RiC-O, PREMIS 3 and EBUCore. — [Matterhorn RDF on Zenodo](https://zenodo.org/records/5984492); [docuteam Matterhorn RDF docs](https://docs.docuteam.ch/introduction/matterhornrdf/)

### Inferences
- ISDF's element set (type, names, classification, dates, description, history, legislation, function-to-function relations with category, relations to corporate bodies) is a ready template for an optional "Activity/Function" class; "Legislation" is the bridge to Rule/Mandate.
- ISDIAH does not justify a new entity class for ELZA: it is a corporate body with extra attributes (contact, opening hours, building). Building(s) is a potential link to Place (man-made structure).

### Gaps
- No reliable source found quantifying how many archives actually maintain ISDF function authorities in production; anecdotally rare, but unverified. Did not verify whether Archives Portal Europe's EAG profile is formally ISDIAH-based (commonly stated, not checked here).

## 4. EAC-CPF 2.0: place, function, occupation, mandate, legalStatus — text with links, not separate entities

### Takeaway
In EAC-CPF 2.0, place/function/occupation/mandate/legalStatus are descriptive elements *inside* the CPF record, carrying text (term/placeName) plus optional `@vocabularySource`, `@vocabularySourceURI`, `@valueURI` pointing to an external vocabulary/authority. They are not relations to separately described entities. Formal relations (`relation/targetEntity/@targetType`) only allow agent, corporateBody, person, family, function, resource — no place, event or concept.

### Cited Findings
(from direct parse of the EAC-CPF 2.0 RelaxNG schema — [eac.rng](https://raw.githubusercontent.com/SAA-SDT/eac-cpf-schema/master/xml-schemas/eac-cpf/eac.rng))
- `places` → `place*` + descriptiveNote. `place` attributes: @localType, @vocabularySource, @vocabularySourceURI, @valueURI (+ common @id, @audience, @languageOfElement, @sourceReference, @maintenanceEventReference, @conventionDeclarationReference). Content: placeName, placeRole, geographicCoordinates, address, contact, date/dateRange/dateSet, descriptiveNote.
- `placeName` has @vocabularySource, @vocabularySourceURI, @valueURI, @countryCode; `placeRole` has @vocabularySource/@vocabularySourceURI/@valueURI (i.e. role such as birth place is itself vocabulary-controlled).
- `function`, `occupation`, `mandate`, `legalStatus` each have identical structure: @localType, @vocabularySource, @vocabularySourceURI, @valueURI; content term, date/dateRange/dateSet, placeName, descriptiveNote. Wrapper elements `functions`, `occupations`, `mandates`, `legalStatuses`.
- `term` itself has no vocabulary attributes in 2.0 (they moved to the parent element).
- `relation` content: targetEntity, date/dateRange/dateSet, place, relationType, targetRole, descriptiveNote, objectXMLWrap. `targetEntity/@targetType` values: agent | corporateBody | person | family | function | resource; plus @valueURI/@vocabularySource.
- Schema header names publisher "Society of American Archivists in collaboration with the Staatsbibliothek zu Berlin" and tag library at https://eac.staatsbibliothek-berlin.de/schemata-and-tag-library/. — [eac.rng](https://raw.githubusercontent.com/SAA-SDT/eac-cpf-schema/master/xml-schemas/eac-cpf/eac.rng)

### Inferences
- EAC-CPF 2.0 treats places, occupations, mandates, legal status as qualified attributes with an optional URI to an external authority (e.g. GeoNames, Wikidata, a national place register, or a CAM GEO entity URI). An ELZA package exporting to EAC-CPF can therefore emit a CAM GEO/TERM link as `placeName@valueURI` / `occupation@valueURI` + label — no need for EAC to "know" the target class.
- Only "function" can be a relation target in EAC-CPF 2.0 — consistent with ISDF being the only ICA non-agent authority standard. Events and places as related entities are only expressible via RiC-O.

### Gaps
- Did not reach the Berlin tag library HTML (URL tried returned 404); semantics of placeRole recommended values not checked.

## 5. Practice: who implements places/activities/concepts as authority entities

### Takeaway
The clearest production example is the Archives nationales de France "Référentiels" → Garance knowledge graph (RiC-O 1.1): agents, places (as rico:Place with types, hierarchy, coordinates, history notes, event lists) and SKOS/RiC-O vocabularies (activities/functions, occupations, place types, documentary forms, legal statuses). Matterhorn RDF (Switzerland) uses rico:Place, rico:Event and rico:Rule but keeps functions/subjects/occupations as appellations/categories. Minimal practical set: Agent + Place (+ hierarchy, types, coordinates) + controlled vocabularies (as concepts) for occupation/activity/place type/legal status; Events/Rules only secondarily.

### Cited Findings
- ANF objective: "Générer un unique graphe de connaissances RiC-O 1.1 à partir des données de référence produites et maintenues depuis des décennies par les AnF sur les entités de contexte"; work started 2021, first GitHub release May 2022, new phase 2023; source data were XML files "souvent des listes de noms ... pas de liens explicites entre lieux". — [Clavaud & Francart, SemWeb.Pro 2025 slides](https://cms.semweb.pro/file/18411460/raw/SWEP25-16-clavaud-archives-garance.pdf)
- ANF changes: "Vocabulaires : conversion en SKOS/RiC-O, avec métadonnées et nouveaux concepts"; "Agents : 255 notices dédoublonnées"; "Lieux : beaucoup de travail ! circonscriptions administratives françaises et voies avec les données IGN, INSEE et Mairie de Paris ; descriptions enrichies manuellement : typage des lieux, liens entre lieux, coordonnées géographiques, notes historiques, listes d'événements". ~70,000 RDF/XML files → three JSON-LD frames: Agents, Lieux, Vocabulaires; static site with ~70,000 HTML pages, Sparnatural search; roadmap end 2025–2026: add concepts to vocabularies, update French administrative districts, finish Paris streets, add location links for buildings ("édifices"). — [same slides](https://cms.semweb.pro/file/18411460/raw/SWEP25-16-clavaud-archives-garance.pdf)
- ANF Referentiels repository: agents (RDF and EAC-CPF), places (RiC-O + SKOS), vocabularies in SKOS (activities/functions, occupations/professions, place types, documentary forms, legal statuses); version 1.1 (June 2022), v2.0 announced; Garance web app and SPARQL endpoint https://sparql.archives-nationales.culture.gouv.fr/garance/sparql. — [GitHub ArchivesNationalesFR/Referentiels](https://github.com/ArchivesNationalesFR/Referentiels) (summary via WebFetch)
- Garance place pages define place as defined/named geographic area, with types such as municipalities, streets, buildings, foreign territories, lighthouses, canals; dataset described as "incomplete but coherent", built progressively as records are described. — [Garance places page](https://rdf.archives-nationales.culture.gouv.fr/garance/pages/en/places)
- RiC-O Converter (open source, ANF) converts EAD finding aids and EAC-CPF to RiC-O RDF. — [Chartes/PSL page](https://chartes.psl.eu/recherche/centre-jean-mabillon/publications-des-chercheurs/ric-o-converter-software-convert-eac-0)
- Matterhorn RDF: places as rico:Place via rico:PlaceRelation (geographic names, place of publication); events as rico:Event (acquisition, appraisal, processing); functions, subjects, keywords, occupations as rico:Appellation with categories; governance via rico:Rule/RuleRelation; no separate Mandate entities documented. — [docuteam Matterhorn RDF docs](https://docs.docuteam.ch/introduction/matterhornrdf/) (summary via WebFetch; treat details as indicative)

### Inferences
- Pattern seen at ANF: Place is the first non-agent class worth making a real entity (hierarchy, types, coordinates, history, dated names) — the same priority as CAM GEO. Activities and occupations are kept as SKOS concept schemes (vocabularies), not full descriptions — matches CAM TERM.
- Events appear mainly as lists attached to places/agents, not as a big authority file.

### Gaps
- Spanish/Portuguese RiC pilots, Finnish (AHAA) and Archives Portal Europe implementations: searches returned nothing reliable within budget — not covered.
- Counts of places/concepts in Garance not obtained.

## 6. CAM classes vs RiC: semantic match

### Takeaway
GEO ≈ rico:Place (incl. man-made structures) with subclasses expressed as rico:PlaceType values; EVENT ≈ rico:Event (Activity for organised/purposeful events); TERM ≈ rico:Concept/rico:Type (SKOS). ARTWORK has no RiC class: it splits between rico:Rule (laws, charters), rico:RecordResource (well-known documents), rico:Place (buildings, routes), rico:Activity (projects/programmes) and plain rico:Thing (works, products, brands, games, awards).

### Cited Findings
- RiC-CM Place covers "a jurisdiction, a man-made structure, or a natural feature"; RiC-O PlaceType gives those three broad categories subdividable further. — [RiC-CM 1.0 §2.2.7](https://www.ica.org/app/uploads/2024/01/ric-cm-1.0_0.pdf); [RiC-O 1.1](https://www.ica.org/standards/RiC/RiC-O_1-1.html)
- RiC Event may be caused by nature, agent or both (earthquake, flood, war, march) — [RiC-CM 1.0 §2.2.4](https://www.ica.org/app/uploads/2024/01/ric-cm-1.0_0.pdf)
- RiC Rule examples include constitutions and charters (Fuero de Guadalajara 1219), and Rule is distinct from the documentary source (a record). — [RiC-CM 1.0 §2.2.5](https://www.ica.org/app/uploads/2024/01/ric-cm-1.0_0.pdf)
- Works, named things/objects, movements, periods, fictitious beings are explicitly "not addressed" by RiC-CM and fall under Thing. — [RiC-CM 1.0 §2.2.1](https://www.ica.org/app/uploads/2024/01/ric-cm-1.0_0.pdf)
- rico:Concept is a RiC-O addition (no CM counterpart) — superclass of Type, Appellation, Language. — [RiC-O 1.1 RDF](https://raw.githubusercontent.com/ICA-EGAD/RiC-O/master/ontology/current-version/RiC-O_1-1.rdf)

### Inferences (proposed mapping for ELZA design; my analysis, not sourced)
| CAM class / subclass | RiC target | Notes |
|---|---|---|
| GEO – administrative territory | rico:Place, PlaceType=jurisdiction | hierarchy via containsOrContained; agent jurisdiction via hasOrHadJurisdiction |
| GEO – natural areas, land/sea formations, waters | rico:Place, PlaceType=natural feature | |
| GEO – other localities, named shapes | rico:Place | named shapes maybe PlaceType "other" |
| GEO – climatic phenomena | mismatch: closer to rico:Event (if dated) or rico:Concept | not "bounded geographic area" |
| GEO – space (cosmic objects) | rico:Place stretches; arguably rico:Thing | RiC Place is "geographic" |
| EVENT – organized events | rico:Event (rico:Activity if purposeful agent action) | participation via isOrWasParticipantIn |
| EVENT – traditions/holidays | rico:EventType or rico:Concept (recurring, not single occurrence) | RiC Event is a bounded occurrence |
| EVENT – temporary natural phenomena | rico:Event | RiC examples: floods, earthquakes |
| EVENT – military events | rico:Event with subevents | Second World War is a RiC example |
| ARTWORK – laws/charters/well-known documents | rico:Rule (laws, charters) or rico:RecordResource (document) | |
| ARTWORK – constructions/buildings/routes | rico:Place (man-made structure) | ANF types buildings, streets, canals as places |
| ARTWORK – projects/programmes | rico:Activity | |
| ARTWORK – works, products/brands, games, awards | rico:Thing (outside RiC; LRMoo/CIDOC-CRM territory) | awards possibly Concept |
| TERM – general concepts, taxonomy terms | rico:Concept / rico:Type subclasses (OccupationType, ActivityType, PlaceType, CorporateBodyType, LegalStatus, DemographicGroup) + SKOS | |
| (CAM function/office) | rico:Position (agent) vs rico:OccupationType (concept) vs rico:Activity | RiC warns not to confuse occupation and position |

- Suggested priority for an international package: (1) Place (names with dates, place type, parent place, coordinates, identifiers/external URIs, history) — highest reuse and most CAM links (birth/death place, seat, jurisdiction); (2) Concept/Term schemes for OccupationType, ActivityType, PlaceType, CorporateBodyType, LegalStatus (vocabulary-level, can be SKOS); (3) Activity/Function (ISDF-shaped) for corporate-body functions; (4) Event; (5) Rule/Mandate (often sufficient as text + valueURI like EAC-CPF mandate). ARTWORK should stay CAM-specific (compatibility layer only), mapped to rico:Thing on export.
- Rationale: matches EAC-CPF 2.0 (place/occupation/function/mandate/legalStatus as vocab-linked elements), ANF Garance priorities (agents, places, vocabularies), and RiC-O property set.

### Gaps
- No published crosswalk CAM ↔ RiC-O was found (not searched in Czech sources within this budget).
