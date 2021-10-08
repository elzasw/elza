package scripts
import cz.tacr.elza.groovy.GroovyAppender
import cz.tacr.elza.groovy.GroovyItem
import cz.tacr.elza.groovy.GroovyPart
import cz.tacr.elza.groovy.GroovyResult
import cz.tacr.elza.groovy.GroovyUtils

return generate(PART)

static GroovyResult generate(final GroovyPart part) {
    GroovyAppender base = GroovyUtils.createAppender(part)
    base.add("IDN_TYPE")
    base.add("IDN_VALUE").withSeparator(": ")
    base.add("IDN_VALID_FROM").withSeparator(", ").withPrefix("platnost od: ")
    base.add("IDN_VALID_TO").withSeparator(", ").withPrefix("platnost do: ")
    base.addBool("IDN_VERIFIED", "ověřena", "neověřena").withSeparator(" ").withPrefix("(hodnota ").withPostfix(")")

    GroovyAppender sort = GroovyUtils.createAppender(part)
    sort.addViewOrder("IDN_TYPE")
    sort.addUnitdateFrom("IDN_VALID_FROM")
    sort.addUnitdateTo("IDN_VALID_TO")
    sort.add("IDN_VALUE")

    GroovyResult result = new GroovyResult()
    result.setDisplayName(base.build())
    result.setSortName(sort.build())

    GroovyAppender shortName = GroovyUtils.createAppender(part)
    shortName.add("IDN_TYPE")
    shortName.add("IDN_VALUE").withSeparator(": ")
    
    def shortn = shortName.build()
    result.addIndex("SHORT_NAME", shortn.toLowerCase())

    def addIdent = false
    for (GroovyItem item : part.getItems("IDN_TYPE")) {
        if (item.spec != null && ( item.spec.equals("ISO 3166-2") ||  item.spec.equals("Kód CZ_RETRO") || item.spec.equals("NUTS/LAU") || item.spec.equals("ISO 3166-1 numeric") || item.spec.equals("ISO 3166-1 alpha-3") || item.spec.equals("ISO 3166-1 alpha-2") || item.spec.equals("číslo archivu")  || item.spec.equals("ORCID")  || item.spec.equals("Kód archivní taxonomické kategorie")) ) {
            addIdent = true
            break
        }
    }

    if (addIdent) {
        result.setKeyValue("PT_IDENT", shortn);
    }
    return result
}
