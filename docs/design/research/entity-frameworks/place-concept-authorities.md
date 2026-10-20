# Place and concept authorities in international archival / cultural-heritage practice

Research date: 2026-10-08. Scope: data-model implications for an international (ISAAR(CPF)/EAC-CPF/RiC-based) entity package in ELZA that must interoperate with the Czech CAM model (GEO, TERM).

## 1. Place models and the minimal common attribute set

### Takeaway
Every major model (RiC, TGN, GeoNames, Wikidata, LPF, GND, EAC-CPF 3.0) treats a place as a historical entity with: several names that can carry a language and dates; a place type drawn from a vocabulary (which can also carry dates); a part-of hierarchy whose links can carry dates (TGN allows several parents); optional coordinates; and links to external authorities. A minimal common set is therefore: names (language, dates, preferred flag), type, external identifiers or URIs (sameAs/closeMatch), parent place with dates, existence dates, and coordinates (optional, approximate). The models disagree only on whether these values sit on the place itself or on its relations.

### Cited Findings
**RiC-CM / RiC-O**
- RiC-CM 1.0 defines Place (RiC-E22) as a "Bounded, named geographic area or region". Its attributes are Place Type ("Categorization of a Place", e.g. settlement, administrative division, country, mountain, river), Coordinates (may follow ISO 6709) and Physical Location ("A delimitation of the physical territory of a Place", e.g. an address or cadastral reference). — [ICAD/RiC Place Type](https://web.esrc.unimelb.edu.au/ICAD/biogs/E000050b.htm); [RiC Coordinates](https://web.esrc.unimelb.edu.au/ICAD/biogs/E000212b.htm); [RiC Location](https://web.esrc.unimelb.edu.au/ICAD/biogs/E000053b.htm)
- rico:Place scope note: "A jurisdiction is the bounded geographic area within which an Agent has the authority to perform specified activities constrained by rules. Jurisdictions, man-made structures, and natural features are historical entities. A Place thus may have a beginning date and ending date and changing boundaries that result from human or natural events. A Place may be systematically referenced to a location on the earth (geographic coordinates)." — [rico:Place (Zazuko prefix server)](https://prefix.zazuko.com/rico:Place)
- RiC-O 1.1: "A Place is considered both a geographical and historical entity… it has a history, and may be preceded or succeeded by other Places". A Place "may have zero to many Physical Location through time (for instance, its boundaries, if it is an administrative area or a country, may change)". Supporting classes are PhysicalLocation, Coordinates, PlaceType and PlaceName. — [RiC-O 1.1 documentation](https://www.ica.org/standards/RiC/RiC-O_1-1.html)
- In RiC-O, controlled values such as types are named individuals that are "instances of both skos:Concept and of" the RiC type class, which makes it easy to integrate external SKOS vocabularies. — [RiC-O 1.1](https://www.ica.org/standards/RiC/RiC-O_1-1.html)
- rico:hasOrHadJurisdiction has domain rico:Agent and range rico:Place. Its inverse is rico:isOrWasJurisdictionOf, and it is a subproperty of rico:isAgentAssociatedWithPlace. — [rico:hasOrHadJurisdiction](https://prefix.zazuko.com/rico:hasOrHadJurisdiction)

**GeoNames**
- GeoNames has 9 feature classes: A (administrative boundary), H (hydrographic), L (area), P (populated place), R (road/railroad), S (spot), T (hypsographic), U (undersea), V (vegetation). Feature codes refine them, e.g. ADM1–ADM5, PCLI, PPLA, PPLC. — [GeoNames feature codes](https://www.geonames.org/export/codes.html)
- Historical entities are handled with separate feature codes, not with dates: ADM1H–ADM5H ("a former … administrative division"), ADMDH, PCLH ("a former political entity"), PPLH ("a populated place that no longer exists"). — [GeoNames feature codes](https://www.geonames.org/export/codes.html)

**Wikidata**
- P131 "located in the administrative territorial entity": "the item is located on the territory of the following administrative entity". The guidance is to "add only the most local administrative territory" and rely on the chain above it. Allowed qualifiers include start time P580, end time P582 and applies to jurisdiction P1001. Constraints block some historical states (e.g. Holy Roman Empire, West Germany) from being used as values. Related properties: P17 (country), P625 (coordinate location). — [Wikidata P131](https://www.wikidata.org/wiki/Property:P131)

**Getty TGN**
- A TGN record is a place concept with a numeric ID. Its names (preferred, variant and historical, with dates and flags such as vernacular, historical/current and official) "are considered equivalents (synonyms)". Each record has one preferred place type, and types can carry date ranges. — [TGN About](https://www.getty.edu/research/tools/vocabularies/tgn/about.html)
- "The TGN is polyhierarchical. Each Subject_ID may be linked to multiple Parent_IDs", with one parent marked as preferred. Hierarchy links have dates. Associative relations include "successor of" and "capital of". Coordinates "are approximate and intended for reference ('finding purposes') only". TGN covers both administrative and physical entities and has about 3.0M place records. It is published as LOD at vocab.getty.edu. — [TGN About](https://www.getty.edu/research/tools/vocabularies/tgn/about.html)

**Linked Places Format (World Historical Gazetteer)**
- Required: `@id`, `properties.title`, `properties.fclasses` (GeoNames-style letters A/H/L/P/R/S/T), `names[]` (each with `toponym` and `lang`, optional `when`), plus geometry and/or `when` (timespans or named periods). Encouraged: `ccodes[]` (ISO 3166-1 alpha-2), `types[]` (Getty AAT IDs with `sourceLabels` and `when`), `descriptions[]`, and `links[]` of type `closeMatch`/`exactMatch`/`primaryTopicOf`/`seeAlso`. Optional: `relations[]` (e.g. `gvp:broaderPartitive` with `when`) and `depictions[]`. — [LPF on GitHub](https://github.com/LinkedPasts/linked-places-format)

**EAC-CPF 3.0.0 (Tag Library, 2026 edition, 30 June 2026; supersedes 2.0.1 of 2024)**
- `<place>` is "a wrapper element used to encode information about a place or jurisdiction". It must have at least one `<label>`, and its children come in this order: label, date/dateRange/dateSet, `<role>` (e.g. "birthplace"; "strongly recommended"), `<relationship>`, address/contact/`<geographicCoordinates>`, descriptiveNote. Attributes include `placeType`, `valueURI`, `vocabularySource` and `vocabularySourceURI`. — [EAC-CPF 3.0.0 Tag Library (PDF)](https://eac.staatsbibliothek-berlin.de/schema/v3/EAC-CPF3-TL-eng.pdf)
- `@placeTypeEncoding="EASList"` limits `@placeType` to administrativeDivision, area, bodyOfWater, landElevation, populatedPlace, roadOrRailroad, spot, undersea and vegetation. These are effectively the 9 GeoNames feature classes. `otherPlaceTypeEncoding` points to a list named in `<conventionDeclaration>`. — [EAC-CPF 3.0.0 TL](https://eac.staatsbibliothek-berlin.de/schema/v3/EAC-CPF3-TL-eng.pdf)
- `<geographicCoordinates>` requires `@coordinateSystem` (e.g. WGS84, OSGB36, ED50). `<placeName>` takes `@countryCode` (with `@countryEncoding` set to iso3166-1-alpha-2/alpha-3/numeric, iso3166-2, iso3166-3 or other). Use `@valueURI`/`@vocabularySource`/`@vocabularySourceURI` "when referencing an entry of an external vocabulary, thesaurus, ontology", and `@target` to point to a fuller `<place>`. — [EAC-CPF 3.0.0 TL](https://eac.staatsbibliothek-berlin.de/schema/v3/EAC-CPF3-TL-eng.pdf)

**GND**
- Place records have Satzart Tg. Entity codes are gik (Gebietskörperschaft: states, member states, administrative units, municipalities), with sub-codes gil (independent state) and gif (member state), plus gir and giv, and giz/gif for other kinds. — [DNB EH-G-06](https://wiki.dnb.de/download/attachments/90411357/EH-G-06.pdf); [AWB A02 Tg](https://wiki.dnb.de/download/attachments/90411357/AWB-A02-Tg.pdf?api=v2)
- In the GND ontology, subclasses of gndo:PlaceOrGeographicName are AdministrativeUnit, BuildingOrMemorial, Country, ExtraterrestrialTerritory, FictivePlace, MemberState, NameOfSmallGeographicUnitLyingWithinAnotherGeographicUnit, NaturalGeographicUnit, ReligiousTerritory, TerritorialCorporateBodyOrAdministrativeUnit (MARC 079 $v gik) and WayBorderOrLine. The ontology also has gndo:precedingPlaceOrGeographicName (predecessor/successor) and agent properties placeOfBirth and placeOfActivity. — [GND Ontology](https://d-nb.info/standards/elementset/gnd)

### Inferences
- A minimal place model that maps cleanly to all of the above:
  - Names: one or more, each with language, optional from/to dates and a preferred flag. This covers TGN names, LPF `names[].when` and the RiC PlaceName class.
  - Type: one or more, from a controlled list, optionally dated. The coarse level is the 9 GeoNames classes (= EAC-CPF EASList = LPF fclasses), with an optional finer type from AAT or a local list.
  - Existence dates: begin and end.
  - Parent place: one or more, each with dates. This covers TGN polyhierarchy, Wikidata P131 with P580/P582 and LPF broaderPartitive with `when`.
  - Coordinates: optional, one point, WGS84, stated to be approximate.
  - Country code: ISO 3166-1/-3.
  - External links: URI plus match type (exactMatch/closeMatch) to GeoNames, Wikidata, TGN, GND or national registers such as RÚIAN.
  - Optional: predecessor/successor place.
- CAM's GEO class set (administrative territories, natural areas, waters, etc.) can be mapped onto the GeoNames/EASList classes: administrative → A, waters → H, land formations → T, sea formations → U/H, other localities → P/S/L. Using that coarse list as the international place type would give a lossless mapping CAM → international (lossy in reverse).
- GeoNames models history through feature codes (ADMxH) rather than dates, while TGN, Wikidata and LPF date the relations. A dated model is the one that can absorb all of them.

### Gaps
- I did not retrieve the exact RiC-O property names for place names, place types and part-of between places (e.g. rico:hasOrHadPlaceName, rico:hasOrHadPlaceType, and whether the part-of property is shared with other entities); only the class names were confirmed. Check the RiC-O 1.1 HTML (from offset 100000 onward).
- Whether RiC-CM 1.0 Place carries a separate "Name" attribute or uses the generic Name entity was not confirmed from the primary source.

## 2. Historical administrative units and the place vs. jurisdiction / corporate body duality

### Takeaway
International practice keeps the territory as a place and models the governing body, when needed, as a separate agent linked to it by a jurisdiction relation (RiC). GND keeps Gebietskörperschaften inside the place class rather than the corporate-body class. Library practice (RDA/NACO) lets the same geographic heading also act as a corporate access point for the government. Changing hierarchies are modelled with dated part-of links and predecessor/successor relations, not by creating a new record each time a parent changes.

### Cited Findings
- RiC: a jurisdiction is a place ("the bounded geographic area within which an Agent has the authority…"). The agent–place link is rico:hasOrHadJurisdiction / isOrWasJurisdictionOf (Agent → Place). — [rico:Place](https://prefix.zazuko.com/rico:Place); [rico:hasOrHadJurisdiction](https://prefix.zazuko.com/rico:hasOrHadJurisdiction)
- GND: gndo:TerritorialCorporateBodyOrAdministrativeUnit ("Gebietskörperschaft oder Verwaltungseinheit") has only one superclass, PlaceOrGeographicName. It is not a subclass of CorporateBody. — [GND Ontology](https://d-nb.info/standards/elementset/gnd)
- LC/NACO practice: a geographic heading (MARC 151) can be established in the name or subject file, but "only a geographical heading established in the name authority file can be used as an access point for a jurisdiction or a government (110)". 151 is also used for jurisdictions such as countries, states, counties, cities and towns, and for various special districts. — [GCcollab FAQ on geographic names](https://wiki.gccollab.ca/FAQ_on_geographic_names); [FDLP](https://fdlp.gov/node/49100)
- EAC-CPF 3.0 `<place>` explicitly covers "a place or jurisdiction" related to the CPF entity. The jurisdiction is not a separate element. — [EAC-CPF 3.0.0 TL](https://eac.staatsbibliothek-berlin.de/schema/v3/EAC-CPF3-TL-eng.pdf)
- Wikidata keeps one item per administrative territorial entity. Changing membership is expressed with P131 plus P580/P582 qualifiers, and constraints steer editors away from linking to some historical polities. — [Wikidata P131](https://www.wikidata.org/wiki/Property:P131)
- TGN: dated hierarchy links, several parents (one preferred), and "successor of" associative relations. — [TGN About](https://www.getty.edu/research/tools/vocabularies/tgn/about.html)
- GeoNames: former units get separate historical feature codes (ADM1H…ADMDH, PCLH, PPLH). — [GeoNames codes](https://www.geonames.org/export/codes.html)

### Inferences
- Recommendation: a municipality, district or state is a place. Its office (e.g. municipal office, district authority, government) is a corporate body linked by a jurisdiction relation (agent → place, with dates). This follows RiC and GND, and it fits CAM, where the administrative territory sits in GEO and the offices sit in PARTY/corporation.
- Avoid one record that is both a place and an agent. If a source treats the territory itself as the creator (RDA 110 on a 151 heading), map it to the place plus an implied jurisdiction agent, or allow "place as creator" only through an explicit relation.
- When a parent unit changes (e.g. district reforms), add a dated parent link. When the territory itself changes identity (merger, split), create a new place and link it with preceded-by/succeeded-by (GND precedingPlaceOrGeographicName, TGN "successor of", RiC succession between places).

### Gaps
- I did not fetch RDA Toolkit text on jurisdictions (subscription site). The LC/NACO rule is cited through secondary training material (GCcollab, FDLP).
- Wikidata's handling of the place-vs-government split (separate items for, e.g., a city and its city council or municipal government) was not verified with sources.

## 3. Concepts / terms: SKOS vocabularies, occupations, functions, subjects

### Takeaway
Archival encoding standards (EAC-CPF 3.0) treat occupations, functions and subjects as terms that may be free text or drawn from a vocabulary identified by vocabularySource/vocabularySourceURI/valueURI. They do not require local concept entities. Major vocabularies (ESCO/ISCO, GND Sachbegriffe, EHRI terms, RiC-O named individuals) are published as SKOS with URIs. The usual pattern is therefore "term + URI link to a SKOS vocabulary", and a local concept authority is kept only where no suitable external vocabulary exists (e.g. domain thesauri such as EHRI terms).

### Cited Findings
- EAC-CPF 3.0 `<occupation>` "uses the child element `<term>` … Terms may be drawn from controlled vocabularies or may be natural language terms". `<term>` can repeat for translations (`@languageOfElement`). It may have date(s), `<placeName>` and `<descriptiveNote>`, and it carries `valueURI`, `vocabularySource` and `vocabularySourceURI`. `<term>` is likewise the required child of `<functionPerformed>`, `<legalStatus>`, `<localDescription>`, `<mandate>`, `<demographicDescription>` and `<otherEntityType>`. — [EAC-CPF 3.0.0 TL](https://eac.staatsbibliothek-berlin.de/schema/v3/EAC-CPF3-TL-eng.pdf)
- ESCO takes the top four levels of its occupation pillar from ISCO-08. ESCO occupations sit at level 5 and below, and "Each ESCO occupation is mapped to exactly one ISCO-08 code" (mono-hierarchy). ESCO is SKOS/LOD with URIs in all official EU languages. Current version: ESCO v1.2.1 (updated 10/12/2025). — [ESCOpedia: ISCO](https://esco.ec.europa.eu/en/about-esco/escopedia/escopedia/international-standard-classification-occupations-isco)
- GND has SubjectHeading subclasses (SubjectHeadingSensoStricto, GroupOfPersons, HistoricSingleEventOrEra, EthnographicName, etc.) and an agent property gndo:professionOrOccupation, whose values are GND subject records. — [GND Ontology](https://d-nb.info/standards/elementset/gnd)
- RiC-O controlled values are instances of skos:Concept plus the RiC type class, which is how RiC expects types (record set types, documentary form types, and by analogy occupation and activity types) to be handled. — [RiC-O 1.1](https://www.ica.org/standards/RiC/RiC-O_1-1.html)
- EHRI: the portal has three controlled vocabularies, one for subjects (EHRI terms: 870+ terms by 2017, each in 12 languages) and two for places (camps, ghettos). These are published in SKOS. GeoNames is used for general geography and Wikidata for camps/ghettos. — [EHRI vocabularies](https://portal.ehri-project.eu/vocabularies); [EHRI Ghettos](https://portal.ehri-project.eu/vocabularies/ehri_ghettos); [EHRI D11.2 roadmap](https://ehri-project.eu/wp-content/uploads/2023/10/D-11.2-Road-map-domain-vocabularies.pdf)

### Inferences
- For the international package, concepts (occupation, function, subject) can be "term + optional language + optional vocabulary URI" on the agent's description item. This follows EAC-CPF exactly and needs no new entity class.
- If ELZA wants reuse and multilingual labels, a lightweight local SKOS-like concept entity (prefLabel per language, altLabels, broader, exactMatch/closeMatch URIs to ESCO/ISCO, GND, Wikidata or LCSH) can serve as a cache or extension. CAM's TERM can play this role, so the international package can refer to TERM records instead of declaring its own class.
- Dated occupations (EAC-CPF puts dates and place on `<occupation>`) belong on the agent–concept link, not on the concept.

### Gaps
- LCSH and Wikidata P106 (occupation) were not fetched in this session. Both are widely used SKOS/LOD sources, but I did not verify current specifics.
- I did not find quantitative evidence of how many archives keep local concept authorities versus linking externally.

## 4. Practice examples: local place authorities vs. external references

### Takeaway
The dominant pattern is hybrid. Institutions keep a local place record (their own URI, a label harmonised with national indexing rules, and local hierarchy) and align it to GeoNames and/or Wikidata (and TGN/GND where relevant). SNAC and the French national archives both do this. EHRI uses GeoNames for ordinary geography and builds its own SKOS authority only for domain-specific places (camps, ghettos) that the general gazetteers cover poorly.

### Cited Findings
- Archives nationales de France (Garance, RiC-O): place descriptions "form an incomplete but coherent geographical reference dataset, built progressively as the records … or the agents described are documented". Places have types (région administrative, département, quartier, canal, phare…) and part-of qualifiers, e.g. "Finistère (Bretagne, département)". — [AnF Garance: places](https://rdf.archives-nationales.culture.gouv.fr/garance/pages/en/places)
- FranceArchives provides place authority files aligned to GeoNames, with labels harmonised according to the "Guide d'indexation à l'heure du web" (Aug 2021) and a permanent dereferenceable URI for each authority. — [data.gouv.fr FranceArchives places](https://data.gouv.fr/es/datasets/francearchives-fr-les-lieux-des-archives); [FranceArchives article](https://francearchives.gouv.fr/article/794416740)
- SNAC: the geographic authority is "based primarily on geonames" (table geo_place), and constellations link to it through place_link. Place roles in the EAC sense include Birth, Death, Citizenship and Residence. — [SNAC documentation commit](https://gitlab.iath.virginia.edu/snac/Documentation/commit/77f410dd970a7cca701b3e7bf93bfe4743fce6ce); [SNACSchool Module 3](https://portal.snaccooperative.org/system/files/media/documents/Public/SNACSchool%204.0%20Module%203%20Create%20and%20Edit.pdf)
- EHRI: GeoNames for geographic locations; own SKOS authority lists for camps and ghettos built from the USHMM and Yad Vashem encyclopedias, ITS (Arolsen), Wikidata and Bundesarchiv, with Wikidata alignment. — [EHRI vocabularies](https://portal.ehri-project.eu/vocabularies); [EHRI data integration paper (arXiv 2505.02455)](https://arxiv.org/pdf/2505.02455)
- Archives Portal Europe: integrating GeoNames lookups with EAC-CPF has been listed as a development priority (blog report, not a spec). — [Archives Hub blog](https://blog.archiveshub.jisc.ac.uk/category/archives-portal-europe)

### Inferences
- Pattern to adopt: a local place entity (CAM GEO in the Czech installation, or a minimal international place) carrying external sameAs URIs. Do not store external URIs only, because archives need historical places, local labels and dated hierarchies that GeoNames lacks and that Wikidata covers unevenly.

### Gaps
- I found no primary documentation for Arolsen Archives' own place authority (they appear only as a source for EHRI camps).
- Archives Portal Europe's current (2026) handling of places and topics is not documented in what I found. The source is an older blog.

## 5. Recommendations for a minimal, extensible model interoperable with CAM GEO/TERM

### Takeaway
The international package should declare minimal place and concept classes, or reuse CAM's GEO/TERM directly when CAM is installed, and not depend on external links alone. Attributes should be limited to the intersection of RiC, EAC-CPF 3.0, LPF and TGN, and every record should carry external authority URIs (exactMatch/closeMatch) so that national and international records can be merged.

### Cited Findings
- EAC-CPF 3.0 can carry a fully external reference (`placeName@valueURI` + `@vocabularySource`) or a local `<place>` with a label, dates, role, placeType and coordinates, linked through `@target`. Both styles are legitimate. — [EAC-CPF 3.0.0 TL](https://eac.staatsbibliothek-berlin.de/schema/v3/EAC-CPF3-TL-eng.pdf)
- LPF's `links[]` uses SKOS match semantics (`closeMatch`/`exactMatch`), `ccodes` (ISO 3166-1) and `types` keyed to AAT. — [LPF](https://github.com/LinkedPasts/linked-places-format)
- The EAC-CPF EASList place types match the GeoNames classes. — [EAC-CPF 3.0.0 TL](https://eac.staatsbibliothek-berlin.de/schema/v3/EAC-CPF3-TL-eng.pdf); [GeoNames codes](https://www.geonames.org/export/codes.html)

### Inferences
- **Place (minimal)**:
  - preferred name and variant names, each with language and optional dates
  - coarse type (EASList / GeoNames class), optional fine type (AAT/local), optionally dated
  - existence dates
  - parent place(s) with dates
  - optional WGS84 point
  - optional ISO 3166 country code
  - repeatable external identifier (scheme + value or URI + match type: GeoNames, Wikidata, TGN, GND, RÚIAN, ISO 3166)
  - optional predecessor/successor
  - CAM extras (RÚIAN codes, its detailed GEO subclasses and GEO_ADMIN_CLASS) stay in the national layer. The shared identifiers and URIs are the join key.
- **Agent–place links** carry the role (birth, death, residence, seat, activity, jurisdiction) and dates on the relation, as in EAC-CPF `<place><role>` with a date and RiC agent–place properties. Jurisdiction is a dated agent→place relation, not a property of the place.
- **Concept (minimal)**: prefLabel per language, altLabels, optional broader, a scheme/vocabulary identifier and exactMatch/closeMatch URIs (ESCO/ISCO, GND, Wikidata, LCSH). Occupation and function links on agents carry dates and an optional place.
- **Allow external-only references** (an item with a URI and label, no local entity) for one-off places and concepts. Promote one to a local entity when it is reused or needs history. This reproduces the EAC-CPF `placeName@valueURI` vs `<place>` split.
- When CAM is present, map international place → GEO and concept → TERM through rules and the shared identifiers, rather than duplicating classes.

### Gaps
- No published crosswalk between CAM GEO/TERM and RiC, LPF or EAC-CPF was found in this session.
