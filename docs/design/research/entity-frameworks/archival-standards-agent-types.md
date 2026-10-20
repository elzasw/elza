# How archival standards and their profiles subtype agents (corporate bodies, persons, families)

Research date: 2026-10-08. Versions checked: RiC-O 1.1 (2025-05-22, aligned with RiC-CM 1.0), EAC-CPF 2.0 schema (GitHub master) and EAC-CPF 3.0.0 Tag Library (2026 edition), ISAAR(CPF) 2nd ed. (2004), apeEAC-CPF schema (APE), Archives nationales de France (AnF) Référentiels (GitHub, 2026).

Main finding across all sources: **no archival standard subclasses Corporate Body / Person / Family any further as a class.** The kind of corporate body is always recorded as data: a category term (RiC `CorporateBodyType`, AnF "catégories des collectivités"), a legal status (ISAAR 5.2.4, EAC-CPF `<legalStatus>`, RiC `LegalStatus`), or functions/occupations. The one production-scale profile found with a controlled corporate-body typology (AnF, 64 concepts) uses a SKOS vocabulary linked by `rico:hasOrHadCorporateBodyType` in RDF and carried in `<legalStatus>/<term>` in EAC-CPF.

## 1. RiC-CM 1.0 / RiC-O 1.x: the Agent hierarchy and how kinds of agents are expressed

### Takeaway
RiC has the class tree Agent → {Person, Group → {Family, CorporateBody}, Position, Mechanism}. Below Corporate Body and Family it uses **Type entities** (`rico:CorporateBodyType`, `rico:FamilyType`, `rico:LegalStatus`, `rico:DemographicGroup`, `rico:OccupationType`), not subclasses. These are linked by properties such as `rico:hasOrHadCorporateBodyType`. RiC provides no vocabulary of types. Fictitious or legendary beings are explicitly *not* RiC agents.

