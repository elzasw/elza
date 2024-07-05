package scripts.PERSON

import cz.tacr.elza.groovy.GroovyAppender
import cz.tacr.elza.groovy.GroovyPart
import cz.tacr.elza.groovy.GroovyResult
import cz.tacr.elza.groovy.GroovyUtils

return generate(PART)

static GroovyResult generate(final GroovyPart part) {
    GroovyAppender compl = GroovyUtils.createAppender(part)
    compl.add("NM_SUP_GEN")
    compl.add("NM_SUP_CHRO").withSeparator(" : ")
    compl.add("NM_SUP_DIFF").withSeparator(" : ")
    compl.add("NM_SUP_PRIV").withSeparator(" : ")

    GroovyAppender base = GroovyUtils.createAppender(part)
    base.add("NM_MAIN")
    base.add("NM_MINOR").withSeparator(", ")
    base.add("NM_DEGREE_PRE").withSeparator(", ")
    base.add("NM_DEGREE_POST").withSeparator(" ")
    base.addStr(compl.build()).withSeparator(" ").withPrefix("(").withPostfix(")")

    GroovyResult result = new GroovyResult()
    def r = base.build();
    result.setDisplayName(r)
    result.setSortName(r)
    if (part.isPreferred()) {
        result.setPtPreferName(r)
    }

    GroovyAppender shortName = GroovyUtils.createAppender(part)
    shortName.add("NM_MAIN")
    shortName.add("NM_MINOR").withSeparator(", ")
    shortName.add("NM_DEGREE_PRE").withSeparator(", ")
    shortName.add("NM_DEGREE_POST").withSeparator(" ")
    
    GroovyAppender complGeo = GroovyUtils.createAppender(part)
    complGeo.add("NM_SUP_GEO")
    shortName.addStr(complGeo.build()).withSeparator(" ").withPrefix("(").withPostfix(")")

    result.addIndex("SHORT_NAME", shortName.build());
    return result
}