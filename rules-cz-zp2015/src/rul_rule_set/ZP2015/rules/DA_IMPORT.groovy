package ZP2015

import cz.tacr.elza.service.da.DaImportElement
import cz.tacr.elza.service.da.DaImportLevel
import cz.tacr.elza.service.da.DaImportPackage
import cz.tacr.elza.service.da.DaImportResult

/*
 * Import of a package of the digital archive into the archival description (ZP2015).
 *
 * Called for every div of the logical structural map of the package, top-down:
 *   PACKAGE - DaImportPackage: content type, profile, options of the import
 *   LEVEL   - DaImportLevel: the div and the unit of description of the EAD it stands for
 *   RESULT  - DaImportResult: the decision - level(), attach() or skip() - and the items
 *
 * The mappings are the inverse of the EAD export of these rules (template INVENTAR_EAD).
 */

@groovy.transform.Field
static final String LEVEL_TYPE = "ZP2015_LEVEL_TYPE"

/** Level types by the TYPE of the div, for the kinds of packages that name their levels (NSESSS). */
@groovy.transform.Field
static final Map<String, String> LEVEL_BY_DIV_TYPE = [
        "vecnaskp" : "ZP2015_LEVEL_SERIES",
        "spis"     : "ZP2015_LEVEL_FOLDER",
        "dil"      : "ZP2015_LEVEL_FOLDER",
        "dokument" : "ZP2015_LEVEL_ITEM"
]

/** TYPE of the divs that are not levels but files attached to the level above them. */
@groovy.transform.Field
static final Set<String> ATTACHED_DIV_TYPES = ["komponenta"] as Set

/** Level types by the level of the EAD unit. */
@groovy.transform.Field
static final Map<String, String> LEVEL_BY_EAD_LEVEL = [
        "subfonds" : "ZP2015_LEVEL_SECTION",
        "series"   : "ZP2015_LEVEL_SERIES",
        "subseries": "ZP2015_LEVEL_SERIES",
        "file"     : "ZP2015_LEVEL_FOLDER",
        "item"     : "ZP2015_LEVEL_ITEM",
        "itempart" : "ZP2015_LEVEL_PART"
]

/** Local types of the other identifiers (unitid). */
@groovy.transform.Field
static final Map<String, String> OTHER_ID_SPECS = [
        "SIGNATURA_PUVODNI"        : "ZP2015_OTHERID_SIG_ORIG",
        "SIGNATURA_ZPRACOVANI"     : "ZP2015_OTHERID_SIG",
        "UKLADACI_ZNAK"            : "ZP2015_OTHERID_STORAGE_ID",
        "CISLO_JEDNACI"            : "ZP2015_OTHERID_CJ",
        "SPISOVA_ZNACKA"           : "ZP2015_OTHERID_DOCID",
        "CISLO_VLOZKY"             : "ZP2015_OTHERID_FORMAL_DOCID",
        "CISLO_PRIRUSTKOVE"        : "ZP2015_OTHERID_ADDID",
        "NEPL_SIGNATURA_ZPRACOVANI": "ZP2015_OTHERID_OLDSIG",
        "NEPL_INV_CISLO"           : "ZP2015_OTHERID_OLDID",
        "NEPL_REFERENCNI_OZNACENI" : "ZP2015_OTHERID_INVALID_REFNO",
        "NEPL_PORADOVE_CISLO"      : "ZP2015_OTHERID_PRINTID",
        "NAKL_CISLO"               : "ZP2015_OTHERID_PICID",
        "CISLO_NEGATIVU"           : "ZP2015_OTHERID_NEGID",
        "CISLO_PRODUKCE"           : "ZP2015_OTHERID_CDID",
        "KOD_ISBN"                 : "ZP2015_OTHERID_ISBN",
        "KOD_ISSN"                 : "ZP2015_OTHERID_ISSN",
        "KOD_ISMN"                 : "ZP2015_OTHERID_ISMN",
        "ZDROJ_ID"                 : "ZP2015_OTHERID_SOURCEID",
        "MATRICNI_CISLO"           : "ZP2015_OTHERID_MATRIXID"
]

/**
 * Local types of the other dates of a unit of description, see
 * https://stands.nacr.cz/ead/current/rozsireny-popis/jina-datace.html
 */
