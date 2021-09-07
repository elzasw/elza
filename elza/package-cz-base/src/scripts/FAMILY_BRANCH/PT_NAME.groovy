package scripts.FAMILY_BRANCH

import cz.tacr.elza.groovy.GroovyAppender
import cz.tacr.elza.groovy.GroovyPart
import cz.tacr.elza.groovy.GroovyResult
import cz.tacr.elza.groovy.GroovyUtils

return generate(PART)

static GroovyResult generate(final GroovyPart part) {
    GroovyAppender compl = GroovyUtils.createAppender(part)
    if (part.isPreferred()) {
        compl.addStr("větev rodu")
    }
    compl.add("NM_SUP_CHRO").withSeparator(" : ")

    GroovyAppender base = GroovyUtils.createAppender(part)
    base.add("NM_MAIN")
    base.add("NM_MINOR").withSeparator(". ")
    base.addStr(compl.build()).withSeparator(" ").withPrefix("(").withPostfix(")")

    GroovyResult result = new GroovyResult()
    def r = base.build();
    result.setDisplayName(r)
    if (part.isPreferred()) {
        result.setPtPreferName(r)
    }

    GroovyAppender shortName = GroovyUtils.createAppender(part)
    shortName.add("NM_MAIN")
    shortName.add("NM_MINOR").withSeparator(". ")
    shortName.add("NM_DEGREE_PRE").withSeparator(", ")
    shortName.add("NM_DEGREE_POST").withSeparator(" ")
    
    result.addIndex("SHORT_NAME", shortName.build().toLowerCase());
    return result
}