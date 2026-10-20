# Reference codes (ISAD(G) 3.1.1): what the standard asks for and what archives actually use

Research date: 2026-10-19. Sources checked: ISAD(G) 2nd edition as reproduced in the ICA
Human-Rights Working Group toolkit (2012, English PDF); *Základní pravidla pro zpracování
archiválií* v3.1 (2022-10-06, PDF from the National Archives); Library of Congress EAD 2002 tag
library (`<unitid>`) and EAD3 tag library (examples only, the element entry was not reached);
citation and catalogue help pages of the national archives named below, as published on
2026-10-19; AtoM 2.8 user manual and the ICA-AtoM wiki; secondary pages on ArchivesSpace. The ELZA
findings come from the code of `rules-en-isadg` and `elza-core` on branch 3.4.x.

Main finding: **ISAD(G) requires a unique identifier made of country code, repository code and a
local part, and prescribes no form for the local part.** Every archive checked forms the local
part by its own convention, assigns it once and keeps it. None derives it from the position of
the unit in the tree, which is what the Czech rule ZP2015 4.2.1 and ELZA's `!UnitIdGenerator` do.

## 1. ISAD(G) 3.1.1 Reference code(s)

### Takeaway
Uniqueness and the three parts are the rule; the examples are all local conventions of the
repositories (letters, series numbers, accession numbers). The application notes of the toolkit
allow consecutive or random series numbers and warn that the code is not a box number.

### Cited findings
- Purpose: "To identify uniquely the unit of description and to provide a link to the description
  that represents it." Rule: "Record, as necessary for unique identification, the following
  elements: — the country code in accordance with the latest version of ISO 3166 …; — the
  repository code in accordance with the national repository code standard or other unique
  location identifier; — a specific local reference code, control number, or other unique
  identifier. All three elements must be present for the purpose of information exchange at the
  international level." — [ICA HRWG toolkit, ISAD(G) EN](https://www.ica.org/app/uploads/2023/12/HRG_2012_toolkit_ISAD-G_EN.pdf), section 1.1
- Examples: `CA OTY F0453 (Fonds)`, `CA OONAD R610-134-2-E (Fonds)`, `US MnHi P2141 (Fonds)`,
  `US DNA NWDNC-77-WDMC (Series)`, `AU A:NLA MS 8822 (Fonds)`, `FR CHAN/363 AP 15 (File)`,
  `FR AD 53/234 J (Fonds)`, `FR AN 320 AP (Fonds)`, `IT ISR FI II/36/4 (Subfile)`,
  `BR AN SA (Fonds)` — same source
- Application rules of the toolkit: "c. The first numbers or letters in the reference codes
  identifies the fonds; the last number is the level being described"; "e. Series either are
  numbered consecutively throughout a fonds (for example, 77-01, 77-02, 77-03) or are numbered
  randomly using an accession code or pre-existing number"; "f. It is helpful to put an alphabetic
  marker before a series; for example 77 S01"; "g. The numbering in this element is a reference
  code, NOT A BOX NUMBER" — same source

### Inferences
- The local part may be alphanumeric, may carry spaces and letters, and may be a running number
  that does not encode the hierarchy. A rule set that accepts only `number([+|-]number)` parts
  cannot hold the standard's own examples.
- The fonds itself has a code (every "(Fonds)" example). A generator that forbids a code on the
  root contradicts the standard.

## 2. ZP2015 4.2.1 Referenční označení

### Takeaway
The Czech rule defines the code as the position of the unit in the hierarchy, with a fixed
grammar of `/` and `//`, digits only, `+`/`-` for later insertions, assigned after the finding aid
is finished. It is a national convention layered on ISAD(G), not the standard itself.

