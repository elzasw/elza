package ZP2015

import java.util.List;

import cz.tacr.elza.groovy.GroovyItem
import cz.tacr.elza.groovy.GroovyGenCtx
import cz.tacr.elza.groovy.GroovyUtils
import cz.tacr.elza.groovy.ResultBuilder
import cz.tacr.elza.print.item.convertors.UnitDatePrintConvertor

return generate(GENERATOR_CONTEXT)

String generate(final GroovyGenCtx ctx) {
    String fundName = ctx.getFund().getName()
    String fundMark = ctx.getFund().getMark() == null ? "" : " (" + ctx.getFund().getMark() + ")"

    String idn = ctx.getInstitutionCode() == null ? "-" : ctx.getInstitutionCode()
    String fundNumber = ctx.getFund().getNumber() == null ? "-" : String.valueOf(ctx.getFund().getNumber())
    GroovyItem groovyUnitId = ctx.getFirstItemByItemType("ZP2015_UNIT_ID")
    String unitId = groovyUnitId == null ? "-" : groovyUnitId.getValue()
    String refOzn = idn + "//" + fundNumber + "//" + unitId

    GroovyItem aeName = GroovyUtils.findItemByPartContains(ctx.getGroovyAe(), "PT_NAME", "NM_MAIN", true)
    final String ZP2015_OTHER_ID = "ZP2015_OTHER_ID"

    rb = new ResultBuilder(aeName, ctx)
    // add fund
    rb.append(fundName + fundMark)

    // ref ozn
    rb.append("ref. ozn. CZ" + refOzn)
      .appendItem("ZP2015_INV_CISLO", "inv. č. ")
      .appendItem("ZP2015_SERIAL_NUMBER", "poř. č. ")

    // jina ozn
    rb.appendItemWithSpecLabel(ZP2015_OTHER_ID, "ZP2015_OTHERID_SIG_ORIG")
      .appendItemWithSpecLabel(ZP2015_OTHER_ID, "ZP2015_OTHERID_SIG")
      .appendItemWithSpecLabel(ZP2015_OTHER_ID, "ZP2015_OTHERID_STORAGE_ID")
      .appendItemWithSpecLabel(ZP2015_OTHER_ID, "ZP2015_OTHERID_OLDSIG2")
      .appendItemWithSpecLabel(ZP2015_OTHER_ID, "ZP2015_OTHERID_CJ")
      .appendItemWithSpecLabel(ZP2015_OTHER_ID, "ZP2015_OTHERID_DOCID")
      .appendItemWithSpecLabel(ZP2015_OTHER_ID, "ZP2015_OTHERID_FORMAL_DOCID")
      .appendItemWithSpecLabel(ZP2015_OTHER_ID, "ZP2015_OTHERID_ADDID")
      .appendItemWithSpecLabel(ZP2015_OTHER_ID, "ZP2015_OTHERID_PICID")
      .appendItemWithSpecLabel(ZP2015_OTHER_ID, "ZP2015_OTHERID_NEGID")
      .appendItemWithSpecLabel(ZP2015_OTHER_ID, "ZP2015_OTHERID_CDID")
      .appendItemWithSpecLabel(ZP2015_OTHER_ID, "ZP2015_OTHERID_ISBN")
      .appendItemWithSpecLabel(ZP2015_OTHER_ID, "ZP2015_OTHERID_ISSN")
      .appendItemWithSpecLabel(ZP2015_OTHER_ID, "ZP2015_OTHERID_ISMN")
      .appendItemWithSpecLabel(ZP2015_OTHER_ID, "ZP2015_OTHERID_MATRIXID")

    // add title
    rb.appendItemWithLimit("ZP2015_TITLE", 100)

    // add date
    GroovyItem unitdate = ctx.getFirstItemByItemType("ZP2015_UNIT_DATE")
    if (unitdate != null) {
        // convert unitdate to String
        String textDate = UnitDatePrintConvertor.convertToPrint(unitdate.getUnitdateValue())
        rb.append(textDate)
    }

    rb.appendItem("ZP2015_STORAGE_ID", "ukl. j. ")
      .setSeparator("/")
      .appendItem("ZP2015_ITEM_ORDER")

    return rb.toString()
}