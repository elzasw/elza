package scripts

import cz.tacr.elza.groovy.GroovyAppender
import cz.tacr.elza.groovy.GroovyPart
import cz.tacr.elza.groovy.GroovyResult
import cz.tacr.elza.groovy.GroovyUtils

return generate(PART)

static GroovyResult generate(final GroovyPart part) {

    def children = part.getChildren()
    GroovyAppender rels = GroovyUtils.createAppender(part)
    if (children != null) {
        for (GroovyPart childPart : children) {
            GroovyAppender childBase = GroovyUtils.createAppender(childPart)
            childBase.add("REL_ENTITY").withSpec()
            rels.addStr(childBase.build()).withSeparator(", ")
        }
    }

    GroovyAppender base = GroovyUtils.createAppender(part)
    base.add("EV_TYPE")
    base.add("EV_BEGIN").withSeparator(", ").withPrefix("od: ")
    base.add("EV_END").withSeparator(", ").withPrefix("do: ")
    base.addStr(rels.build()).withSeparator(" ").withPrefix("(").withPostfix(")")

    GroovyResult result = new GroovyResult()
    result.setDisplayName(base.build())
    return result
}