### Cited findings
- "Referenční označení je řetězec znaků, který identifikuje místo uložení jednotky popisu (stát,
  archiv) a vyjadřuje umístění jejího popisu v hierarchické struktuře příslušného archivního
  souboru" — [ZP v3.1](https://stands.nacr.cz/da-aip/_downloads/41e7d11ed22d648a23f272dc8a4cfe1b/Zakladni_pravidla_pro_zpracovani_archivalii_v3-1_-_20221006.pdf), 4.2.1
- Rule 3: "V rámci referenčního označení má každá úroveň popisu svou pevně danou pozici. Úrovně
  složka a jednotlivost sdílejí společnou pozici … Vnořování je vyjádřeno oddělovači – dvě lomítka
  ohraničují příslušnou úroveň popisu, jedno lomítko vyjadřuje vnořování v rámci této úrovně" —
  same source
- Rule 4: "V rámci archivního souboru se řetězec referenčního označení neopakuje … Referenční
  označení je tvořeno zpravidla po dokončení tvorby archivní pomůcky." Rule 5: insertions use
  "rozšíření stejného pořadového čísla … s použitím znaku + nebo -". Rule 6: a moved unit gets a
  new code and the old one goes to "Jiné označení" — same source
- Full example: `CZ100000010//742/1//1/15/2//1//10/1-1`; cited from the series down as
  `1/15/2//1//10/1-1` — same source

### Inferences
- The ZP2015 code is a path, so it changes when the arrangement changes; the rule mitigates this
  by assigning codes last and by the `+`/`-` insertions. Foreign conventions avoid the problem by
  not encoding the path at all.
- The ELZA generator implements rules 3 to 6 for the levels of ZP2015; its grammar and its
  prohibition of a root code follow from the Czech data model (the fonds is identified by its NAD
  number outside the tree).

## 3. EAD

### Takeaway
`<unitid>` is free text, repeatable, with attributes for the country and repository codes and a
type to tell former or other identifiers apart. Nothing in EAD expects a positional grammar.

