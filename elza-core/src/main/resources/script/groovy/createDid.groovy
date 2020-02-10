package script.groovy

import cz.tacr.elza.domain.ArrDataText
import cz.tacr.elza.domain.ArrDataUnitdate
import cz.tacr.elza.domain.ArrDataUnitid
import cz.tacr.elza.domain.ArrDescItem
import cz.tacr.elza.domain.ArrNode
import cz.tacr.elza.domain.convertor.UnitDateConvertor
import cz.tacr.elza.service.cache.CachedNode
import cz.tacr.elza.ws.types.v1.Datesingle
import cz.tacr.elza.ws.types.v1.Did
import cz.tacr.elza.ws.types.v1.Unitdatestructured
import cz.tacr.elza.ws.types.v1.Unitid
import cz.tacr.elza.ws.types.v1.Unittitle
import cz.tacr.elza.ws.types.v1.Calendar
import org.springframework.util.Assert

/**
 * Skript pro vytvoření did.
 */


ArrNode node = NODE
CachedNode cachedNode = CACHED_NODE
return createDid(node, cachedNode);


/**
 * Provede vytvoření did pro zadaný node.
 * @param node       node pro který se vytváří did
 * @param cachedNode celá JP
 * @return vytvořený did
 */
static Did createDid(ArrNode node, CachedNode cachedNode) {
    Assert.notNull(node, "JP musí být vyplněna")
    Assert.notNull(cachedNode, "JP z cache musí být vyplněna")

    // kontrola správnosti zadaného node
    Assert.hasText(node.getUuid(), "Unikátní identifikátor JP musí být vyplněn")

    Did did = new Did()
    did.setIdentifier(node.getUuid())

    for (ArrDescItem descItem : cachedNode.getDescItems()) {
        def type = descItem.getItemType()
        if (type.getCode().equalsIgnoreCase("ZP2015_TITLE")) {
            def data = (ArrDataText) descItem.getData()
            def unittitle = new Unittitle()
            unittitle.setLocaltype(data.getValue())
            did.getUnittitle().add(unittitle)
        } else if (type.getCode().equalsIgnoreCase("ZP2015_UNIT_ID")) {
            def data = (ArrDataUnitid) descItem.getData()
            def unitid = new Unitid()
            unitid.setLocaltype(data.getUnitId())
            did.getUnitid().add(unitid)
        } else if (type.getCode().equalsIgnoreCase("ZP2015_UNIT_DATE")) {
            def data = (ArrDataUnitdate) descItem.getData()
            def calendarType = data.getCalendarType()
            def unitdatestructured = new Unitdatestructured();
            unitdatestructured.setCalendar(Calendar.fromValue(calendarType.getCode().toLowerCase()))
            def datesingle = new Datesingle()
            datesingle.setLocaltype(UnitDateConvertor.convertToString(data))
            unitdatestructured.setDatesingle(datesingle)
            did.setUnitdatestructured(unitdatestructured)
        }
    }

    return did
}
