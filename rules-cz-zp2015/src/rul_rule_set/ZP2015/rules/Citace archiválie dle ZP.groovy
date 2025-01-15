package ZP2015

import cz.tacr.elza.groovy.GroovyItem
import cz.tacr.elza.groovy.GroovyGenCtx
import cz.tacr.elza.groovy.GroovyUtils
import cz.tacr.elza.groovy.ResultBuilder

return generate(GENERATOR_CONTEXT)

String generate(final GroovyGenCtx ctx) {
    GroovyItem aeName = GroovyUtils.findItemByPartContains(ctx.getGroovyAe(), "PT_NAME", "NM_MAIN", true)
    GroovyItem aeIdn = GroovyUtils.findItemByPartContains(ctx.getGroovyAe(), "PT_IDENT", "IDN_VALUE", false)

    String fundName = ctx.getFund().getName()
    String fundMark = ctx.getFund().getMark() == null ? "" : " (" + ctx.getFund().getMark() + ")"

    String idn = aeIdn == null ? "-" : aeIdn.getValue()
    String fundNumber = ctx.getFund().getNumber() == null ? "-" : String.valueOf(ctx.getFund().getNumber())
    GroovyItem groovyUnitId = GroovyUtils.findItemByItemTypeCode("ZP2015_UNIT_ID", ctx.getItems())
    String unitId = groovyUnitId == null ? "-" : groovyUnitId.getValue()
    String refOzn = idn + "//" + fundNumber + "//" + unitId

    final String ZP2015_OTHER_ID = "ZP2015_OTHER_ID"

    return new ResultBuilder(aeName, ctx)
        .append(fundName + fundMark)
        .append("ref. ozn. CZ" + refOzn)
        .append("ZP2015_INV_CISLO", "inv. č. ")
        .append("ZP2015_SERIAL_NUMBER", "poř. č. ")
        .append(ZP2015_OTHER_ID, "ZP2015_OTHERID_SIG_ORIG", "sign. pův. ")
        .append(ZP2015_OTHER_ID, "ZP2015_OTHERID_SIG", "sign. ")
        .append(ZP2015_OTHER_ID, "ZP2015_OTHERID_STORAGE_ID", "ukl. znak ")
        .append(ZP2015_OTHER_ID, "ZP2015_OTHERID_OLDSIG2", "sp. znak ")
        .append(ZP2015_OTHER_ID, "ZP2015_OTHERID_CJ", "č. j. ")
        .append(ZP2015_OTHER_ID, "ZP2015_OTHERID_DOCID", "zn. sp. ")
        .append(ZP2015_OTHER_ID, "ZP2015_OTHERID_FORMAL_DOCID", "č. vl. ")
        .append(ZP2015_OTHER_ID, "ZP2015_OTHERID_ADDID", "přír. č. ")
        .append(ZP2015_OTHER_ID, "ZP2015_OTHERID_PICID", "nakl. č. ")
        .append(ZP2015_OTHER_ID, "ZP2015_OTHERID_NEGID", "č. neg. ")
        .append(ZP2015_OTHER_ID, "ZP2015_OTHERID_CDID", "č. prod. ")
        .append(ZP2015_OTHER_ID, "ZP2015_OTHERID_ISBN", "ISBN ")
        .append(ZP2015_OTHER_ID, "ZP2015_OTHERID_ISSN", "ISSN ")
        .append(ZP2015_OTHER_ID, "ZP2015_OTHERID_ISMN", "ISMN ")
        .append(ZP2015_OTHER_ID, "ZP2015_OTHERID_MATRIXID", "matr. č. ")
        .append("ZP2015_TITLE", 100)
        .append("ZP2015_UNIT_DATE", "") // to format
        .append("ZP2015_STORAGE_ID", "ukl. j. ")
        .setSeparator("/")
        .append("ZP2015_ITEM_ORDER", "")
        .toString()
}