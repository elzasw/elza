package ZP2015

import java.time.LocalDate

import cz.tacr.elza.service.da.DaImportLevel
import cz.tacr.elza.service.da.DaImportPackage
import cz.tacr.elza.service.da.DaMatchResult

/*
 * Placement of a received package of the digital archive that matches no unit of description by
 * UUID (ZP2015). Called once for the package:
 *   PACKAGE - DaImportPackage: content type, profile
 *   ROOT    - DaImportLevel: the top div of the logical structural map, with the divs below it
 *   MATCH   - DaMatchResult: the chain of levels below the root of the fund - level(), each found
 *             by its matchBy or created - ended by importHere(), linkHere() or none()
 *
 * The levels of the package itself are planned by the DA_IMPORT script below the chain.
 */

@groovy.transform.Field
static final String LEVEL_TYPE = "ZP2015_LEVEL_TYPE"

/** Content type of the packages of loose files. */
@groovy.transform.Field
static final String LOOSE_FILES = "Volné soubory"

/** Name of the series collecting the packages placed automatically. */
@groovy.transform.Field
static final String IMPORTED = "Importováno"

place(PACKAGE, ROOT, MATCH)

static void place(DaImportPackage pkg, DaImportLevel root, DaMatchResult match) {
    if (pkg.contentType != LOOSE_FILES || root == null) {
        match.none()
        return
    }
    // Packages of loose files are collected in the series "Importováno", in a series per storage
    // unit (<container>), or per day of import when the package names none - the series help the
    // user keep track of what was imported and are rearranged later.
    series(match, IMPORTED)
    series(match, container(root) ?: LocalDate.now().toString())
    match.importHere()
}

static void series(DaMatchResult match, String name) {
    match.level()
         .item(LEVEL_TYPE, "ZP2015_LEVEL_SERIES")
         .item("ZP2015_NAME", null, name)
         .matchBy(LEVEL_TYPE, "ZP2015_NAME")
}

/** Text of the first <container> of the package, top-down; null when it has none. */
static String container(DaImportLevel level) {
    String text = level.elements.find { it.name == "container" }?.text
    if (text) {
        return text
    }
    for (DaImportLevel child : level.children) {
        text = container(child)
        if (text) {
            return text
        }
    }
    return null
}
