package scripts

import cz.tacr.elza.groovy.GroovyAppender
import cz.tacr.elza.groovy.GroovyPart
import cz.tacr.elza.groovy.GroovyResult
import cz.tacr.elza.groovy.GroovyUtils

return generate(PART)

static GroovyResult generate(final GroovyPart part) {
    GroovyAppender base = GroovyUtils.createAppender(part)
    base.add("EV_TYPE")
    base.add("EV_BEGIN").withSeparator(", ").withPrefix("od: ")
    base.add("EV_END").withSeparator(", ").withPrefix("do: ")

    GroovyAppender sort = GroovyUtils.createAppender(part)
    sort.add("EV_TYPE")
    sort.addUnitdateFrom("EV_BEGIN")
    sort.addUnitdateTo("EV_END")

    GroovyResult result = new GroovyResult()
    result.setDisplayName(base.build())
    result.setSortName(sort.build())

    def children = part.getChildren()
    GroovyAppender rels = GroovyUtils.createAppender(part)
    if (children != null) {
        for (GroovyPart childPart : children) {
            GroovyAppender childBase = GroovyUtils.createAppender(childPart)
            childBase.add("REL_ENTITY").withSpec()
            rels.addStr(childBase.build()).withSeparator(", ")
        }
    }
    base.addStr(rels.build()).withSeparator(" ").withPrefix("(").withPostfix(")")
    result.addIndex("DISPLAY_NAME_LOWER", base.build())

    return result
}