@groovy.transform.Field
static final Map<String, String> DATE_OTHER_SPECS = [
        "CONTENT"               : "ZP2015_DATE_OF_CONTENT",
        "DECLARED"              : "ZP2015_DATE_DECLARED",
        "ORIGIN"                : "ZP2015_DATE_ORIG",
        "COPY"                  : "ZP2015_DATE_OF_COPY",
        "SEALING"               : "ZP2015_DATE_SEALING",
        "ACT_PUBLISHING"        : "ZP2015_DATE_ACT_PUBLISHING",
        "INSERT"                : "ZP2015_DATE_INSERT",
        "MOLD_CREATION"         : "ZP2015_DATE_MOLD_CREATION",
        "USAGE"                 : "ZP2015_DATE_USAGE",
        "PUBLISHING"            : "ZP2015_DATE_PUBLISHING",
        "MAP_UPDATE"            : "ZP2015_DATE_MAP_UPDATE",
        "CAPTURING"             : "ZP2015_DATE_CAPTURING",
        "RECORDING"             : "ZP2015_DATE_RECORDING",
        "AWARDING"              : "ZP2015_DATE_AWARDING",
        "AWARD_CER"             : "ZP2015_DATE_AWARD_CER",
        "WITHDRAWAL"            : "ZP2015_DATE_WITHDRAWAL",
        "LEGALLY_EFFECTIVE_FROM": "ZP2015_DATE_LEGALLY_EFFECTIVE_FROM",
        "VALID_FROM"            : "ZP2015_DATE_VALID_FROM",
        "LEGALLY_EFFECTIVE_TO"  : "ZP2015_DATE_LEGALLY_EFFECTIVE_TO",
        "VALID_TO"              : "ZP2015_DATE_VALID_TO",
        "DISPATCH"              : "ZP2015_DATE_DISPATCH",
        "DELIVERY"              : "ZP2015_DATE_DELIVERY",
        "PROCESSING"            : "ZP2015_DATE_PROCESSING",
        "CLOSING"               : "ZP2015_DATE_CLOSING"
]

decide(PACKAGE, LEVEL, RESULT)

static void decide(DaImportPackage pkg, DaImportLevel level, DaImportResult result) {
    // The file plan changes in some offices so often that it cannot identify anything; unless
    // asked for, only the hierarchy below it is imported.
    if (level.fileplan) {
        if (pkg.fileplanAsRoot) {
            result.level()
                  .item(LEVEL_TYPE, "ZP2015_LEVEL_SERIES")
                  .item("ZP2015_NAME", null, level.label)
                  .matchKey(level.label)
        } else {
            result.skip()
        }
        return
    }
    if (level.divType in ATTACHED_DIV_TYPES) {
        result.attach()
        return
    }

    String levelType = LEVEL_BY_DIV_TYPE[level.divType] ?: LEVEL_BY_EAD_LEVEL[eadLevel(level)]
    if (levelType == null) {
        throw new IllegalStateException("Nelze určit úroveň popisu pro " + level
                + " (level=" + level.eadLevel + ", otherlevel=" + level.eadOtherLevel + ")")
    }
    result.level().item(LEVEL_TYPE, levelType)

    boolean named = false
    for (DaImportElement element : level.elements) {
        named |= addItem(result, element)
    }
    if (!named) {
        result.item("ZP2015_NAME", null, level.label)
    }
    // groups of the file plan are shared by the packages coming from it - recognized by name
    if (levelType == "ZP2015_LEVEL_SERIES") {
        result.matchKey(level.label)
    }
}

static String eadLevel(DaImportLevel level) {
    return level.eadLevel == "otherlevel" ? level.eadOtherLevel : level.eadLevel
}

/**
 * @return whether the element gave the level its name
 */
static boolean addItem(DaImportResult result, DaImportElement element) {
    String localType = element.localType
    switch (element.name) {
        case "unittitle":
            if (localType == null) {
                result.item("ZP2015_NAME", null, element)
                return true
            }
            if (localType == "FORMAL_TITLE") {
                result.item("ZP2015_FORMAL_TITLE", null, element)
            }
            break
        case "abstract":
            result.item("ZP2015_CONTENT", null, element)
            break
        case "unitdatestructured":
            if (localType == null) {
                result.item("ZP2015_UNIT_DATE", null, element)
            } else if (DATE_OTHER_SPECS[localType] != null) {
                result.item("ZP2015_DATE_OTHER", DATE_OTHER_SPECS[localType], element)
            }
            break
        case "unitid":
            if (OTHER_ID_SPECS[localType] != null) {
                result.item("ZP2015_OTHER_ID", OTHER_ID_SPECS[localType], element)
            }
            break
    }
    return false
}
