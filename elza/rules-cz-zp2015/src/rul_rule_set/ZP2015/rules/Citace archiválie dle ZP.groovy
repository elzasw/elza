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

    GroovyItem invCislo = GroovyUtils.findItemByItemTypeCode("ZP2015_INV_CISLO", ctx.getItems())
    GroovyItem serialNumber = GroovyUtils.findItemByItemTypeCode("ZP2015_SERIAL_NUMBER", ctx.getItems())

    final String ZP2015_OTHER_ID = "ZP2015_OTHER_ID"
    GroovyItem otherIdSigOrig = GroovyUtils.findItemByItemTypeSpecCode(ZP2015_OTHER_ID, "ZP2015_OTHERID_SIG_ORIG", ctx.getItems())
    GroovyItem otherIdSig = GroovyUtils.findItemByItemTypeSpecCode(ZP2015_OTHER_ID, "ZP2015_OTHERID_SIG", ctx.getItems())
    GroovyItem otherIdStorageId = GroovyUtils.findItemByItemTypeSpecCode(ZP2015_OTHER_ID, "ZP2015_OTHERID_STORAGE_ID", ctx.getItems())
    GroovyItem otherIdOldSig = GroovyUtils.findItemByItemTypeSpecCode(ZP2015_OTHER_ID, "ZP2015_OTHERID_OLDSIG2", ctx.getItems())
    GroovyItem otherIdCJ = GroovyUtils.findItemByItemTypeSpecCode(ZP2015_OTHER_ID, "ZP2015_OTHERID_CJ", ctx.getItems())
    GroovyItem otherIdDocId = GroovyUtils.findItemByItemTypeSpecCode(ZP2015_OTHER_ID, "ZP2015_OTHERID_DOCID", ctx.getItems())
    GroovyItem otherIdFormalDocId = GroovyUtils.findItemByItemTypeSpecCode(ZP2015_OTHER_ID, "ZP2015_OTHERID_FORMAL_DOCID", ctx.getItems())
    GroovyItem otherIdAddId = GroovyUtils.findItemByItemTypeSpecCode(ZP2015_OTHER_ID, "ZP2015_OTHERID_ADDID", ctx.getItems())
    GroovyItem otherIdPicId = GroovyUtils.findItemByItemTypeSpecCode(ZP2015_OTHER_ID, "ZP2015_OTHERID_PICID", ctx.getItems())
    GroovyItem otherIdNegId = GroovyUtils.findItemByItemTypeSpecCode(ZP2015_OTHER_ID, "ZP2015_OTHERID_NEGID", ctx.getItems())
    GroovyItem otherIdCdId = GroovyUtils.findItemByItemTypeSpecCode(ZP2015_OTHER_ID, "ZP2015_OTHERID_CDID", ctx.getItems())
    GroovyItem otherIdIsbn = GroovyUtils.findItemByItemTypeSpecCode(ZP2015_OTHER_ID, "ZP2015_OTHERID_ISBN", ctx.getItems())
    GroovyItem otherIdIssn = GroovyUtils.findItemByItemTypeSpecCode(ZP2015_OTHER_ID, "ZP2015_OTHERID_ISSN", ctx.getItems())
    GroovyItem otherIdIsmn = GroovyUtils.findItemByItemTypeSpecCode(ZP2015_OTHER_ID, "ZP2015_OTHERID_ISMN", ctx.getItems())
    GroovyItem otherIdMatrixId = GroovyUtils.findItemByItemTypeSpecCode(ZP2015_OTHER_ID, "ZP2015_OTHERID_MATRIXID", ctx.getItems())

    GroovyItem title = GroovyUtils.findItemByItemTypeCode("ZP2015_TITLE", ctx.getItems())
    GroovyItem unitDate = GroovyUtils.findItemByItemTypeCode("ZP2015_UNIT_DATE", ctx.getItems())
    GroovyItem storageId = GroovyUtils.findItemByItemTypeCode("ZP2015_STORAGE_ID", ctx.getItems())
    GroovyItem itemOrder = GroovyUtils.findItemByItemTypeCode("ZP2015_ITEM_ORDER", ctx.getItems())

    return new ResultBuilder(aeName)
        .append(fundName + fundMark)
        .append("ref. ozn. CZ" + refOzn)
        .append("inv. č. ", invCislo)
        .append("poř. č. ", serialNumber)
        .append("sign. ", otherIdSig)
        .append("ukl. znak ", otherIdStorageId)
        .append("sp. znak ", otherIdOldSig)
        .append("č. j. ", otherIdCJ)
        .append("zn. sp. ", otherIdDocId)
        .append("č. vl. ", otherIdFormalDocId)
        .append("přír. č. ", otherIdAddId)
        .append("nakl. č. ", otherIdPicId)
        .append("č. neg. ", otherIdNegId)
        .append("č. prod. ", otherIdCdId)
        .append("ISBN ", otherIdIsbn)
        .append("ISSN ", otherIdIssn)
        .append("ISMN ", otherIdIsmn)
        .append("matr. č. ", otherIdMatrixId)
        .append(title, 100)
        .append(unitDate)
        .append("ukl. j. ", storageId)
        .setSeparator("/")
        .append(itemOrder)
        .toString()
}