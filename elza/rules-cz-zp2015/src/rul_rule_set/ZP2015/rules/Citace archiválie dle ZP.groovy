package ZP2015

import cz.tacr.elza.groovy.GroovyItem
import cz.tacr.elza.groovy.GroovyGenCtx
import cz.tacr.elza.groovy.GroovyUtils

return generate(GENERATOR_CONTEXT)

String generate(final GroovyGenCtx ctx) {
    GroovyItem aeName = GroovyUtils.findItemByPartContains(ctx.getGroovyAe(), "PT_NAME", "NM_MAIN", true)
    GroovyItem aeIdn = GroovyUtils.findItemByPartContains(ctx.getGroovyAe(), "PT_IDENT", "IDN_VALUE", false)

    String fundName = ctx.getFund().getName()
    String fundMark = ctx.getFund().getMark()
    Integer fundNumber = ctx.getFund().getNumber()

    GroovyItem groovyUnitId = GroovyUtils.findItemByItemTypeCode("ZP2015_UNIT_ID", ctx.getItems())
    String unitId = groovyUnitId == null ? "" : groovyUnitId.getValue()

    return aeName.getValue() + ", " + fundName + " (" + fundMark + "), ref. ozn. CZ" + aeIdn.getValue() + "//" + fundNumber + "//" + unitId
}