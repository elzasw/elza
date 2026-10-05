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

/*
 * Kinds of the levels of the NSESSS packages. The level is recognized by the TYPE of the div of
 * the logical structural map (METS) - the package itself says what its parts are; the TYPE is
 * accepted as a code (vecnaskp) or a readable name ("věcná skupina"). The inherent archival
 * description (EAD, its otherlevel) comes from the originator and is only supporting evidence,
 * used when the div says nothing or something unknown.
 */

/** Kinds of level that stand for the file plan the package comes from. */
@groovy.transform.Field
static final Set<String> FILEPLAN_KINDS = ["spisplan", "spisový plán"] as Set

/** Kinds of level that are not levels but files attached to the level above them. */
@groovy.transform.Field
static final Set<String> ATTACHED_KINDS = ["komponenta"] as Set

/** Level types by the kind of level - the EAD otherlevel code or the TYPE of the div. */
@groovy.transform.Field
static final Map<String, String> LEVEL_BY_KIND = [
        "vecnaskp"     : "ZP2015_LEVEL_SERIES",
        "věcná skupina": "ZP2015_LEVEL_SERIES",
        "spis"         : "ZP2015_LEVEL_FOLDER",
        "dil"          : "ZP2015_LEVEL_FOLDER",
        "díl"          : "ZP2015_LEVEL_FOLDER",
        "díl spisu"    : "ZP2015_LEVEL_FOLDER",
        "dokument"     : "ZP2015_LEVEL_ITEM"
]

/** Level types by the standard level of the EAD unit. */
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

/**
 * Content type of the packages of loose files: no file plan, the package is one unit of
 * description (or a wrapper of one), see decideLooseFiles.
 */
@groovy.transform.Field
static final String LOOSE_FILES = "Volné soubory"

decide(PACKAGE, LEVEL, RESULT)

static void decide(DaImportPackage pkg, DaImportLevel level, DaImportResult result) {
    if (pkg.contentType == LOOSE_FILES) {
        decideLooseFiles(level, result)
        return
    }
    String eadKind = eadLevel(level)
    String divKind = level.divType?.trim()?.toLowerCase()

    // The file plan changes in some offices so often that it cannot identify anything; unless
    // asked for, only the hierarchy below it is imported. It is recognized by the kind of its
    // div, or by the id of its <fileplan> in the EAD.
    if (divKind in FILEPLAN_KINDS || level.fileplan || (!isKnown(divKind) && eadKind in FILEPLAN_KINDS)) {
        if (pkg.fileplanAsRoot) {
            result.level()
                  .item(LEVEL_TYPE, "ZP2015_LEVEL_SERIES")
                  .item("ZP2015_NAME", null, level.label)
                  .matchBy(LEVEL_TYPE, "ZP2015_NAME")
        } else {
            result.skip()
        }
        return
    }
    if (divKind in ATTACHED_KINDS || (!isKnown(divKind) && eadKind in ATTACHED_KINDS)) {
        result.attach()
        return
    }

    String levelType = LEVEL_BY_KIND[divKind] ?: LEVEL_BY_KIND[eadKind] ?: LEVEL_BY_EAD_LEVEL[eadKind]
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
        result.matchBy(LEVEL_TYPE, "ZP2015_NAME")
    }
}

/*
 * A package of loose files describes one unit: either the package itself (the top div is the
 * <archdesc> with the title and the identifiers), or a single div below it (e.g. a "slozka")
 * when the top div is only a wrapper without a description of its own. The unit is an item;
 * the components are attached to it.
 */
static void decideLooseFiles(DaImportLevel level, DaImportResult result) {
    String kind = level.divType?.trim()?.toLowerCase() ?: eadLevel(level)
    if (kind in ATTACHED_KINDS) {
        result.attach()
        return
    }
    if (kind == "balicek" && !describes(level)) {
        result.skip()
        return
    }
    result.level().item(LEVEL_TYPE, "ZP2015_LEVEL_ITEM")
    boolean named = false
    for (DaImportElement element : level.elements) {
        named |= addItem(result, element)
    }
    if (!named) {
        result.item("ZP2015_NAME", null, level.label)
    }
    // the same unit coming again (a new version of the package) is recognized by its id in the
    // source system - the divs of these packages need not carry UUIDs
    if (level.elements.any { it.name == "unitid" && it.localType == "ZDROJ_ID" }) {
        result.matchBySpec("ZP2015_OTHER_ID", OTHER_ID_SPECS["ZDROJ_ID"])
    }
}

/** Whether the unit has a description of its own - something taken over as an item. */
static boolean describes(DaImportLevel level) {
    return level.elements.any {
        it.name in ["unittitle", "abstract", "unitdatestructured"] || (it.name == "unitid" && OTHER_ID_SPECS[it.localType] != null)
    }
}

/** Whether the kind of level names something this script knows. */
static boolean isKnown(String kind) {
    return kind in FILEPLAN_KINDS || kind in ATTACHED_KINDS || LEVEL_BY_KIND.containsKey(kind)
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
