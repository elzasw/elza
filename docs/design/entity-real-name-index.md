# Entity search: the real name index

How the text search of entities ranks an entity whose name the user types the way it is commonly
used ("Petr Pytelka", "Československá akademie věd") above the entities whose catalogue names
merely contain those words: every name part gets the index `REAL_NAME`, the search configuration
says which indexes the Lucene index holds, and the whole search text is matched against the whole
index value. Bugzilla #9800, continuation of #9575.

## Problem

The catalogue form of a name carries the inverted order and the supplements that keep it unique:
`Pytelka, Petr (1975-)`, `Československá akademie věd. Kabinet pedagogických věd`. A user types
the common form, without them. The search splits the text into words and requires every word to
match some field, so all the candidates match; their order is decided by the boosts of the
configuration and by the BM25 score of the whole text on the analyzed fields. Without a stored
value that *equals* what the user typed, nothing separates the candidates reliably:

- the main name (`NM_MAIN`) is shared by a corporate body and its organisational units
  (`Československá akademie věd` is the main name of the academy and of each of its institutes),
  so an exact match on it, the strongest clause of the item fields (`pref_nm_main`), cannot
  separate them;
- for a person the main and minor names are separate fields, so "petr pytelka" exactly matches
  neither;
- the display name carries the supplements, so an exact match on it fails whenever the entity has
  one.

## Names and their indexes

An `INDEX` entity rule (Groovy, per class and part type,
`package-cz-base/src/rul_rule_set/CAM/rules/index/<CLASS>/PT_NAME.groovy`) builds the indexes of
a name part: `DISPLAY_NAME` (and `DISPLAY_NAME_LOWER`), `SORT_NAME`, `SHORT_NAME`, `REAL_NAME`,
and the key value `PT_PREFER_NAME` of the preferred name. They are stored in `ap_index` (one row
per part and type, `PartService.updatePartIndexes`), copied into the serialized entity cache
(`ap_cached_access_point`), kept for revisions (`ap_rev_index`) and written into the CAM XML
export as the `eits` items of a part.

`SHORT_NAME` is the main name (plus minor name and degrees) with the geographic supplement in
brackets. It is not a search field. It is the short name of the institution described by the
entity (`InstitutionService`, statistics in `ReportServiceQuery`), the text form of an entity
reference in export filters (`FilterRule`), and `%s` of the CAM user-name template. The
geographic supplement is part of its contract, which is why the search got an index of its own.

`REAL_NAME` is the name as it is commonly used: no degrees, no supplements (`NM_SUP_*`), no
ordinal, natural word order for persons. The core does not know it; CZ_BASE produces it and names
it in its search configuration.

| Class | `REAL_NAME` | Display name → real name |
|---|---|---|
| PERSON | `NM_MINOR NM_MAIN` | `Pytelka, Petr (1975-)` → `Petr Pytelka` |
| PARTY_GROUP | `NM_MAIN. NM_MINOR` | `Československá akademie věd. Kabinet pedagogických věd` → the same; the academy itself → `Československá akademie věd` |
| FAMILY, FAMILY_BRANCH, FICTIVE_DYNASTY | `NM_MAIN. NM_MINOR` | `Lobkowiczové (rod/rodina)` → `Lobkowiczové` |
| GEO, TERM, ARTWORK, EVENT | `NM_MAIN` | `Praha (Česko)` → `Praha` |

The word order for persons is a presentation choice: the exact match of the search ignores the
order of the words (below), so "petr pytelka", "pytelka petr" and "pytelka, petr" all meet
`Petr Pytelka`. Archivists type the inverted form, lay users the natural one; the natural form was
chosen because it reads as the name of a well-known person does ("Božena Němcová"). Changing it
is a one-line change of the script and a regeneration of the entities.

### Uniqueness of indexes

`RuleService.validateIndexRepeatability` reports `AP_DUPLICATE_INDEX` when two parts of one
entity under the same parent share an index type and value, unless a validation rule marked the
index repeatable. Rule R_UNQ_002 (`validation/GLOBAL.drl`) marks `DISPLAY_NAME_LOWER`,
`SORT_NAME` and `REAL_NAME` repeatable among names: two names of one entity that differ only in
supplements or degrees (the preferred name and a variant) legitimately share the real name.
`DISPLAY_NAME` stays unique.

## Search fields of an index type

The `index-search` setting (`SettingIndexSearch`) has, besides item fields, fields with the
attribute `index-type`:

```xml
<ns10:field name="pref_real_name" index-type="REAL_NAME"
            boost-exact="1000.0" boost-trans-exact="800.0" boost-fulltext="400.0"/>
<ns10:field name="real_name" index-type="REAL_NAME"
            boost-exact="300.0" boost-trans-exact="240.0" boost-fulltext="100.0"/>
```

