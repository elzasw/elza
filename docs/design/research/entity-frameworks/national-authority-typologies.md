# National and library authority-file typologies of corporate bodies, persons and families (vs. CAM subclasses)

Scope note: library practice (GND, RDA/MARC 21, BnF/IdRef, Wikidata) was researched in depth. Archival producer typologies were found for Poland (SEZAM). Slovakia and Austria were not found, and are listed under Gaps. Research date: 2026-10-08.

## 1. GND (DNB): entity types, ontology classes and Entitätencodes

### Takeaway
GND has the most detailed library subtyping of the agent entities. It has six top-level record types. Corporate bodies have the subtypes company, musical body, organ of a territorial authority, project, religious body, religious administrative unit, fictive body and "other". Persons have the subtypes royal, pseudonym, collective pseudonym, god, literary character, spirit and "other". Families are coded under persons. Conferences are a separate top-level type. Jurisdictions (territorial corporate bodies) are coded as *geographic* entities, not as corporate bodies. Animals are ordinary differentiated persons. GND has no subtypes for public administration, army, school/health, charity, association or political party.

### Cited Findings
- GND top-level entity types (Satztypen, PICA field 005 pos. 2): b Körperschaft, f Konferenz, g Geografikum, p Person (individualisiert), s Sachbegriff, u Werk (Stand 14.12.2023). Conferences are a separate top-level entity, distinct from corporate bodies. [DNB, Entitäten der GND (Satztypen)](https://wiki.dnb.de/download/attachments/90411323/entitaetenSatztypen.pdf?api=v2)
- Entitätencodierung, Vergaberichtlinien (Stand 03.08.2021). The PDF columns are misaligned in text extraction. The code-to-definition pairing below follows the order of the definitions. Re-check against the PDF before quoting verbatim. [DNB, Entitätencodierung](https://wiki.dnb.de/download/attachments/90411323/entitaetenCodes.pdf?api=v2)
  - **Corporate bodies (k):** kif Firmen (since Jan 2016); kim Musikalische Körperschaften (since Jan 2016); kio Organe von Gebietskörperschaften; kip Projekte und projektähnliche Vorhaben und Programme; kir Religiöse Körperschaften (since Jan 2016); kiv;kir Religiöse Verwaltungseinheiten (e.g. Catholic dioceses, always double-coded with kir); kiz all other corporate bodies; kxz Fiktive Körperschaften.
  - **Geographic/jurisdictions (g):** gik Gebietskörperschaften und Verwaltungseinheiten; gil;gik Länder, Staaten (selbstständig); gif;gik Gliedstaaten; gir;gik Geistliche Reichsfürstentümer (to 1803) and administrative units of the Eastern Church; giv;gik administrative units with an instance-type broader term. Territorial bodies are therefore Geografika carrying a corporate-body sub-code, not Körperschaften.
  - **Persons (p):** pif Familien; pik Regierende Fürsten, Mitglieder regierender Fürstenhäuser; pip Pseudonyme; pis Sammelpseudonyme; piz all other personal names; pxg Götter; pxl Literarische Gestalten, Sagengestalten; pxs Geister. Families are a person subtype in GND coding, not a top-level type.
  - **Conferences (v):** vie / vif = Konferenzen, Veranstaltungen and Konferenzfolgen/-reihen (codes v, vie, vif; the misalignment makes the exact pairing uncertain).
  - **Subjects (s):** siw "Personengruppen, die keine Körperschaft sind" (groups of persons that are not corporate bodies) and sih "Historische Einzelereignisse" are subject terms, not agents.
- An older DNB working paper on corporate-body coding proposes further codes: kib Bauwerke, kih Historische Einzelereignisse (currently recorded as corporate bodies), kis Ortsgebundene Körperschaften. It says "kiz" should remain a placeholder, and that non-place-bound bodies are "in der Regel noch nicht codiert" (usually not yet coded). Subtype coding is therefore partial and optional. [DNB wiki, Koerperschaft.pdf](https://wiki.dnb.de/download/attachments/51744106/Koerperschaft.pdf?api=v2)
- Entity codes for corporate bodies in GND records (kif, kio, kip, kir, kiz) are used with record types Tb/Tf. kiz covers bodies that belong to no special type. [search summary of DNB/ZDB GND training material](https://zeitschriftendatenbank.de/fileadmin/user_upload/ZDB/dokumente/rda/modul5/GND_Schulung2022.pdf)
- The GND Ontology gives the corporate-body subclasses as gndo:Company, FictiveCorporateBody, MusicalCorporateBody, OrganOfCorporateBody, ProjectOrProgram, ReligiousAdministrativeUnit, ReligiousCorporateBody. Person → DifferentiatedPerson, with subclasses CollectivePseudonym, Gods, LiteraryOrLegendaryCharacter, Pseudonym, RoyalOrMemberOfARoyalHouse, Spirits; plus UndifferentiatedPerson. gndo:Family; gndo:ConferenceOrEvent, with subclass SeriesOfConferenceOrEvent. PlaceOrGeographicName has subclasses incl. TerritorialCorporateBodyOrAdministrativeUnit, AdministrativeUnit, Country, MemberState, ReligiousTerritory, FictivePlace. [GND Ontology](https://d-nb.info/standards/elementset/gnd)
- Animals: Knut the polar bear (GND 1176843206) is typed Person / DifferentiatedPerson, with subject category "Personen zu Zoologie". GND has no animal class. [lobid GND search](https://lobid.org/gnd/search?q=Knut+Eisb%C3%A4r&format=json&size=3)

### Inferences
- GND's "OrganOfCorporateBody" / kio (organs of territorial authorities) roughly matches CAM PUBLIC_ADMINISTRATION. GND jurisdictions (gik), however, are places. CAM REGION (an administrative territory with its own government) matches GND's TerritorialCorporateBodyOrAdministrativeUnit, which GND places under PlaceOrGeographicName.
- CAM CHURCH corresponds to kir/kiv, and COMPANY to kif. CAM has no equivalent of kim (musical groups) or kip (projects). GND has no equivalent of ARMY, POLITICAL_PARTY, HEALTH_AND_EDU, CHARITY, GUILD, CLUB or ORGANIZATION. In GND these all fall into kiz.
- On the person side, CAM FICTIVE_INDIVIDUAL ≈ pxl, and CAM PERSON_BEING ≈ pxg + pxs (gods, spirits). CAM PERSON_ANIMAL has no GND counterpart, because animals are plain persons in GND. GND's royal and pseudonym subtypes have no CAM counterpart.
- GND has no FAMILY_BRANCH or fictive-dynasty distinction.

### Gaps
- No GND code for "Tier" (animal) was found; animals appear to receive piz (not verified on the record's 005 field).
- Whether "Geschlecht" (noble lineage) is distinguished from "Familie" in GND was not confirmed. No separate code appears in the code list.
- The exact vie/vif pairing is uncertain because of the PDF extraction.

## 2. RDA / LC-PCC / MARC 21 Authority

### Takeaway
RDA and MARC have three agent types: person, family and corporate body. The meeting/conference and the jurisdiction are *forms of corporate body*, distinguished by MARC tag (111 vs 110) and by the 110 first indicator "1 – Jurisdiction name". Finer typing (type of corporate body, type of jurisdiction, type of family) is recorded as free or controlled *attributes* (MARC 368 $a/$b, 376 $a), not as classes. LC-PCC policy describes fictitious and non-human entities as Agents (person/corporate body/family).

### Cited Findings
- MARC 21 Authority field 110 first indicator: 0 Inverted name, 1 Jurisdiction name, 2 Name in direct order. Jurisdictions are therefore corporate bodies flagged by an indicator, not a separate entity. [LC MARC 21 Authority 110](https://www.loc.gov/marc/authority/ad110.html)
- MARC 21 Authority field 368 "Other attributes of person or corporate body":
  - $a Type of corporate body (examples: "Television station", "Firm").
  - $b Type of jurisdiction (examples: "County", "Village").
  - $c Other designation (e.g. "Saint", "Spirit").
  - $d Title of person.
  - $2 Source vocabulary.
  - The field was introduced in 2011 for corporate bodies and extended to persons in 2012. Subfields $i and $4 were added in 2025. [LC MARC 21 Authority 368](https://www.loc.gov/marc/authority/ad368.html)
- LC-PCC policy statements say to describe a fictitious entity or a non-human entity as an Agent, using the entity subtypes Person, Corporate Body and Family as appropriate. A non-human entity is related to resources as a "related agent", not as the agent of creation. [RDA Toolkit LC-PCC PS (one of the linked statements)](https://access.rdatoolkit.org/PolicyStatement/PolicyById/0306f66f-686f-4bf1-ab90-8b0d0cd8356d); see also the [RDA Toolkit PS list](https://access.rdatoolkit.org/PolicyStatement/PolicyById/d4673ecb-c9ec-4bd3-b38a-e4b780771de3) (search-snippet level, exact PS id per sentence not verified).

### Inferences
- In the anglophone library world, legal or functional type (firm, school, diocese) is an attribute (368 $a) with a vocabulary source in $2, rather than an entity subclass. This supports modelling CAM's PARTY_GROUP subtypes as a typed attribute, mappable to 368 $a.
- The three distinctions that are "structural" in RDA/MARC are: jurisdiction (110 ind1=1), meeting (111) and person/family/corporate body. CAM's REGION is the closest analogue to the jurisdiction flag. CAM has no conference/meeting subclass.

### Gaps
- The MARC 376 (Family information, $a Type of family) and 111/151 scope were not fetched in this session. 376 $a "Type of family" (e.g. "Clan", "Dynasty", "Royal house") is known from training data but not verified here.
- The RDA official Toolkit (post-3R) definition of Person ("individual human being…") and its option for fictitious entities were not fetched verbatim.

## 3. BnF / IdRef (France)

### Takeaway
French library authority files type agents coarsely: person, corporate body (collectivité, including congresses), family. There is no detailed public typology of collectivités comparable to GND.

### Cited Findings
- BnF INTERMARC authority record types: PEP Personnes physiques, ORG Organismes, GEO, MAR (Marques), RAM, TIC/TUM/TUT. "Organismes" covers corporate bodies (and congresses). [BnF, INTERMARC Autorités de diffusion](https://www.bnf.fr/node/1643/)
- IdRef authority types include Personne, Collectivité (incl. congrès), Famille ("nom générique de famille"), Nom géographique, Titre uniforme, Auteur-Titre. Proposals can be made for persons, families, organizations, congresses, titles and geographic names. [RERO wiki, IdRef propositions](https://wiki.rero.ch/fr/bib/idref/propositions); [Abes](https://abes.fr/enquete-flash-correspondant-autorites-2022)

### Inferences
- France distinguishes congress vs collectivité at the level of the heading/format (as in MARC 111). It has no published functional subclass list.

### Gaps
- UNIMARC(A) 210 indicator values (meeting flag; entry under jurisdiction) and BnF practice for fictitious characters and animals were not verified. The IFLA PDF URL returned 404. BnF's [UNIMARC(A) 2018 conversion PDF](https://www.bnf.fr/sites/default/files/2019-01/UNIMARC%28A%29_2018_conversion.pdf) was not read.

## 4. Wikidata (and VIAF)

### Takeaway
Wikidata uses an open class hierarchy (P31 instance of / P279) for organization types. It explicitly keeps **legal form (P1454)** as a separate single-value property whose values are instances of "legal form" (Q10541491). Function or kind (class) and legal form are therefore separate axes.

### Cited Findings
- Q43229 organization is "social entity established to meet needs or pursue goals". It is a subclass of corporate body, group of humans, etc., and its notable subclasses include formal, informal, commercial, nonprofit, hybrid and government organization. Its typical properties include legal form (P1454) and industry (P452). [Wikidata Q43229](https://www.wikidata.org/wiki/Q43229)
- P1454 "legal form" ("legal form of an entity", aka legal structure / type of business entity) has value type Q10541491 (legal form) and a single-value constraint. Its subject types include organizations, humans, juridical persons and educational facilities. Examples: Haribo → GmbH & Co. KG; Migros → cooperative. [Wikidata P1454](https://www.wikidata.org/wiki/Property:P1454)

### Inferences
- The Wikidata pattern of class = what kind of body it is functionally (government agency, university, political party, religious organization…) and P1454 = legal form maps well onto splitting CAM subclasses into a function/sector type and a legal form attribute. Several CAM classes mix the two axes: CHARITY (foundation is a legal form), CLUB (association is a legal form), COMPANY (business is a sector, but also implies a legal form).

### Gaps
- VIAF typing was not researched. VIAF is understood to aggregate only personal/corporate/geographic/work clusters, but this was not verified this session.
- The Wikidata subclass list came from a page summary; the full P279 tree was not enumerated.

## 5. Archival producer typologies (PL, SK, AT, CZ)

### Takeaway
Archival practice classifies *record creators / fonds* by sector in a flat or two-level list that resembles CAM's PARTY_GROUP subclasses much more closely than library typologies do. The Polish SEZAM classification of record creators (17 groups) is a published national example. Comparable published typologies for Slovakia and Austria were not found in this session.

### Cited Findings
- Polish state archives systematise holdings by the SEZAM "classification of record creators" (klasyfikacja aktotwórców). The AP Warszawa, Otwock branch guide lists 14 of the groups used there:
  1. Administracja ogólna – państwowa i samorządowa
  2. Administracja specjalna
  3. Instytucje ochrony prawa i wymiaru sprawiedliwości
  4. Wojsko i organizacje paramilitarne
  5. Akta metrykalne i urzędy stanu cywilnego
  6. Samorząd gospodarczy, spółdzielczość
  7. Instytucje finansowe
  8. Instytucje gospodarcze, przedsiębiorstwa, zakłady
  9. Stowarzyszenia i związki
  10. Partie polityczne i podległe organizacje, ruchy społeczne
  11. Instytucje nauki i oświaty
  12. Instytucje kultury
  13. Instytucje ochrony zdrowia i opieki społecznej
  14. Zbiory

  Subgroups are subject, chronological and geographic. [AP Warszawa Oddział Otwock, Informator (2015)](https://warszawa.ap.gov.pl/container/publikacje/INF-OTWOCK-23-01-2015-FINALNY.pdf)
- The full classification has 17 groups. "Klasyfikacja aktotwórców" is an annex to Regulation (Zarządzenie) No. 7 of the Naczelny Dyrektor Archiwów Państwowych of 30 Jan 2018 on registering archival holdings. It includes family/estate archives (archiwa rodzinno-majątkowe) and personal papers (spuścizny). [search summary, Polish archival journals](https://ejournals.eu/pliki_artykulu_czasopisma/pelny_tekst/01948448-53c1-70f2-b9f6-40ea2f2d5243/pobierz); [Pułtusk informator](https://1939.warszawa.ap.gov.pl/container/publikacje/Informator_pultusk.pdf)

### Inferences
- The SEZAM groups align closely with CAM PARTY_GROUP subclasses:
  - Administracja ogólna/specjalna + justice ≈ PUBLIC_ADMINISTRATION (and REGION for self-government)
  - Wojsko ≈ ARMY
  - Przedsiębiorstwa + instytucje finansowe ≈ COMPANY
  - Stowarzyszenia ≈ CLUB/ORGANIZATION
  - Partie ≈ POLITICAL_PARTY
  - Nauka/oświata + kultura + zdrowie ≈ HEALTH_AND_EDU
  - Samorząd gospodarczy ≈ GUILD
  - Wyznania ≈ CHURCH

  This sector-based typology is an *archival fonds/producer classification* tradition (Central-European), not a library authority practice.
- Archival standards themselves (ISAAR(CPF) 5.1.1, EAC-CPF entityType: person / corporateBody / family) define only the three-way split. Sector subtypes are national additions.

### Gaps
- The full 17-group list, with exact names of groups 14–17 (presumably wyznania, archiwa rodzinno-majątkowe, spuścizny, zbiory), was not verified from the 2018 regulation text.
- Slovakia: no published producer typology was found (the Slovak archival portal and ELZA-SK configuration were not researched).
- Austria: no type list for Provenienzstellen in the ÖStA Archivinformationssystem (scopeArchiv) was found. A search returned nothing relevant.
- Czech beyond CAM: the NAD/PEVA fond-group classification was not located. The MV methodology PDF fetched (Metodický návod 2/2009) concerns AKP/NKP, not producer types.
- ISAAR(CPF) and EAC-CPF entityType values were not fetched in this session.

## 6. Cross-cutting: which distinctions recur, and attribute vs class

### Takeaway
Internationally recurring distinctions are: (1) person / family / corporate body; (2) conference/meeting/event as a special corporate body (GND top-level f, MARC 111, IdRef congrès); (3) jurisdiction/territorial body (MARC 110 ind1=1, GND gik as a geographic entity); (4) fictitious and supernatural entities (GND kxz, pxl, pxg, pxs; RDA/PCC treat them as agents); (5) religious bodies and companies (GND kir/kiv, kif); (6) subordinate organs (GND OrganOfCorporateBody/kio). Animals and family branches are rarely distinguished: GND treats animals as persons, and no source subtypes family branches. Fine-grained sector types (army, party, school, charity, club) appear in archival fonds classifications (Poland's SEZAM), not in library authority classes. Libraries record them as attributes (MARC 368 $a) or, as in Wikidata, separate legal form (P1454) from class.

### Cited Findings
- See the sections above: [GND codes](https://wiki.dnb.de/download/attachments/90411323/entitaetenCodes.pdf?api=v2), [MARC 110](https://www.loc.gov/marc/authority/ad110.html), [MARC 368](https://www.loc.gov/marc/authority/ad368.html), [Wikidata P1454](https://www.wikidata.org/wiki/Property:P1454), [SEZAM groups](https://warszawa.ap.gov.pl/container/publikacje/INF-OTWOCK-23-01-2015-FINALNY.pdf).

### Inferences
- Comparison matrix (✓ = class or code exists; attr = recorded as an attribute; — = not distinguished):

| CAM subclass | GND | MARC/RDA | Wikidata | PL SEZAM |
|---|---|---|---|---|
| REGION | gik (as Geografikum) | 110 ind1=1, 368 $b | class (e.g. administrative territorial entity) | Adm. ogólna (samorząd) |
| PUBLIC_ADMINISTRATION | kio (organs of territorial bodies) | 368 $a | class (government organization) | ✓ |
| ARMY | — | 368 $a | class | ✓ |
| COMPANY | kif | 368 $a | class + P1454 | ✓ |
| POLITICAL_PARTY | — | 368 $a | class | ✓ |
| CHURCH | kir/kiv | 368 $a | class | ✓ (Wyznania, unverified) |
| HEALTH_AND_EDU | — | 368 $a | class | ✓ (3 groups) |
| CHARITY / CLUB / GUILD / ORGANIZATION | — (kiz) | 368 $a | class and/or P1454 | Stowarzyszenia; Samorząd gosp. |
| (conference/event) | f / vie | 111 | class | — |
| (musical group, project) | kim, kip | 368 $a | class | — |
| FICTIVE_INDIVIDUAL | pxl | PCC: agent | class | — |
| PERSON_BEING | pxg, pxs | 368 $c "Spirit" | class | — |
| PERSON_ANIMAL | — (person) | PCC: non-human agent | class | — |
| FAMILY / FAMILY_BRANCH / FICTIVE_DYNASTY | pif / — / — | family; 376 $a (unverified) | class | archiwa rodzinno-majątkowe |

- Recommendation-relevant: an international package could keep three ISAAR/EAC/RiC top classes (plus an optional conference/event flag), and carry sector type and legal form as two separate controlled attributes. Jurisdiction/territorial body and fictitious/non-human are the two subtypes with the strongest international precedent for class-level treatment.

### Gaps
- The matrix's Wikidata column is generalised from Q43229 and P1454 only; specific Q-ids for army, party etc. were not checked.
