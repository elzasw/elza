package scripts

import cz.tacr.elza.groovy.GroovyAppender
import cz.tacr.elza.groovy.GroovyItem
import cz.tacr.elza.groovy.GroovyPart
import cz.tacr.elza.groovy.GroovyResult
import cz.tacr.elza.groovy.GroovyUtils

return generate(PART)

// end of existence: kind, date and the related entities of the child parts
static GroovyResult generate(final GroovyPart part) {
    GroovyAppender rels = GroovyUtils.createAppender(part)
    def children = part.getChildren()
    if (children != null) {
        for (GroovyPart childPart : children) {
            GroovyAppender childBase = GroovyUtils.createAppender(childPart)
            childBase.add("REL_ENTITY").withSpec()
            rels.addStr(childBase.build()).withSeparator(", ")
        }
    }
    GroovyAppender base = GroovyUtils.createAppender(part)
    base.add("EXT_CLASS")
    base.add("EXT_DATE").withSeparator(", ")
    base.addStr(rels.build()).withSeparator(" ").withPrefix("(").withPostfix(")")

    GroovyResult result = new GroovyResult()
    String text = base.build()
    result.setDisplayName(text == null || text.isEmpty() ? "End of existence" : text)
    return result
}
