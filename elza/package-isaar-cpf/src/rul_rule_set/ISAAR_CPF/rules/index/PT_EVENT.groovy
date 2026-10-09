package scripts

import cz.tacr.elza.groovy.GroovyAppender
import cz.tacr.elza.groovy.GroovyItem
import cz.tacr.elza.groovy.GroovyPart
import cz.tacr.elza.groovy.GroovyResult
import cz.tacr.elza.groovy.GroovyUtils

return generate(PART)

// event: kind, dates and the related entities of the child parts
static GroovyResult generate(final GroovyPart part) {
    GroovyAppender base = GroovyUtils.createAppender(part)
    base.add("EV_TYPE")
    base.add("ISAAR_LEGAL_STATUS").withSeparator(": ")
    base.add("EV_BEGIN").withSeparator(", ").withPrefix("from: ")
    base.add("EV_END").withSeparator(", ").withPrefix("to: ")

    GroovyAppender sort = GroovyUtils.createAppender(part)
    sort.addUnitdateFrom("EV_BEGIN")
    sort.addUnitdateTo("EV_END")
    sort.addViewOrder("EV_TYPE")

    GroovyAppender rels = GroovyUtils.createAppender(part)
    def children = part.getChildren()
    if (children != null) {
        for (GroovyPart childPart : children) {
            GroovyAppender childBase = GroovyUtils.createAppender(childPart)
            childBase.add("REL_ENTITY").withSpec()
            rels.addStr(childBase.build()).withSeparator(", ")
        }
    }
    base.addStr(rels.build()).withSeparator(" ").withPrefix("(").withPostfix(")")

    GroovyResult result = new GroovyResult()
    result.setDisplayName(base.build())
    result.setSortName(sort.build())
    return result
}
