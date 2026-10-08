package scripts

import cz.tacr.elza.groovy.GroovyAppender
import cz.tacr.elza.groovy.GroovyItem
import cz.tacr.elza.groovy.GroovyPart
import cz.tacr.elza.groovy.GroovyResult
import cz.tacr.elza.groovy.GroovyUtils

return generate(PART)

// identifier: type and value; the pair is unique in the scope
static GroovyResult generate(final GroovyPart part) {
    GroovyAppender base = GroovyUtils.createAppender(part)
    base.add("IDN_TYPE")
    base.add("IDN_VALUE").withSeparator(": ")
    base.add("IDN_VALID_FROM").withSeparator(", ").withPrefix("valid from: ")
    base.add("IDN_VALID_TO").withSeparator(", ").withPrefix("valid to: ")

    GroovyAppender sort = GroovyUtils.createAppender(part)
    sort.addViewOrder("IDN_TYPE")
    sort.add("IDN_VALUE")

    GroovyResult result = new GroovyResult()
    result.setDisplayName(base.build())
    result.setSortName(sort.build())

    GroovyAppender shortName = GroovyUtils.createAppender(part)
    shortName.add("IDN_TYPE")
    shortName.add("IDN_VALUE").withSeparator(": ")
    String key = shortName.build()
    result.addIndex("SHORT_NAME", key.toLowerCase())
    result.setKeyValue("PT_IDENT", key)
    return result
}
