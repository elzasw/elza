package scripts

import cz.tacr.elza.groovy.GroovyAppender
import cz.tacr.elza.groovy.GroovyItem
import cz.tacr.elza.groovy.GroovyPart
import cz.tacr.elza.groovy.GroovyResult
import cz.tacr.elza.groovy.GroovyUtils

return generate(PART)

// relationship: the related entity with the kind of relationship and dates
static GroovyResult generate(final GroovyPart part) {
    GroovyAppender base = GroovyUtils.createAppender(part)
    base.add("REL_ENTITY").withSpec()
    base.add("REL_BEGIN").withSeparator(", ").withPrefix("from: ")
    base.add("REL_END").withSeparator(", ").withPrefix("to: ")

    GroovyAppender sort = GroovyUtils.createAppender(part)
    sort.addViewOrder("REL_ENTITY")
    sort.addUnitdateFrom("REL_BEGIN")
    sort.addUnitdateTo("REL_END")
    sort.add("REL_ENTITY")

    GroovyResult result = new GroovyResult()
    result.setDisplayName(base.build())
    result.setSortName(sort.build())
    return result
}
