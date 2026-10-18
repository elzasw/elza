package scripts

import cz.tacr.elza.groovy.GroovyAppender
import cz.tacr.elza.groovy.GroovyPart
import cz.tacr.elza.groovy.GroovyResult
import cz.tacr.elza.groovy.GroovyUtils

return generate(PART)

// catalogue form "Main, Other (supplement)"; the common name (REAL_NAME) "Other Main" without the supplement
static GroovyResult generate(final GroovyPart part) {
    GroovyAppender base = GroovyUtils.createAppender(part)
    base.add("NM_MAIN")
    base.add("STD_MINOR").withSeparator(", ")
    base.add("STD_SUP").withSeparator(" ").withPrefix("(").withPostfix(")")
    String name = base.build()
    GroovyResult result = new GroovyResult()
    result.setDisplayName(name)
    result.setSortName(name)
    if (part.isPreferred()) {
        result.setPtPreferName(name)
        result.addIndex(GroovyResult.SHORT_NAME, name.take(3))
    }
    GroovyAppender realName = GroovyUtils.createAppender(part)
    realName.add("STD_MINOR")
    realName.add("NM_MAIN").withSeparator(" ")
    result.addIndex(GroovyResult.REAL_NAME, realName.build())
    return result
}
