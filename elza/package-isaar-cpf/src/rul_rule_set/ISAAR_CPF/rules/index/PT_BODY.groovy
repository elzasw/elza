package scripts

import cz.tacr.elza.groovy.GroovyAppender
import cz.tacr.elza.groovy.GroovyItem
import cz.tacr.elza.groovy.GroovyPart
import cz.tacr.elza.groovy.GroovyResult
import cz.tacr.elza.groovy.GroovyUtils

return generate(PART)

// description: the abstract, else the beginning of the history
static GroovyResult generate(final GroovyPart part) {
    GroovyAppender base = GroovyUtils.createAppender(part)
    base.add("BRIEF_DESC")
    String text = base.build()
    if (text == null || text.isEmpty()) {
        GroovyAppender history = GroovyUtils.createAppender(part)
        history.add("HISTORY")
        text = history.build()
        if (text != null && text.length() > 250) {
            text = text.substring(0, 247) + "..."
        }
    }
    if (text == null || text.isEmpty()) {
        text = "Description"
    }
    GroovyResult result = new GroovyResult()
    result.setDisplayName(text)
    return result
}
