package scripts

import cz.tacr.elza.groovy.GroovyAppender
import cz.tacr.elza.groovy.GroovyPart
import cz.tacr.elza.groovy.GroovyResult
import cz.tacr.elza.groovy.GroovyUtils

return generate(PART)

static GroovyResult generate(final GroovyPart part) {
    GroovyAppender base = GroovyUtils.createAppender(part)
    base.add("NM_MAIN")
    String name = base.build()
    GroovyResult result = new GroovyResult()
    result.setDisplayName(name)
    result.setSortName(name)
    if (part.isPreferred()) {
        result.setPtPreferName(name)
        result.addIndex(GroovyResult.SHORT_NAME, name.take(3))
    }
    return result
}