### Cited Findings
- RiC-O current version is "Version 1.1 - 2025-05-22", versionIRI `https://www.ica.org/standards/RiC/ontology/1.1`; many change notes read "Aligned with RiC-CM 1.0" — [RiC-O 1.1 RDF, ICA-EGAD GitHub](https://raw.githubusercontent.com/ICA-EGAD/RiC-O/master/ontology/current-version/RiC-O_1-1.rdf); HTML docs at [ica.org/standards/RiC/ontology](https://www.ica.org/standards/RiC/ontology)
- **rico:Agent** (RiC-E07): "A Person, or Group, or an entity created by a Person or Group (Mechanism), or a Position, that acts in the world." The scope note says an Agent may have alternative identities (pseudonyms, heteronyms, DBA, trade identities). These must not be confused with a Position — [RiC-O 1.1](https://raw.githubusercontent.com/ICA-EGAD/RiC-O/master/ontology/current-version/RiC-O_1-1.rdf)
- **rico:Person** (RiC-E08), subClassOf Agent, disjoint with Position: "An individual human being." The scope note discusses personae: an alternative persona may eclipse the original one (Mark Twain/Samuel Clemens), and "A persona shared by two or more persons constitutes a kind of group" — [RiC-O 1.1](https://raw.githubusercontent.com/ICA-EGAD/RiC-O/master/ontology/current-version/RiC-O_1-1.rdf)
- **rico:Group** (RiC-E09), subClassOf Agent, disjoint with Person, Position and Mechanism: "Two or more Agents that act together as an Agent." Scope note: "Corporate bodies and families are kinds of groups, though other kinds of groups are possible. For example, the 'electorate'… Complex, large groups may be subdivided into other groups." — [RiC-O 1.1](https://raw.githubusercontent.com/ICA-EGAD/RiC-O/master/ontology/current-version/RiC-O_1-1.rdf)
- **rico:Family** (RiC-E10), subClassOf Group: "Two or more persons related by birth, or through marriage, adoption, civil union, or other social conventions that bind them together as a socially recognized familial group." Scope note: "'Family' is used here as a general term that encompasses a wide variety of familial groups. Other types of familial groups include Dynasty, Clan, House, Tribe and others… In some context, a Family may be legally recognized as Corporate Body" (example: North American tribes) — [RiC-O 1.1](https://raw.githubusercontent.com/ICA-EGAD/RiC-O/master/ontology/current-version/RiC-O_1-1.rdf)
- **rico:CorporateBody** (RiC-E11), subClassOf Group: "An organized group of persons that act together as an Agent, and that has a recognized legal or social status." Scope note: a sole trader may count as a corporate body in some legal contexts. Corporate bodies "often have a mandate" and act within a jurisdiction, but "may be constituted in a more informal manner and exist as an entity by virtue of its recognition as such by its members" — [RiC-O 1.1](https://raw.githubusercontent.com/ICA-EGAD/RiC-O/master/ontology/current-version/RiC-O_1-1.rdf)
- **rico:Position** (RiC-E12), subClassOf Agent: "The functional role of a Person within a Group." It is "the intersection of a Person and a Group" and "exists independently of the Person that holds the Position" — [RiC-O 1.1](https://raw.githubusercontent.com/ICA-EGAD/RiC-O/master/ontology/current-version/RiC-O_1-1.rdf)
- **rico:Mechanism**, subClassOf Agent, disjoint with Person and Position: "A process or system created by a Person or Group that performs an Activity" (software agents, robots, probes) — [RiC-O 1.1](https://raw.githubusercontent.com/ICA-EGAD/RiC-O/master/ontology/current-version/RiC-O_1-1.rdf)
- **rico:CorporateBodyType** (subClassOf rico:Type; corresponds to RiC-A12 Corporate Body Type attribute): "Categorization of a Corporate Body." **rico:hasOrHadCorporateBodyType** (subPropertyOf rico:hasOrHadType; domain CorporateBody, range CorporateBodyType; inverse `isOrWasCorporateBodyTypeOf`): "Connects a Corporate Body to a Corporate Body Type which categorizes or categorized it." The "hasOrHad" form means the type can change over time — [RiC-O 1.1](https://raw.githubusercontent.com/ICA-EGAD/RiC-O/master/ontology/current-version/RiC-O_1-1.rdf)
- **rico:FamilyType** (RiC-A20): "Categorization of a Family." Scope: "Encompasses a wide variety of familial groups related by consanguinity, affinity, cohabitation, or other social conventions." **rico:hasFamilyType** (domain Family, range FamilyType; inverse `isFamilyTypeOf`) — note: not "hasOrHad" — [RiC-O 1.1](https://raw.githubusercontent.com/ICA-EGAD/RiC-O/master/ontology/current-version/RiC-O_1-1.rdf)
- **rico:LegalStatus** (RiC-A26), subClassOf Type: "A status defined by law." **rico:hasOrHadLegalStatus**: domain is the union of Agent and Record Resource, range LegalStatus: "Connects an Agent or Record Resource to a Legal Status which categorized or categorizes it." In RiC, legal status is therefore not limited to corporate bodies — [RiC-O 1.1](https://raw.githubusercontent.com/ICA-EGAD/RiC-O/master/ontology/current-version/RiC-O_1-1.rdf)
- **rico:DemographicGroup** (RiC-A15): "Categorization of a Person or Group based on shared characteristics." Scope: "Can be extended with any number of specific subclasses… gender, (biological) sex, education, identity, place, ancestry, ethnic/cultural identification, and religion." **rico:hasOrHadDemographicGroup**: domain Person or Group — [RiC-O 1.1](https://raw.githubusercontent.com/ICA-EGAD/RiC-O/master/ontology/current-version/RiC-O_1-1.rdf)
- **rico:OccupationType** (RiC-A30), subClassOf both ActivityType and DemographicGroup: "Categorization of a profession, trade, or craft pursued by a Person in fulfilment of an Activity." Linked by **rico:hasOrHadOccupationOfType** (domain Person; subPropertyOf hasOrHadDemographicGroup) — [RiC-O 1.1](https://raw.githubusercontent.com/ICA-EGAD/RiC-O/master/ontology/current-version/RiC-O_1-1.rdf)
- Subdivisions are relations, not types: **rico:hasOrHadSubdivision** (domain/range Group): "Connects a Group to one of its present or past subdivisions." It has the inverse `isOrWasSubdivisionOf` and the n-ary class `rico:GroupSubdivisionRelation`. Separately, **rico:hasOrHadSubordinate** (Agent→Agent) means "hierarchically inferior" — [RiC-O 1.1](https://raw.githubusercontent.com/ICA-EGAD/RiC-O/master/ontology/current-version/RiC-O_1-1.rdf)
- The **rico:Thing** (RiC-E01) scope note lists, among "entities not explicitly addressed in RiC… legendary, mythical or fictitious figures, characters or beings" — [RiC-O 1.1](https://raw.githubusercontent.com/ICA-EGAD/RiC-O/master/ontology/current-version/RiC-O_1-1.rdf)

### Inferences
- The RiC pattern for CAM's corporate-body subclasses is: one class `rico:CorporateBody`, plus `rico:hasOrHadCorporateBodyType` → a SKOS-like concept for REGION, PUBLIC_ADMINISTRATION, COMPANY and so on. Because the property is time-qualified ("hasOrHad"), RiC also allows the type to change during an entity's existence. CAM's fixed class cannot do this.
- RiC has three distinct "kind" axes for corporate bodies: CorporateBodyType (what kind of organisation), LegalStatus (legal form such as joint-stock company or public institution), and Activity/Function (what it does). The CAM subclasses mix all three axes.
- RiC's own scope note names Dynasty, Clan, House and Tribe as "types of familial groups", which are FamilyType values. CAM's FAMILY and DYNASTY map onto this naturally.
- A CAM BRANCH (organisational unit) and FAMILY_BRANCH map in RiC to a Group linked by `hasOrHadSubdivision`, not to a type.
- Under RiC, CAM's FICTIVE_INDIVIDUAL, PERSON_BEING and FICTIVE_DYNASTY would be `rico:Thing` (subjects), not Agents. PERSON_ANIMAL has no RiC home either: Person = "individual human being". An animal acting via a trained mechanism is not a Mechanism.

### Gaps
- RiC-CM 1.0 PDF (attribute texts for RiC-A12/A20/A26) was not read directly. The attribute numbers come from the `RiCCMCorrespondingComponent` annotations in RiC-O.
- No ICA-published vocabulary of CorporateBodyType values exists, as far as I found. RiC leaves the values to implementers.

## 2. EAC-CPF 1.x / 2.0 / 3.0: entityType, localType, otherEntityType, legalStatus

### Takeaway
EAC-CPF fixes `entityType` to exactly three values (corporateBody, person, family) in every version. From 2.0 it adds `<otherEntityTypes>/<otherEntityType>` with `<term>` and `@valueURI`/`@vocabularySource`. This is the explicit slot for "a specification of the default entity types", i.e. subtypes. `<legalStatus>` is a term-based element for corporate bodies. No vocabulary for either is prescribed. `@localType` should be a URI or label from a formal vocabulary (e.g. SKOS) declared in `<localTypeDeclaration>`.

### Cited Findings
- EAC-CPF 2.0 RNG: `entityType` has a required attribute `@value` with values `corporateBody | person | family` — [EAC-CPF 2.0 schema (SAA-SDT GitHub)](https://raw.githubusercontent.com/SAA-SDT/eac-cpf-schema/master/xml-schemas/eac-cpf/eac.rng). In 1.x the value was element text, e.g. `<entityType>corporateBody</entityType>`, as seen in AnF EAC-CPF files — [AnF FRAN_NP_000001.xml](https://raw.githubusercontent.com/ArchivesNationalesFR/Referentiels/master/agents/eac-cpf/FRAN_NP_000001.xml)
- In the EAC-CPF 2.0 schema, `<identity>` = `entityType`, `nameEntry|nameEntrySet`+, optional **`otherEntityTypes`**, `identityId`*, `descriptiveNote`? . `otherEntityType` contains `term` (1..n), an optional date, `placeName`* and `descriptiveNote`?, plus `@localType`, `@vocabularySource`, `@vocabularySourceURI` and `@valueURI` — [EAC-CPF 2.0 RNG](https://raw.githubusercontent.com/SAA-SDT/eac-cpf-schema/master/xml-schemas/eac-cpf/eac.rng)
- EAC-CPF 3.0.0 Tag Library (2026 Edition) on `<entityType>`: "A required child element of <identity> encoding the type of CPF entity being described… values limited to: corporateBody, family, person… Use <otherEntityType> to encode additional or alternative entity types, such as a translation or a specification of the default entity types." — [EAC-CPF 3.0.0 Tag Library](https://eac.staatsbibliothek-berlin.de/schema/v3/EAC-CPF3-TL-eng.html)
- On `<otherEntityType>`: "used to encode any additional or alternative entity types… in addition to the required <entityType>… For example, in a local context 'organization' might be used as an entity type instead of 'corporateBody'… must include one <term>." It is repeatable and can carry a date or place — [EAC-CPF 3.0.0 TL](https://eac.staatsbibliothek-berlin.de/schema/v3/EAC-CPF3-TL-eng.html)
- On `<legalStatus>`: "encode information about the legal status of a corporate body… typically defined and granted by authorities… Enter terms in accordance with provisions of the controlling legislation. Terms may be drawn from controlled vocabularies or may be natural language terms." It needs at least one `<term>` and allows a date, place and note. It is grouped in `<legalStatuses>` within `<description>` — [EAC-CPF 3.0.0 TL](https://eac.staatsbibliothek-berlin.de/schema/v3/EAC-CPF3-TL-eng.html)
- On `@localType`: "narrow the semantics of an element… it is recommended that the value be either the URI or the preferred label for a term defined in a formal vocabulary (e.g., SKOS)… Local conventions or controlled vocabularies… may be declared in <localTypeDeclaration> within <control>." `@localTypeDeclarationReference` "should always be used alongside @localType" — [EAC-CPF 3.0.0 TL](https://eac.staatsbibliothek-berlin.de/schema/v3/EAC-CPF3-TL-eng.html)
- The 3.0.0 TL states it supersedes 2.0.1 and aligns with RiC-CM "where feasible" — [EAC-CPF 3.0.0 TL](https://eac.staatsbibliothek-berlin.de/schema/v3/EAC-CPF3-TL-eng.html); 2.0.1 TL: [eac.staatsbibliothek-berlin.de/schema/v2/eac.html](https://eac.staatsbibliothek-berlin.de/schema/v2/eac.html)
- The 3.0.0 TL also defines `@identityType` (values "acquired"/"given") on `<identity>`, to distinguish a person from personae in `<multipleIdentities>` — [EAC-CPF 3.0.0 TL](https://eac.staatsbibliothek-berlin.de/schema/v3/EAC-CPF3-TL-eng.html)

### Inferences
- For export of a CAM-style subtype, the most standard EAC-CPF 2.0/3.0 encoding is `<entityType value="corporateBody"/>` plus `<otherEntityTypes><otherEntityType><term>…</term></otherEntityType></otherEntityTypes>`, with `@valueURI` pointing to a published SKOS concept. A legal form (e.g. joint-stock company) belongs in `<legalStatus>`.
- EAC-CPF 1.x has no `otherEntityType`. Implementers there used `<legalStatus>` (AnF, see §4), `<localDescription>`, or `@localType` on `<entityType>`/`<identity>`.
- `@identityType="acquired"` (persona) is the EAC mechanism closest to pseudonymous or literary personae. It is not a "fictitious being" type.

### Gaps
- The 1.x Tag Library (2010/2018) was not opened, so the exact 1.x wording of `<legalStatus>` and `@localType` is not quoted.
- I found no SAA-endorsed controlled list for `otherEntityType` or `legalStatus` values.

## 3. ISAAR(CPF) 2nd ed.: type of entity, legal status, functions

### Takeaway
ISAAR has exactly three entity types (5.1.1) and no subtypes. The "kind" of a corporate body is explicitly recorded in **5.2.4 Legal status**, whose rule says to record "the legal status and where appropriate the type of corporate body", with dates. Activities go in 5.2.5, the legal basis in 5.2.6, and internal structure or family genealogy in 5.2.7. Under ISAAR, type is a dated descriptive element, not a class.

### Cited Findings
- 5.1.1 Type of entity: "Purpose: To indicate whether the entity being described is a corporate body, person or family. Rule: Specify the type of entity (corporate body, person or family) that is being described in this authority record." The examples are national-language labels: "Institución", "Ente", "Famille", "Entidade coletiva". 5.1.1 is one of the four essential elements (§4.7) — [ISAAR(CPF) 2nd ed. (EN PDF)](https://www.archives.go.jp/thinktank/pdf/ISAAR_2nd_EN.pdf); ICA copy: [ica.org](https://www.ica.org/app/uploads/2023/12/CBPS_Guidelines_ISAAR_Second-edition_EN.pdf)
- 5.2.4 Legal status: "Purpose: To indicate the legal status of a corporate body. Rule: Record the legal status and where appropriate the type of corporate body together with the covering dates when this status applied." Examples: "Organismo de la Administración Central del Estado 1516 (probable)/1834" (Simancas), "Department of State" (NAA), "Órgão público do Executivo Federal, da administração direta" (Brazil), "Ufficio governativo di antico regime" (Florence) — [ISAAR(CPF) 2nd ed.](https://www.archives.go.jp/thinktank/pdf/ISAAR_2nd_EN.pdf)
- 5.2.5 Functions, occupations and activities: "Record the functions, occupations and activities performed by the entity… together with the covering dates when useful." 5.2.6 Mandates/Sources of authority: "Record any document, law, directive or charter which acts as a source of authority…" — [ISAAR(CPF) 2nd ed.](https://www.archives.go.jp/thinktank/pdf/ISAAR_2nd_EN.pdf)
- 5.2.7 Internal structures/Genealogy: "describe… the internal administrative structure(s) of a corporate body or the genealogy of a family." Footnote: for persons, genealogy goes in History (5.2.2) and/or Relationships (5.3) — [ISAAR(CPF) 2nd ed.](https://www.archives.go.jp/thinktank/pdf/ISAAR_2nd_EN.pdf)
- Glossary: "Corporate body. An organization or group of persons that is identified by a particular name and that acts, or may act, as an entity. Also includes an individual acting in a corporate capacity." The glossary has no definition of Person or Family — [ISAAR(CPF) 2nd ed.](https://www.archives.go.jp/thinktank/pdf/ISAAR_2nd_EN.pdf)
- Appendix B family example (Noel family) describes a family branch only in narrative History ("founded the branch of the Noel family from which the Earls of Gainsborough descended"). The French Dampierre example likewise mentions "branche aînée" and "branche cadette" in text. Branches are not a separate entity type — [ISAAR(CPF) 2nd ed.](https://www.archives.go.jp/thinktank/pdf/ISAAR_2nd_EN.pdf)
- Appendix A maps 1st edition 2.1.5 "Legal status" → 5.2.4, and 2.1.6 "Mandate, functions, sphere of activity" → 5.2.5 + 5.2.6 — [ISAAR(CPF) 2nd ed.](https://www.archives.go.jp/thinktank/pdf/ISAAR_2nd_EN.pdf)

### Inferences
- ISAAR treats "type of corporate body" as part of a dated legal-status statement. In ISAAR terms, CAM's PARTY_GROUP subclasses are 5.2.4 content: repeatable and time-bound, not an identity-level class.
- ISAAR 5.1.1 maps 1:1 to CAM's root classes (PARTY_GROUP=corporate body, PERSON, DYNASTY=family). CAM's roots are not assignable, but on export the root is exactly what ISAAR/EAC require.
- ISAAR does not mention fictitious persons or animals.

### Gaps
- ISAAR 1st ed. (1996) text not consulted beyond the Appendix A mapping.

## 4. Implementation profiles (SNAC, APE, AnF/FranceArchives, Australia)

### Takeaway
SNAC and APE keep only the three EAC entity types. The Archives nationales de France is the strongest precedent: a published, SKOS/RiC-O-conformant **"Catégories des collectivités" thesaurus (FRAN_RI_104, 64 concepts, mostly flat)**. It is linked from corporate bodies with `rico:hasOrHadCorporateBodyType` in RDF and with `<legalStatus><term vocabularySource="concept-id">` in EAC-CPF. Australia's CRS splits agents into Agencies, Organisations and Persons. This split is about level of autonomy, not organisational kind.

### Cited Findings
**Archives nationales de France (AnF)**
- AnF Référentiels repo: authority data published as RDF (SKOS + RiC-O) and EAC-CPF. v1.1 dates from June 2022, and v2.0 (RiC-O 1.1 conformant) is announced for mid-June 2026. The data can be browsed in "Garance" (v1 published late April 2026) and queried via SPARQL — [ArchivesNationalesFR/Referentiels README](https://github.com/ArchivesNationalesFR/Referentiels)
- Concept vocabularies include `FRAN_RI_104_corporateBodyTypes`, `FRAN_RI_010_occupationTypes` and `FRAN_RI_011_activityTypes`. Agents are split into folders `collectivites`, `familles` and `personnes-physiques` (= the three ISAAR types) — [Referentiels repo tree](https://github.com/ArchivesNationalesFR/Referentiels/tree/master/concepts)
- FRAN_RI_104 ConceptScheme "Catégories des collectivités": "Thésaurus des catégories des collectivités… conforme à SKOS et RiC-O 1.1… Nombre de concepts : 64." Each concept is typed with RiC-O class `CorporateBodyType`. It is mostly flat, with only 9 `skos:broader` links: e.g. several EP subtypes → "établissement public"; "juridiction d'exception ou d'attribution" → "juridiction"; "comité d'assemblée parlementaire" → "assemblée parlementaire" — [FRAN_RI_104 RDF](https://raw.githubusercontent.com/ArchivesNationalesFR/Referentiels/master/concepts/rdf/FRAN_RI_104_corporateBodyTypes.rdf); [CSV](https://raw.githubusercontent.com/ArchivesNationalesFR/Referentiels/master/concepts/csv/FRAN_RI_104_corporateBodyTypes.csv)
- Sample FRAN_RI_104 concepts:
  - Public administration: ministère, cabinet ministériel, service d'administration centrale, service déconcentré de l'État, service à compétence nationale, autorité indépendante, organisme consultatif, comité interministériel, présidence de la République, assemblée parlementaire
  - Territories: collectivité territoriale ("collectivité locale"), corps de ville ou communauté d'habitants
  - Courts: juridiction, cour supérieure ou souveraine, commission juridictionnelle
  - Public establishments: établissement public, EPA, EPIC, EPSCP, EPST, établissement public de santé, groupement d'intérêt public
  - Companies: société anonyme, société civile, société commerciale, SCOP, société d'économie mixte, société d'État, groupement d'intérêt économique
  - Associations and professions: association (defined as "Groupement de personnes régi par la loi du 1er juillet 1901"), fondation, syndicat professionnel, ordre professionnel, corporation ou communauté de métier
  - Political and religious: parti politique, établissement religieux, congrégation religieuse ou ordre militaire, administration ou juridiction religieuse séculière
  - Security and international: organisation paramilitaire, service actif de police, organisation intergouvernementale
  - Research and other: laboratoire de recherche, UMR, établissement d'enseignement, hôpital ou organisme d'assistance publique, office notarial, société secrète, institution de prévoyance, organisme de sécurité sociale
  - Ancien Régime-specific: organe gouvernemental de la monarchie, autorité seigneuriale laïque, assemblée représentative, organisme parapublic
  - Fallback: "non identifié"
  — [FRAN_RI_104 CSV](https://raw.githubusercontent.com/ArchivesNationalesFR/Referentiels/master/concepts/csv/FRAN_RI_104_corporateBodyTypes.csv)
- Encoding in AnF EAC-CPF (1.x): `<entityType>corporateBody</entityType>` … `<legalStatuses><legalStatus><term vocabularySource="d5blolsbn6--1v8dkhwwncgqp">présidence de la république</term>`. The corporate-body *category* is carried in `legalStatus` with the concept id — [FRAN_NP_000001.xml](https://raw.githubusercontent.com/ArchivesNationalesFR/Referentiels/master/agents/eac-cpf/FRAN_NP_000001.xml); same pattern in [FRAN_NP_000010.xml](https://raw.githubusercontent.com/ArchivesNationalesFR/Referentiels/master/agents/eac-cpf/FRAN_NP_000010.xml) ("cabinet ministériel")
- Encoding in AnF RDF: `rdf:type rico:CorporateBody` + `rico:hasOrHadCorporateBodyType <corporateBodyType/FRAN_RI_104-…>` — [FRAN_Agent_000001.rdf](https://raw.githubusercontent.com/ArchivesNationalesFR/Referentiels/master/agents/rdf/collectivites/FRAN_Agent_000001.rdf)
- AnF's converter (RiC-O Converter) turned about 15,000 EAC-CPF and 29,000 EAD files into a RiC-O graph — [ENC/Chartes summary](https://chartes.psl.eu/recherche/centre-jean-mabillon/publications-des-chercheurs/ric-o-converter-software-convert-eac-0); repo [ArchivesNationalesFR/rico-converter](https://github.com/ArchivesNationalesFR/rico-converter)

**Archives Portal Europe (apeEAC-CPF)**
- The apeEAC-CPF XSD restricts `entityType` to `person | corporateBody | family`. It has no other-entity-type element. `legalStatus`, `occupation` and `function` take `@localType` of type `xs:anyURI` and are grouped in `legalStatuses`, `occupations` and `functions` — [apeEAC-CPF.xsd (ape-dpt GitHub)](https://raw.githubusercontent.com/ArchivesPortalEuropeFoundation/ape-dpt/master/DPTutils/src/main/resources/apeEAC-CPF.xsd); profile maintenance repo [ArchivesPortalEuropeFoundation/apeEAC-CPF](https://github.com/ArchivesPortalEuropeFoundation/apeEAC-CPF) (WGoS change process, wiki link)

**SNAC**
- SNAC uses the three EAC-CPF entity types. Its training glossary defines a corporate body as an organization or group "identified by a name and that acts, or may act, as a unit", which can also cover "an institutional position held by a person". It defines a family as two or more people related through marriage, birth, adoption or other legal manner, "or who present themselves as a family" — [SNACSchool Training Glossary](https://portal.snaccooperative.org/system/files/media/documents/Public/SNACSchool%20Training%20Glossary_1.pdf) (quoted via search-result summary; PDF not text-extractable here); per-type cheat sheets: [Corporate Bodies](https://portal.snaccooperative.org/system/files/media/documents/Public/SNAC%20Cheat%20Sheet%204.0%20Corporate%20Bodies.pdf), [Families](https://portal.snaccooperative.org/system/files/media/documents/Public/SNAC%20Cheat%20Sheet%204.0%20Families.pdf)

**Australia (NAA Commonwealth Record Series system)**
- CRS registers **agencies** (CA numbers): "distinct, recognisable bodies that are responsible for carrying out administrative functions… can be head, state or local offices of Commonwealth Government departments, statutory authorities, courts, or tribunals". It separately registers **organisations** (CO numbers): "a whole government, learned society, church or company that is independent or broadly autonomous" (e.g. CO 1 Commonwealth of Australia). It also registers **persons** linked to agencies — [NAA: The CRS System](https://naa.gov.au/node/417) (via search summary; page fetch timed out); [CRS Manual – Person links](https://recordsearch.naa.gov.au/manual/Provenance/PersonLinks.htm)
- The NAA's ISAAR 5.2.4 example of a legal status is "Department of State" — [ISAAR(CPF) 2nd ed.](https://www.archives.go.jp/thinktank/pdf/ISAAR_2nd_EN.pdf)

### Inferences
- AnF FRAN_RI_104 is the closest international counterpart to CAM's PARTY_GROUP subclasses. It is a flat, dated, SKOS-published category list whose values are a mix of legal forms (SA, EPIC, association loi 1901) and organisational kinds (ministry, political party, court). This is the same mix CAM has, but AnF holds it as data, not classes.
- Australia's agency/organisation split looks like CAM's distinction between a unit (BRANCH, PUBLIC_ADMINISTRATION units) and an autonomous body (ORGANIZATION, REGION). In CRS this is a hierarchy-level distinction (agency within organisation), not a kind distinction.
- No profile examined (SNAC, APE, AnF, NAA) treats fictitious persons, animals or family branches as separate agent types.

### Gaps
- NAA "agency status" values (Head Office / Regional or State Office / Local Office, etc.) were not verified because naa.gov.au and recordsearch timed out. The CSIRO CRS ontology ([crs-ont](https://raw.githack.com/CSIRO-enviro-informatics/crs-ont/master/html/crs.html)) returned HTTP 429.
- PROV (Victoria) VRG/VA typology not checked.
- Archives Hub (UK) EAC-CPF practice not checked.
- SNAC: I could not confirm from primary text whether any corporate-body subtype or local-type vocabulary exists. I found no evidence of one.
- SIAF national guidance on producer typology (e.g. "personne morale de droit public/privé" as a top split) was not found as a published list. AnF's FRAN_RI_104 is an institution-level vocabulary.

## 5. Fictional persons, animals, legendary beings; family branches

### Takeaway
Archival standards do **not** treat fictional persons, animals or legendary beings as agents. RiC explicitly classes "legendary, mythical or fictitious figures, characters or beings" as plain `Thing`s, and Person is a human being. Only the library world has wavered: RDA once let fictitious and non-human personages be persons, while IFLA LRM restricts Person to real humans. No archival standard defines "family branch" as an entity type. Branches appear only as narrative, as FamilyType values (e.g. "House"), or as Group subdivisions.

### Cited Findings
- RiC-O `rico:Thing` scope note: entities "not explicitly addressed in RiC" include "legendary, mythical or fictitious figures, characters or beings" — [RiC-O 1.1](https://raw.githubusercontent.com/ICA-EGAD/RiC-O/master/ontology/current-version/RiC-O_1-1.rdf)
- RiC Person = "An individual human being." Shared personae count as "a kind of group" — [RiC-O 1.1](https://raw.githubusercontent.com/ICA-EGAD/RiC-O/master/ontology/current-version/RiC-O_1-1.rdf)
- Library context (secondary): IFLA LRM defines Person as "an individual human being", restricted to real persons, so fictional and legendary figures are not persons. LC/PCC practice nevertheless allows describing a fictitious or non-human entity as an Agent (Person/Corporate Body/Family). The RDA Fictitious Entities Working Group documented the debate around 2013–2015 — [RDA and nonhuman personages (RSC)](https://rdatoolkit.org/sites/default/files/rsc/RDA%20and%20nonhuman%20personages.pdf); [6JSC/FictitiousWG/1](https://rdatoolkit.org/sites/default/files/rsc/6JSC-FictitiousWG-1.pdf). These are search-result summaries; the PDF text could not be extracted, so treat them as unverified details.
- Family branches: RiC Family scope note lists "Dynasty, Clan, House, Tribe" as other types of familial groups. RiC Group "may be subdivided into other groups" via `rico:hasOrHadSubdivision` (Group→Group, so Family→Family is valid) — [RiC-O 1.1](https://raw.githubusercontent.com/ICA-EGAD/RiC-O/master/ontology/current-version/RiC-O_1-1.rdf). ISAAR mentions branches only in narrative examples (Noel, Dampierre) — [ISAAR(CPF) 2nd ed.](https://www.archives.go.jp/thinktank/pdf/ISAAR_2nd_EN.pdf). SNAC's definition of family includes groups "who present themselves as a family" — [SNAC glossary](https://portal.snaccooperative.org/system/files/media/documents/Public/SNACSchool%20Training%20Glossary_1.pdf)

### Inferences
- An international package aligned with RiC should model PERSON_BEING, FICTIVE_INDIVIDUAL, FICTIVE_DYNASTY and PERSON_ANIMAL as non-agent entities: subjects or `rico:Thing`, or a local extension. Otherwise they need an explicit local flag, since EAC-CPF will export them as `person`/`family` with no standard way to say "fictitious". A possible carrier is `otherEntityType` with a local term, or the `@identityType` / persona mechanism for literary personae of real people.
- FAMILY_BRANCH → `rico:Family` + `rico:hasOrHadSubdivision` from the parent family (optionally FamilyType "branch"). FAMILY vs DYNASTY → FamilyType values.

### Gaps
- I did not find the current RDA Toolkit (official, post-3R) wording on non-human personages verbatim.

## 6. Comparison of CAM subclasses with standards

| CAM class | ISAAR(CPF) | EAC-CPF | RiC-CM/RiC-O | AnF FRAN_RI_104 counterpart | Nature |
|---|---|---|---|---|---|
| PARTY_GROUP (root) | 5.1.1 corporate body | `entityType=corporateBody` | `rico:CorporateBody` | — | standard top type |
| REGION (territories with own govt.) | 5.2.4 legal status / type | `legalStatus` or `otherEntityType` | CorporateBodyType (+ Place via jurisdiction) | collectivité territoriale; corps de ville ou communauté d'habitants | org. kind / public-law status |
| PUBLIC_ADMINISTRATION | 5.2.4 (e.g. "Department of State", "Organismo de la Administración Central") | legalStatus | CorporateBodyType / LegalStatus | ministère, service d'administration centrale, service déconcentré, autorité indépendante, établissement public… | legal status (public law) + kind |
| ORGANIZATION (assoc. of organisations) | 5.2.4 | legalStatus | CorporateBodyType | organisation intergouvernementale (partly) | kind; weak counterpart |
| ARMY (military & security) | 5.2.5 functions | function / legalStatus | CorporateBodyType or Activity type | organisation paramilitaire, service actif de police, (ordre militaire) | function-based |
| COMPANY | 5.2.4 legal status | legalStatus | LegalStatus (legal form) | société anonyme/civile/commerciale/d'État, SEM, SCOP, GIE | legal form |
| POLITICAL_PARTY | 5.2.4 | legalStatus | CorporateBodyType | parti politique | kind (direct match) |
| CHURCH | 5.2.4 | legalStatus | CorporateBodyType | établissement religieux; congrégation; administration religieuse séculière | kind (direct match) |
| HEALTH_AND_EDU | 5.2.5 functions | function | CorporateBodyType / Activity type | établissement d'enseignement, EPSCP, EPST, laboratoire, UMR, établissement public de santé, hôpital | function-based |
| CHARITY (foundations) | 5.2.4 | legalStatus | LegalStatus | fondation | legal form (direct match) |
| GUILD (professional & interest orgs) | 5.2.4 | legalStatus | CorporateBodyType | ordre professionnel, syndicat professionnel, corporation ou communauté de métier | kind (direct match) |
| CLUB (associations) | 5.2.4 | legalStatus | LegalStatus | association (loi 1901) | legal form (direct match) |
| BRANCH | 5.2.7 internal structure / 5.3 hierarchical relation | `relation` (hierarchical) | `rico:hasOrHadSubdivision` / GroupSubdivisionRelation | (subdivision d'un organe … Ancien Régime only) | **relation, not type** |
| PERSON (root) | 5.1.1 person | `entityType=person` | `rico:Person` | personnes-physiques | standard top type |
| PERSON_INDIVIDUAL | person | person | Person | — | = root |
| FICTIVE_INDIVIDUAL | none | none (persona: `@identityType`) | `rico:Thing` (explicitly not Agent) | — | **no archival counterpart** |
| PERSON_BEING | none | none | `rico:Thing` | — | **no archival counterpart** |
| PERSON_ANIMAL | none | none | none (Person = human) | — | **no archival counterpart** (library LC-PCC only) |
| DYNASTY (root) | 5.1.1 family | `entityType=family` | `rico:Family` | familles | standard top type |
| FAMILY | family | family | Family (+FamilyType) | — | = root / FamilyType |
| FAMILY_BRANCH | narrative in 5.2.2/5.2.7 | relation | Family + `hasOrHadSubdivision` | — | **relation, not type** |
| FICTIVE_DYNASTY | none | none | `rico:Thing` | — | **no archival counterpart** |

### Inferences (summary)
- **Standard distinctions:** only the three roots (corporate body, person, family). RiC adds Group (generic), Position and Mechanism, which have no CAM equivalent.
- **CAM distinctions that are legal forms or statuses** (COMPANY, CHARITY, CLUB, REGION, PUBLIC_ADMINISTRATION) map to ISAAR 5.2.4, EAC `legalStatus` and RiC `LegalStatus`.
- **Organisational kinds** (POLITICAL_PARTY, CHURCH, GUILD, ORGANIZATION) map to RiC `CorporateBodyType`. AnF has direct concept equivalents for most.
- **Function-based classes** (ARMY, HEALTH_AND_EDU) map to ISAAR 5.2.5 functions, or to a CorporateBodyType.
- **Structural relations** (BRANCH, FAMILY_BRANCH) map to subdivision relations.
- **No counterpart** in archival standards: FICTIVE_INDIVIDUAL, PERSON_BEING, PERSON_ANIMAL, FICTIVE_DYNASTY.
- Exchange path for subtypes:
  - EAC-CPF 2.0/3.0 `otherEntityType` with `@valueURI` to a published SKOS scheme, and/or `legalStatus` with `@vocabularySource`/`@valueURI` (the AnF pattern).
  - RiC-O `hasOrHadCorporateBodyType` / `hasFamilyType` / `hasOrHadLegalStatus` to SKOS concepts typed as `rico:CorporateBodyType` etc.
  - Because the RiC properties are time-qualified, a type change over time is possible, which a fixed class cannot express.
