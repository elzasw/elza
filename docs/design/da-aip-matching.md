# Automatic placement of received AIPs (`DA_MATCH`)

How a received AIP is placed into the archival description when no UUID of the package matches a
node of the fund.

## 1. Decisions

1. **A UUID match is absolute.** `DA_MATCH` runs only when no UUID of the package matches a node of
   the fund; it never overrides a UUID match.
2. **Two scripts, one read model.** `DA_MATCH` decides *where* the package goes (once per package);
   `DA_IMPORT` decides *what each div becomes* (once per div, also on manual import). Placement is
   local policy an addon may override; the mapping belongs to the rules.
3. **Boundary.** A level that stands for a div of the package is made by `DA_IMPORT`; a level that
   does not exist in the package (a grouping, a fixed name) is made by `DA_MATCH`.
4. **Scripts write nothing.** Java carries out the chain and the `DA_IMPORT` plan in one transaction
   per AIP (`DaImportBuilder`); a failure is an `AipProblem` of the AIP and nothing is linked.
5. **Lookup scope:** only the chain from the fund root, each level matched among the children of
   the previous one. No fund-wide search; wider lookups, when needed, become new methods of the
   result object.
6. **Matching** by item type (`matchBy`) or by item type + specification (`matchBySpec`).

"Volné soubory" (ZP2015):

7. The unit is `ZP2015_LEVEL_ITEM`.
8. Chain: series **"Importováno"** → series named by `<container>`, or by the date of import when
   the package has none. The series are temporary working folders that help the user keep track of
   what was imported; several date series, and a duplicate unit when a package without UUIDs comes
   again on another day, are accepted.
9. **Not published until reviewed.** "Importováno" is created with a restriction of access
   `ZP2015_RESTRICTION_ACCESS_INLINE` (reason `ZP2015_ACCESS_PROC_STATE`, extent
   `ZP2015_RESTRICTION_ARCHDESC2` — hidden unit), which hides it and everything below it in the
   outputs. It is planned with `whenCreated()`: a series found by the match key does not get it
   again, so the user lifts it once after the review and no later package restores it.

## 2. Architecture

- **Script contract.** Rule type `RulArrangementRule.RuleType.DA_MATCH`, highest priority wins.
  Variables `PACKAGE` (`DaImportPackage`), `ROOT` (`DaImportLevel` of the top div, with
  `getChildren()`), `MATCH` (`DaMatchResult`). `MATCH.level()` returns a `DaMatchLevel` (same item /
  `matchBy` / `matchBySpec` API as `DaImportResult`, common base `DaLevelItems`); the chain ends with
  `importHere()`, `linkHere()` or `none()`. `DaMatchResult.validate()` requires an ending and a match
  key + items on every chain level.
- **Items planned by both scripts.** Base class of all item lists is `DaItems`. Items a level gets
  **only when it is created** are planned with `whenCreated()` (a `DaItemGroup`;
  `DaImportBuilder.enrich` never gives them to a found level). Items that are **new anonymous
  structured objects** are planned with `structuredItem(type)` and the items of the object; a
  shared structured type is refused (such an object would have to be found, not made), a
  structured type cannot be a match key, and an existing item of the type is kept without
  comparison.
- **Planning.** `DaImportPlanner.planPlacement` runs `DA_MATCH`, then plans the package with
  `DA_IMPORT` below the chain (`DaImportPlan.placement`; chain nodes have no div). A plan attaching
  nothing links the whole AIP to the last chain level (`attachWholeAip`).
- **Entry point.** `DaAipAutoLinkService.linkReceivedAip` calls
  `DaImportService.placeReceived(aip, fundRoot)` when the AIP offers no UUID or none matches.
- **Read model.** `DaImportTree` pairs divs with EAD units by id, including the `<archdesc>`;
  `<container>` is readable (`DaImportElement.getText()`).
- **ZP2015.** `DA_MATCH.groovy`: loose files → the chain of decision 8 ("Importováno" hidden when
  created, decision 9), other packages → `none()`. `DA_IMPORT.groovy`, branch `decideLooseFiles`:
  a `balicek` with its own description is the item, a `balicek` without one is skipped (its
  `slozka` is the item); `ZDROJ_ID` → `matchBySpec(ZP2015_OTHER_ID, SOURCEID)`, so a new version
  of the same unit is recognized without UUIDs.
- **Tests.** `Zp2015DaLooseFilesPlanTest`, `Zp2015DaLooseFilesPlacementTest` on samples in
  `elza-core/src/test/resources/zp2015/da-volne`: A (self-describing archdesc, non-UUID ids) and B
  (wrapper + `slozka` with `<container>`).

## Open work

- **A described `balicek` with a `slozka` below it** (not in the samples): both would become
  items, an item below an item. The rule is to be decided when such a package appears.
- **Alternative level types (item or folder).** The user can choose `ZP2015_LEVEL_FOLDER` instead
  of `ZP2015_LEVEL_ITEM`. Proposal modelled on the scenarios of a new level (`NewLevel.drl`:
  "Složka", "Jednotlivost pod sérii"):
  - `DA_IMPORT` may declare named alternatives of a level, e.g.
    `result.level().item(LEVEL_TYPE, "ZP2015_LEVEL_ITEM").alternative("Složka").item(LEVEL_TYPE, "ZP2015_LEVEL_FOLDER")`.
  - Automatic placement always takes the default (first) one.
  - The manual connect page (`AipConnectPanel` / `AipBulkConnectPanel`) offers the alternatives as
    a choice, the way the new-level dialog offers scenarios.
  - An imported level can be changed by ordinary editing of `ZP2015_LEVEL_TYPE`; validation reports
    what no longer fits.

  Variant to evaluate: take the alternatives straight from the `NEW_LEVEL` scenarios for the
  parent instead of declaring them in the script.