### Cited findings
- EAD 2002: `<unitid>` carries an optional `TYPE` naming the system the identifier came from,
  `COUNTRYCODE` (ISO 3166-1), `REPOSITORYCODE` (ISO 15511) and `IDENTIFIER` (a machine-readable
  form). "Although not required, COUNTRYCODE and REPOSITORYCODE should be used on `<unitid>` at
  the `<archdesc><did>` level to meet ISAD(G) element 3.1.1." Example:
  `<unitid countrycode="us" repositorycode="cbgtu" label="Accession number">GTU 2001-8-03</unitid>` —
  [LoC EAD 2002 tag library, unitid](https://loc.gov/ead/tglib/elements/unitid.html)
- EAD3 examples: `<unitid countrycode="US" repositorycode="cu-i" label="Collection number">MS-R72</unitid>`,
  `<unitid … identifier="http://library.syr.edu/guides/s/stone_dc.htm" label="Accession number">GTU 2001-8-03</unitid>` —
  [LoC EAD3 tag library](https://www.loc.gov/ead/EAD3taglib/index.html)
- Archives Hub (UK aggregator): the reference code has three parts, country code, repository
  code and local reference; the element text holds the local reference and `<unitid>` "must be
  included at all levels of description" — [Archives Hub, unitid](https://archiveshub.ac.uk/help/unitid)
- ZP2015's EAD export in ELZA writes `CZ<institution code>//<NAD number>//<referenční označení>`
  as `<ead:unitid localtype="REFERENCNI_OZNACENI">` — `rules-cz-zp2015/src/rul_rule_set/ZP2015/templates/INVENTAR_EAD/index.ftl`

## 4. Conventions of national archives

### Takeaway
Codes are alphanumeric and permanent. The fonds carries letters or a number; lower levels extend
it or carry a running number. Several conventions name a physical unit (article, piece, volume).

### Cited findings
| Archive | Form | Source |
|---|---|---|
| UK, The National Archives | Department letter code, series number, piece, item: `RAIL 1021/6`. "The department code consists of one or more capital letters … Series numbers follow the department code after a space … The piece number follows the series after a slash." Cite "exactly as it appears in Discovery". | [TNA glossary](https://www.nationalarchives.gov.uk/help-with-your-research/discovery-help/glossary-for-discovery-our-catalogue/), [TNA citing records](https://www.nationalarchives.gov.uk/help-with-your-research/discovery-help/citing-records-national-archives/) |
| France, Archives nationales and departmental archives | Série letter, sous-série number, article running number: `F/7/12345`, `Q2/1 à 222`. "Each series is designated by one or more letters … for a sub-series, the letter is preceded by a number … followed by an article number"; an article is sized to one storage unit. | [cadre de classement, CDG 44](https://www.cdg44.fr/sites/default/files/atoms/files/Fiche%20archives%20n%C2%B023%20cadre%20de%20classement.pdf), [Geneawiki, Série Q](https://fr.geneawiki.com/wiki/Archives_nationales_-_S%C3%A9rie_Q) |
| Germany, Bundesarchiv | Bestand signature plus running file number: `R 43-II/1234`, sub-holdings R 43, R 43-I, R 43-II; R 43 II "organized in the following order: File 1-1662". | [Bundesarchiv, Reichskanzlei](https://www.bundesarchiv.de/im-archiv-recherchieren/archivgut-recherchieren/nach-themen/reichskanzlei/schriftgutbestaende-der-reichskanzlei/), [EHRI, R 43 II](https://portal.ehri-project.eu/units/us-005578-irn507405) |
| Switzerland, Bundesarchiv | Fonds, accession, dossier: `CH-BAR#E2001E#1969/121#757*`, older form `E 2001(E)1969/121 vol.50`. | [Dodis dossier](https://dodis.ch/dossier/42678) |
| Netherlands, Nationaal Archief | Finding-aid number and a flat inventory number: `NL-HaNA, …, 2.21.179.03, inv.nr. 123` (citeerinstructie of every inventaris). | [Nationaal Archief, inventaris 2.21.179.03](https://www.nationaalarchief.nl/onderzoeken/archief/2.21.179.03/download/xml) |
| Poland, state archives | Fonds identifier, series and subseries numbers, `sygnatura`: "sygnatura jest numerem kolejnym w obrębie zespołu (zbioru) archiwalnego, wyrażonym wyłącznie cyframi arabskimi", unchanged once assigned; cited as `34/247/0, sygn. II-37`. | [zarządzenie NDAP 7/2018](https://www.gov.pl/attachment/3591007e-6bb6-46d9-b322-c84a72588ce4), [Bydgoszcz library guide](https://biblioteka.bydgoszcz.pl/wp-content/uploads/2026/06/Archiwalia-KP.pdf) |
| Sweden, Riksarkivet | Archive number, series signum, volume: `SE/RA/721473` at the top; a dedicated reference-code search field. | [Riksarkivet NAD help](https://sok.riksarkivet.se/en/nad), [NAD record via Nuohtti](https://nuohtti.com/Collection/riksarkivet.SE%252FRA%252F721473?lng=en-gb) |
| Spain, PARES and Madrid archives | Reference code and `signatura` are distinct fields; the code carries `ES 28079` plus a hyphenated path, the signatura a local shelf code. | [UPM archive record](https://ahdb.upm.es/index.php/seccion-18) |
| Australia, NAA | Series number and control symbol: "NAA: P1556, Heard Island 1954"; the barcode is a lookup key, not part of the citation. No fonds hierarchy (series system). | [NAA, citing archival records](https://www.naa.gov.au/help-your-research/using-collection/citing-archival-records) |
| USA, NARA | Record group, series, entry, box: "Record Group 36 … Entry 914 - Box 501I"; NARA "requests that you not use box numbers"; the NAID identifies the catalogue record. | [NARA leaflet 17](https://WWW.ARCHIVES.GOV/publications/general-info-leaflets/17-citing-records.pdf), [Evidence Explained](https://evidenceexplained.com/node/112) |
| USA, DACS | 2.1 Reference Code is a required element: local identifier, repository and country. | [DACS 2019.0.3](https://files.archivists.org/pubs/DACS_2019.0.3_Version.pdf) |

### Inferences
- The only conventions that encode a path are the Czech ZP2015 and, partly, Poland's series
  numbers and Spain's classification path; even those keep a permanent running `sygnatura` or
  `signatura` beside the path.
- Letters are normal (`RAIL`, `F`, `R 43-II`, `E I a`, `MS-R72`), so a UNITID that only accepts
  digits excludes most of the world.

## 5. Software

### Takeaway
AtoM and ArchivesSpace store a free identifier per level. AtoM can display the concatenation of
the identifiers of the ancestors; ArchivesSpace generates an internal id and leaves the component
identifier to the institution.

### Cited findings
- AtoM: "Provide a specific local reference code, control number, or other unique identifier"
  (identifier field); "The country and repository code will be automatically added from the linked
  repository record to form a full reference code"; with "Inherit reference code" on, "the full
  reference code including any identifiers inherited from higher levels will appear below the
  Identifier field" — [AtoM 2.8 ISAD template](http://www.accesstomemory.org/en/docs/2.8/user-manual/data-templates/isad-template/)
- ICA-AtoM: "Each child archival description will inherit reference code data entered at the
  parent levels of description" — [ICA-AtoM wiki, ISAD](https://wiki.ica-atom.org/ISAD)
- ArchivesSpace: ref ids "are randomly generated 32-bit hexadecimal strings that are unique within
  each collection"; the component unique identifier is a separate field that a processing guide
  says may be left blank — [Rockefeller Archive Center](https://blog.rockarch.org/replacing-refids-for-automation-and-standardization), [University of Oklahoma guide](https://guides.ou.edu/archivalprocessing/createarchivalobject)
- SAA dictionary: a reference code is "a set of letters and/or numbers that uniquely identifies an
  archival collection or its components" — [SAA Dictionary](https://dictionary.archivists.org/entry/reference-code.html)

## 6. What `!UnitIdGenerator` would do with an ISADG fonds

### Takeaway
The ZP2015 generator cannot serve ISAD(G) as it is: it refuses the root code, accepts only digits,
numbers a subseries like a file and deletes manual codes.

### Cited findings (code of branch 3.4.x)
- The action numbers the units by position; `/` nests within a level and `//` is inserted where
  a unit of another level sits under the level named by `extraDelimiterAfter` —
  `elza-core/src/main/java/cz/tacr/elza/bulkaction/generator/unitid/LevelGenerator.java`,
  `AssignedUnitId.java`
- Parts must match `number([+|-]number)`; any other character throws "Invalid token" —
  `elza-core/src/main/java/cz/tacr/elza/bulkaction/generator/unitid/UnitIdPart.java`
- The root must have no code: "Root node has unit id" —
  `elza-core/src/main/java/cz/tacr/elza/bulkaction/generator/unitid/UnitIdGenerator.java`;
  ISADG's `AvailableItems.drl` requires the code on the root
- A replaced value is kept as a former code only when fully sealed; sealing comes from
  `!SealUnitId`, which ISADG does not ship, so the old value is deleted —
  `LevelGenerator.generateNode`, `SealedUnitIdTree.addSealedValue`
- Only the one level spec of `extraDelimiterAfter` counts as "series": a subseries under a series
  gets `//` like a file — `LevelGenerator.isExtraSlashBoundary`

## Gaps
- The separator AtoM uses when it concatenates inherited identifiers was not found in the manual.
- The EAD3 tag library entry of `<unitid>` was not reached (the page is served in parts); EAD3
  attributes are taken from its examples and from EAD 2002.
- Riksarkivet's series signa (allmänna arkivschemat) and the Polish `35/1234/0` fonds identifier
  were not read from primary documentation.
- The Swiss signature logic comes from Dodis records, not from the Bundesarchiv's own guidance.
