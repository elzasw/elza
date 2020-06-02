package scripts

import cz.tacr.elza.groovy.GroovyAppender
import cz.tacr.elza.groovy.GroovyPart
import cz.tacr.elza.groovy.GroovyResult
import cz.tacr.elza.groovy.GroovyUtils

return generate(PART)

static GroovyResult generate(final GroovyPart part) {
    GroovyAppender base = GroovyUtils.createAppender(part)
    base.add("REL_ENTITY").withSpec()
    base.add("REL_BEGIN").withSeparator(", ").withPrefix("od: ")
    base.add("REL_END").withSeparator(", ").withPrefix("do: ")
    GroovyResult result = new GroovyResult()
    result.setDisplayName(base.build())
    
    GroovyAppender shortName = GroovyUtils.createAppender(part)
    shortName.addInt("REL_ENTITY").withSpec()
    shortName.add("REL_BEGIN").withSeparator(", ").withPrefix("od: ")
    shortName.add("REL_END").withSeparator(", ").withPrefix("do: ")
    
    result.addIndex("SHORT_NAME", shortName.build().toLowerCase());  
    
    return result
}