- **Schema.** `ApCachedAccessPointBinder` declares the fields by templates (`data_*_ix`,
  `data_*_ix_trans`, `data_*_ix_analyzed`), not by name, so a field of a package imported after the
  start needs no restart. The item fields keep their explicit declaration from the item types
  present at the start.
- **Writing.** `ApCachedAccessPointBridge.addIndexFields` writes the index of the configured type
  into `data_<name>_ix` (the exact form), `_ix_trans` (the transliterated exact form) and
  `_ix_analyzed` (the value, tokenized by the classic analyzer). A name prefixed by `pref_` is
  written from the preferred part only, any other name from every part. The bridge holds the
  configuration in a static snapshot because indexing also runs outside a request (outbox
  polling, mass indexer); the snapshot is taken at the start and refreshed after every package
  import or removal (`PackageService` → `ApCachedAccessPointBridge.refreshSearchConfig`), because
  packages auto-imported at the start come after the initialisation of the bridge and their
  entities are regenerated right away.
- **Normal forms.** `IndexValueNormalizer`: the exact form is lower case, punctuation replaced by
  spaces, words sorted; the transliterated exact form folds the diacritics first. Both sides, the
  index value and the search text, go through the same form, so the match ignores case,
  punctuation and the order of the words, and the transliterated one the diacritics as well.
- **Query.** `ApCachedAccessPointRepositoryImpl.addFullTextBoost` adds, once per search text and
  for every field of an index type in the area (`SettingIndexSearch.indexTypeFields`), three
  `should` clauses: `match` of the exact form on `_ix` (`boost-exact`), of the transliterated
  exact form on `_ix_trans` (`boost-trans-exact`) and of the lower-cased text on `_ix_analyzed`
  (BM25, `boost-fulltext`). The fields take no part in the per-word clauses: their words are a
  subset of the display name, so they add no recall, and `boost` has no meaning for them
  (`searchedItemFields` skips them). In the area of the preferred names only the `pref_` fields
  apply.

Why both the exact and the BM25 clause: the exact clause puts the academy first for
"československá akademie věd" and the person first for "petr pytelka". The BM25 clause on a
two-word field keeps the plain person above `Pytelka, Petr Jan` for a text that is not the whole
name, and makes the institute lose to the academy by length normalization.

The weights of CZ_BASE are a starting point relative to `pref_nm_main` (exact 250, fulltext 200)
and `pref_index` (fulltext 800). BM25 and constant-score clauses live on different scales, so they
are tuned on real data with the trace log of `ApCachedAccessPointRepositoryImpl`.

## Rollout of a new index

An index added by a package reaches existing entities through the regeneration of the entities:
`compatibility-rul-package` of the changed `INDEX` rules in `rul_entity_rule.xml` set to the
version that ships the change makes `PackageService` queue every active entity of those classes
at the upgrade; the asynchronous generator rebuilds the indexes of each, validates it and refreshes
its cache, which reindexes it in Lucene. The run is long on a large registry and the ranking
improves as it proceeds. The admin `reindex` alone does not help: the cache lacks the index until
the entity is regenerated.

Entities received from CAM get their indexes rebuilt locally (`EntityDBDispatcher` →
`updatePartIndexes`), so the search works whether or not CAM adopts the index; the export XML
carries `REAL_NAME` as one more `eits` item.

## Rejected alternatives

- **Change `SHORT_NAME`** (drop the geographic supplement). It is a contract for institutions,
  statistics, exports and the CAM user template, and by R_UNQ_002 the supplement is what keeps
  names apart. It is not in Lucene anyway, so the change alone would not reach the search.
- **Hard-code `REAL_NAME` in the bridge and the query.** The core stays framework-agnostic
  (ISAAR_CPF, addons); a configured index type costs the same code and lets a framework choose.
- **Index every index type unconditionally.** Grows the index for values nobody searches
  (`DISPLAY_NAME_LOWER`, `SORT_NAME`, the `SHORT_NAME` of identifiers).
- **Whole-text exact match on item fields only.** No field holds the concatenation; Lucene cannot
  join `NM_MINOR` and `NM_MAIN` at query time.
- **Exact match sensitive to the order of the words.** It would make the word order of
  `REAL_NAME` a search decision between archivists and lay users; the sorted form serves both.

## Open work

- Tuning of the CZ_BASE weights on the MÚA registry with the #9575 queries ("československá
  akademie věd", "akademie věd české republiky", "akademie věd") and person queries in both word
  orders; the trace log shows the clauses and weights of a query.
- Alignment of the index and its form with the Kolegium CAM (technical section): the CZ_BASE
  rules are shared with CAM.
- ISAAR_CPF: not needed for the ranking (its display names carry no supplements). It may add the
  index, persons in natural order, and state its fields in its own `ui_setting.xml`.
- Ordinal and general supplement of rulers and events (`Karel IV.`, `Olympijské hry (23.)`) are
  dropped like other supplements; a form per class may follow if the search needs it.
