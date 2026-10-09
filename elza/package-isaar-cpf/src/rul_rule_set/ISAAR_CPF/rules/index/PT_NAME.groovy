package scripts

import cz.tacr.elza.groovy.GroovyAppender
import cz.tacr.elza.groovy.GroovyItem
import cz.tacr.elza.groovy.GroovyPart
import cz.tacr.elza.groovy.GroovyResult
import cz.tacr.elza.groovy.GroovyUtils

return generate(PART)

// name of a person, family, corporate body, place or concept: main part, other part
static GroovyResult generate(final GroovyPart part) {
    GroovyAppender base = GroovyUtils.createAppender(part)
    base.add("NM_MAIN")
    base.add("NM_MINOR").withSeparator(", ")

    GroovyResult result = new GroovyResult()
    String name = base.build()
    result.setDisplayName(name)
    result.setSortName(name)
    if (part.isPreferred()) {
        result.setPtPreferName(name)
    }
    GroovyAppender shortName = GroovyUtils.createAppender(part)
    shortName.add("NM_MAIN")
    result.addIndex("SHORT_NAME", shortName.build())
    return result
}
