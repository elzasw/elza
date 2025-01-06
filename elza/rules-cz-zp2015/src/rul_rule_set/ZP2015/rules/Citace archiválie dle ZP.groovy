package ZP2015

import cz.tacr.elza.groovy.GroovyItem
import cz.tacr.elza.groovy.GroovyGenCtx
import cz.tacr.elza.groovy.GroovyUtils

return generate(GENERATOR_CONTEXT)

String generate(final GroovyGenCtx ctx) {
    GroovyItem aeName = GroovyUtils.findItemByPartContains(ctx.getGroovyAe(), "PT_NAME", "NM_MAIN", true)
    GroovyItem aeIdn = GroovyUtils.findItemByPartContains(ctx.getGroovyAe(), "PT_IDENT", "IDN_VALUE", false)

    String idn = aeIdn == null ? "-" : aeIdn.getValue()
    String fundName = ctx.getFund().getName()
    String fundMark = ctx.getFund().getMark() == null ? "" : " (" + ctx.getFund().getMark() + ")"
    String fundNumber = ctx.getFund().getNumber() == null ? "-" : String.valueOf(ctx.getFund().getNumber())
    String unitId = GroovyUtils.findItemByItemTypeCode("ZP2015_UNIT_ID", "-", "", ctx.getItems())

    String refOzn = ", ref. ozn. CZ" + idn + "//" + fundNumber + "//" + unitId

    String invCislo = GroovyUtils.findItemByItemTypeCode("ZP2015_INV_CISLO", ", inv. č. ", ctx.getItems())
    String serialNumber = GroovyUtils.findItemByItemTypeCode("ZP2015_SERIAL_NUMBER", ", poř. č. ", ctx.getItems())
    String otherId = GroovyUtils.findItemByItemTypeCode("ZP2015_OTHER_ID", "", ctx.getItems())
    String otherIdSigOrig = GroovyUtils.findItemByItemTypeCode("ZP2015_OTHERID_SIG_ORIG", "", ctx.getItems())
    String sigOrig = otherId.isEmpty() && otherIdSigOrig.isEmpty() ? "" : ", sign. pův. " + otherId + otherIdSigOrig
    String otherIdSig = GroovyUtils.findItemByItemTypeCode("ZP2015_OTHERID_SIG", ", sign. ", ctx.getItems())
    String otherIdStorageId = GroovyUtils.findItemByItemTypeCode("ZP2015_OTHERID_STORAGE_ID", ", ukl. znak ", ctx.getItems())
    String otherIdOldSig = GroovyUtils.findItemByItemTypeCode("ZP2015_OTHERID_OLDSIG2", ", sp. znak ", ctx.getItems())
    String otherIdCJ = GroovyUtils.findItemByItemTypeCode("ZP2015_OTHERID_CJ", ", č. j. ", ctx.getItems())
    String otherIdDocId = GroovyUtils.findItemByItemTypeCode("ZP2015_OTHERID_DOCID", ", zn. sp. ", ctx.getItems())
    String otherIdFormalDocId = GroovyUtils.findItemByItemTypeCode("ZP2015_OTHERID_FORMAL_DOCID", ", č. vl. ", ctx.getItems())
    String otherIdAddId = GroovyUtils.findItemByItemTypeCode("ZP2015_OTHERID_ADDID", ", přír. č. ", ctx.getItems())
    String otherIdPicId = GroovyUtils.findItemByItemTypeCode("ZP2015_OTHERID_PICID", ", nakl. č. ", ctx.getItems())
    String otherIdNegId = GroovyUtils.findItemByItemTypeCode("ZP2015_OTHERID_NEGID", ", č. neg. ", ctx.getItems())
    String otherIdCdId = GroovyUtils.findItemByItemTypeCode("ZP2015_OTHERID_CDID", ", č. prod. ", ctx.getItems())
    String otherIdIsbn = GroovyUtils.findItemByItemTypeCode("ZP2015_OTHERID_ISBN", ", ISBN ", ctx.getItems())
    String otherIdIssn = GroovyUtils.findItemByItemTypeCode("ZP2015_OTHERID_ISSN", ", ISSN ", ctx.getItems())
    String otherIdIsmn = GroovyUtils.findItemByItemTypeCode("ZP2015_OTHERID_ISMN", ", ISMN ", ctx.getItems())
    String otherIdMatrixId = GroovyUtils.findItemByItemTypeCode("ZP2015_OTHERID_MATRIXID", ", matr. č. ", ctx.getItems())
    String title = GroovyUtils.findItemByItemTypeCode("ZP2015_TITLE", "", ", ", ctx.getItems(), 100)
    String unitDate = GroovyUtils.findItemByItemTypeCode("ZP2015_UNIT_DATE", ", ", ctx.getItems())
    String storageId = GroovyUtils.findItemByItemTypeCode("ZP2015_STORAGE_ID", ", ukl. j. ", ctx.getItems())
    String itemOrder = GroovyUtils.findItemByItemTypeCode("ZP2015_ITEM_ORDER", "/", ctx.getItems())

    return aeName.getValue() + ", " + fundName + fundMark + refOzn + 
        invCislo + 
        serialNumber + 
        sigOrig + 
        otherIdStorageId + 
        otherIdOldSig + 
        otherIdCJ +
        otherIdDocId +
        otherIdFormalDocId +
        otherIdAddId +
        otherIdPicId +
        otherIdNegId +
        otherIdCdId +
        otherIdIsbn +
        otherIdIssn +
        otherIdIsmn +
        otherIdMatrixId +
        title +
        unitDate +
        storageId +
        itemOrder
}