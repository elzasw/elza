# New version of an AIP — cases and behaviour

What ELZA does when the digital archive (DA) reports a change of an AIP that ELZA already knows,
and in particular what happens to its links to the archival description.

## How a new version is processed

1. The synchronisation (`DaService.processUpdates`) queues the AIP for download. It asks for the
   same form of the package that ELZA holds: PACKAGE-INFO only, the metadata, or the complete AIP.
2. PACKAGE-INFO (`PackageInfoService`) closes the previous `da_aip_state` and creates a new one
   (new version, size, codes of the institution and the fund).
3. With metadata, the digital entities (DAO) are rebuilt from METS (`DaoProcessor`). An entity of
   the previous version is kept when its code and type are the same; what the new version no
   longer contains is closed.
4. A repository with automatic metadata download places an AIP that has no link (match by UUID,
   then the `DA_MATCH` rules, see `da-aip-matching.md`). An AIP that has a link is left where it is.

Links are closed by `DaLinkCloser`: one `arr_change` of type `DELETE_DAO_LINK` per unit of
description, with that unit as its primary node, plus the `DAO_LINK_DELETE` event, so the history
of the unit and the open clients see the removal.

## Cases

### A. Package as a whole

| # | Case | Behaviour |
|---|---|---|
| A1 | New AIP (unknown code) | Imported; in an automatic repository its metadata are requested and it is placed. |
| A2 | Same version reported again (e.g. full synchronisation) | Processed like a new version: a new state row, the rebuild changes nothing, links stay. |
| A3 | AIP invalidated in the DA | All links closed, entities and state closed, downloaded packages deleted, waiting requests withdrawn. |
| A4 | Valid version after an invalidation | Imported as a new AIP; the former links are not restored. |
| A5 | Processing of the new version fails (broken METS, …) | The problem is recorded on the AIP; entities and links of the previous version stay. The state shows the new `aip_version`, `aip_version_metadata` stays at the old one. |

### B. Fund and institution

| # | Case | Behaviour |
|---|---|---|
| B1 | Same fund and institution codes as before | The fund of the previous state is kept, even when a lookup by the code would now give another answer (renumbered fund, two funds with the same number). Links stay. |
| B2 | Another fund that ELZA knows | Links outside the new fund are closed. In an automatic repository the AIP is then placed into the new fund like a newly received one (its metadata are requested if needed). Otherwise the user attaches it. |
| B3 | A fund that ELZA does not know | Links are closed; the AIP shows the problem *unknown fund*. After the fund is created: *Remap institution and fund*. |
| B4 | Fund not known before, known now | The AIP gets the fund; in an automatic repository its metadata are requested and it is placed. |
| B5 | Same fund code, another institution | Another fund: the fund is looked up under the new institution. A fund found only by its internal code is not used when it belongs to another institution, and the previous fund is not found again when the new institution is unknown to ELZA. Then as B2 or B3. |
| B6 | The package names the institution of its fund for the first time | The fund stays; links stay. |

### C. Content of the package (links of parts)

A link either attaches the whole package (`da_dao_id IS NULL`) or one part of it (a level of the
logical structure, a representation, a file).

| # | Case | Behaviour |
|---|---|---|
| C1 | Nothing changed | Links stay. |
| C2 | Content added (file, representation, metadata file, level) | Links stay. Link state is recomputed: a link of the whole package stays *fully linked*; links of parts give *partially linked* when the new content is not under an attached part. The package browser shows only the content of the current version. |
| C3 | Linked part removed | The link of that part is closed by a change of its unit; the other links stay; link state recomputed. If it was the last link, an automatic repository places the AIP again. |
| C4 | Linked part renamed (label of the level, original file name) | The entity is kept under the new label; the link stays. |
| C5 | Part moved under another parent | The entity is kept, its relations are rebuilt; the link stays; link state recomputed. |
| C6 | Part kept its code but changed its type | Treated as removed + new (C3). |
| C7 | File content changed (checksum, size) | The file record is replaced under the same entity; the link stays. |

### D. Description (EAD) in the package

| # | Case | Behaviour |
|---|---|---|
| D1 | Description of a linked AIP changed | Nothing in the archival description changes. ELZA cannot tell whether the levels and items were taken over from the package or written by the archivist, so it does not overwrite them. Taking over the description again is a user action. |
| D2 | AIP without a link, automatic repository | Placed again with every new version (match by UUID, then `DA_MATCH`). |

### E. Related behaviour

| # | Case | Behaviour |
|---|---|---|
| E1 | Link state of the new state | Recomputed when PACKAGE-INFO is processed, so it is right before any metadata rebuild. |
| E2 | *Forced update* (`FORCE_UPDATE`) | Same as *Database update only*: removed parts are detached in both. The option is not offered in the UI; the API value stays for compatibility. |
| E3 | Metadata deleted by the user, then a new version arrives (automatic repository) | The metadata are downloaded again (A1 / B4 rule: an automatic repository asks for metadata of every received PACKAGE-INFO). |

## Open work

- **D1 — taking over a changed description.** Doing it automatically needs to know which levels
  and items came from the package and were not edited since. A possible way is to record the
  origin on the import (`DaImportBuilder` knows the div each level was made from) and compare the
  current values with the imported ones; until then the user re-imports.
- **A2 — state rows for unchanged versions.** Harmless, but the history of the AIP grows with
  every full synchronisation. Skipping PACKAGE-INFO of an unchanged version would avoid it.
