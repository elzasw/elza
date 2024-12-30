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

    GroovyItem groovyUnitId = GroovyUtils.findItemByItemTypeCode("ZP2015_UNIT_ID", ctx.getItems())
    String unitId = groovyUnitId == null ? "-" : groovyUnitId.getValue()

    String refOzn = ", ref. ozn. CZ" + idn + "//" + fundNumber + "//" + unitId

    GroovyItem groovyInvCislo = GroovyUtils.findItemByItemTypeCode("ZP2015_INV_CISLO", ctx.getItems())
    GroovyItem groovySerialNumber = GroovyUtils.findItemByItemTypeCode("ZP2015_SERIAL_NUMBER", ctx.getItems())
    GroovyItem groovyOtherId = GroovyUtils.findItemByItemTypeCode("ZP2015_OTHER_ID", ctx.getItems())
    GroovyItem groovyOtherIdSigOrig = GroovyUtils.findItemByItemTypeCode("ZP2015_OTHERID_SIG_ORIG", ctx.getItems())
    GroovyItem groovyOtherIdSig = GroovyUtils.findItemByItemTypeCode("ZP2015_OTHERID_SIG", ctx.getItems())
    GroovyItem groovyOtherIdStorageId = GroovyUtils.findItemByItemTypeCode("ZP2015_OTHERID_STORAGE_ID", ctx.getItems())
    GroovyItem groovyOtherIdOldSig2 = GroovyUtils.findItemByItemTypeCode("ZP2015_OTHERID_OLDSIG2", ctx.getItems())
    GroovyItem groovyOtherIdCJ = GroovyUtils.findItemByItemTypeCode("ZP2015_OTHERID_CJ", ctx.getItems())
    GroovyItem groovyOtherIdDocId = GroovyUtils.findItemByItemTypeCode("ZP2015_OTHERID_DOCID", ctx.getItems())
    GroovyItem groovyOtherIdFormalDocId = GroovyUtils.findItemByItemTypeCode("ZP2015_OTHERID_FORMAL_DOCID", ctx.getItems())
    GroovyItem groovyOtherIdAddId = GroovyUtils.findItemByItemTypeCode("ZP2015_OTHERID_ADDID", ctx.getItems())
    GroovyItem groovyOtherIdPicId = GroovyUtils.findItemByItemTypeCode("ZP2015_OTHERID_PICID", ctx.getItems())
    GroovyItem groovyOtherIdNegId = GroovyUtils.findItemByItemTypeCode("ZP2015_OTHERID_NEGID", ctx.getItems())
    GroovyItem groovyOtherIdCdId = GroovyUtils.findItemByItemTypeCode("ZP2015_OTHERID_CDID", ctx.getItems())
    GroovyItem groovyOtherIdIsbn = GroovyUtils.findItemByItemTypeCode("ZP2015_OTHERID_ISBN", ctx.getItems())
    GroovyItem groovyOtherIdIssn = GroovyUtils.findItemByItemTypeCode("ZP2015_OTHERID_ISSN", ctx.getItems())
    GroovyItem groovyOtherIdIsmn = GroovyUtils.findItemByItemTypeCode("ZP2015_OTHERID_ISMN", ctx.getItems())
    GroovyItem groovyOtherIdMatrixId = GroovyUtils.findItemByItemTypeCode("ZP2015_OTHERID_MATRIXID", ctx.getItems())
    GroovyItem groovyTitle = GroovyUtils.findItemByItemTypeCode("ZP2015_TITLE", ctx.getItems())
    GroovyItem groovyUnitDate = GroovyUtils.findItemByItemTypeCode("ZP2015_UNIT_DATE", ctx.getItems())
    GroovyItem groovyStorageId = GroovyUtils.findItemByItemTypeCode("ZP2015_STORAGE_ID", ctx.getItems())
    GroovyItem groovyItemOrder = GroovyUtils.findItemByItemTypeCode("ZP2015_ITEM_ORDER", ctx.getItems())

    String invCislo = groovyInvCislo == null ? "" : ", inv. č. " + groovyInvCislo.getValue()
    String serialNumber = groovySerialNumber == null ? "" : ", poř. č. " + groovySerialNumber.getValue()
    String sigOrig = groovyOtherId != null || groovyOtherIdSigOrig != null ? 
        ", sign. pův. " + (groovyOtherId == null ? "" : groovyOtherId.getValue()) + (groovyOtherIdSigOrig == null ? "" : groovyOtherIdSigOrig.getValue()) : ""
    String otherIdSig = groovyOtherIdSig == null ? "" : ", sign. " + groovyOtherIdSig.getValue()
    String otherIdStorageId = groovyOtherIdStorageId == null ? "" : ", ukl. znak " + groovyOtherIdStorageId.getValue()
    String otherIdOldSig = groovyOtherIdOldSig2 == null ? "" : ", sp. znak " + groovyOtherIdOldSig2.getValue()
    String otherIdCJ = groovyOtherIdCJ == null ? "" : ", č. j. " + groovyOtherIdCJ.getValue()
    String otherIdDocId = groovyOtherIdDocId == null ? "" : ", zn. sp. " + groovyOtherIdDocId.getValue()
    String otherIdFormalDocId = groovyOtherIdFormalDocId == null ? "" : ", č. vl. " + groovyOtherIdFormalDocId.getValue()
    String otherIdAddId = groovyOtherIdAddId == null ? "" : ", přír. č. " + groovyOtherIdAddId.getValue()
    String otherIdPicId = groovyOtherIdPicId == null ? "" : ", nakl. č. " + groovyOtherIdPicId.getValue()
    String otherIdNegId = groovyOtherIdNegId == null ? "" : ", č. neg. " + groovyOtherIdNegId.getValue()
    String otherIdCdId = groovyOtherIdCdId == null ? "" : ", č. prod. " + groovyOtherIdCdId.getValue()
    String otherIdIsbn = groovyOtherIdIsbn == null ? "" : ", ISBN " + groovyOtherIdIsbn.getValue()
    String otherIdIssn = groovyOtherIdIssn == null ? "" : ", ISSN " + groovyOtherIdIssn.getValue()
    String otherIdIsmn = groovyOtherIdIsmn == null ? "" : ", ISMN " + groovyOtherIdIsmn.getValue()
    String otherIdMatrixId = groovyOtherIdMatrixId == null ? "" : ", matr. č. " + groovyOtherIdMatrixId.getValue()
    String title = groovyTitle == null ? "" : ", " + groovyTitle.getValue() // up to 100 char
    String unitDate = groovyUnitDate == null ? "" : ", " + groovyUnitDate.getValue()
    String storageId = groovyStorageId == null ? "" : ", ukl. j. " + groovyStorageId.getValue()
    String itemOrder = groovyItemOrder == null ? "" : ", " + groovyItemOrder.getValue()

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
        itemOrder
}