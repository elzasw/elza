package scripts.PERSON

import cz.tacr.elza.groovy.GroovyAe
import cz.tacr.elza.groovy.GroovyPart
import cz.tacr.elza.groovy.GroovyItem

return generate(AE)

static List<GroovyItem> generate(final GroovyAe ae) {
    List<GroovyItem> items = new ArrayList<>()
    for (GroovyPart part : ae.getParts()) {
        for (GroovyItem item : part.getItems()) {
            items.add(item)
        }
    }

    return items
}