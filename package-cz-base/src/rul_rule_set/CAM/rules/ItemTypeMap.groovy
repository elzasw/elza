package rul_rule_set.CAM.rules

import cz.tacr.elza.api.ApExternalSystemType
import groovy.transform.Field

return findItemTypeCode(EXT_SYSTEM_TYPE, ITEM_TYPE_CODE)

@Field static Map<String, Map<String, String>> itemTypeMap = createItemTypeMap()


static String findItemTypeCode(final String extSystemType, final String itemTypeCode) {
    String resultItemTypeCode = null
    Map<String, String> systemItemTypeMap = itemTypeMap.getOrDefault(extSystemType, null)

    if (systemItemTypeMap != null) {
        resultItemTypeCode = systemItemTypeMap.getOrDefault(itemTypeCode, null)
    }

    return resultItemTypeCode
}

static Map<String, Map<String, String>> createItemTypeMap() {
    Map<String, String> camItemTypeMap = createCamItemTypeMap()

    Map<String, Map<String, String>> itemTypeMap = new HashMap<>()
    itemTypeMap.put(ApExternalSystemType.CAM.toString(), camItemTypeMap)
    itemTypeMap.put(ApExternalSystemType.CAM_COMPLETE.toString(), camItemTypeMap)
    itemTypeMap.put(ApExternalSystemType.CAM_UUID.toString(), camItemTypeMap)
    return itemTypeMap
}

static Map<String, String> createCamItemTypeMap() {
    Map<String, String> camItemTypeMap = new HashMap<>()
    camItemTypeMap.put("COORD_POINT", "COORD_POINT")
    camItemTypeMap.put("COORD_BORDER", "COORD_BORDER")
    camItemTypeMap.put("COORD_NOTE", "COORD_NOTE")
    camItemTypeMap.put("GEO_TYPE", "GEO_TYPE")
    camItemTypeMap.put("GEO_ADMIN_CLASS", "GEO_ADMIN_CLASS")
    camItemTypeMap.put("NOTE", "NOTE")
    camItemTypeMap.put("IDN_TYPE", "IDN_TYPE")
    camItemTypeMap.put("IDN_VALID_FROM", "IDN_VALID_FROM")
    camItemTypeMap.put("IDN_VALID_TO", "IDN_VALID_TO")
    camItemTypeMap.put("IDN_VALUE", "IDN_VALUE")
    camItemTypeMap.put("IDN_VERIFIED", "IDN_VERIFIED")
    camItemTypeMap.put("NM_MAIN", "NM_MAIN")
    camItemTypeMap.put("NM_MINOR", "NM_MINOR")
    camItemTypeMap.put("NM_SUP_GEN", "NM_SUP_GEN")
    camItemTypeMap.put("NM_SUP_GEO", "NM_SUP_GEO")
    camItemTypeMap.put("NM_SUP_CHRO", "NM_SUP_CHRO")
    camItemTypeMap.put("NM_ORDER", "NM_ORDER")
    camItemTypeMap.put("NM_AUTH", "NM_AUTH")
    camItemTypeMap.put("NM_TYPE", "NM_TYPE")
    camItemTypeMap.put("NM_USED_FROM", "NM_USED_FROM")
    camItemTypeMap.put("NM_USED_TO", "NM_USED_TO")
    camItemTypeMap.put("NM_LANG", "NM_LANG")
    camItemTypeMap.put("NM_DEGREE_PRE", "NM_DEGREE_PRE")
    camItemTypeMap.put("NM_DEGREE_POST", "NM_DEGREE_POST")
    camItemTypeMap.put("BRIEF_DESC", "BRIEF_DESC")
    camItemTypeMap.put("LANG", "LANG")
    camItemTypeMap.put("EV_TYPE", "EV_TYPE")
    camItemTypeMap.put("EV_BEGIN", "EV_BEGIN")
    camItemTypeMap.put("EV_END", "EV_END")
    camItemTypeMap.put("REL_ENTITY", "REL_ENTITY")
    camItemTypeMap.put("REL_BEGIN", "REL_BEGIN")
    camItemTypeMap.put("REL_END", "REL_END")
    camItemTypeMap.put("CRE_CLASS", "CRE_CLASS")
    camItemTypeMap.put("CRE_TYPE", "CRE_TYPE")
    camItemTypeMap.put("CRE_DATE", "CRE_DATE")
    camItemTypeMap.put("EXT_CLASS", "EXT_CLASS")
    camItemTypeMap.put("EXT_TYPE", "EXT_TYPE")
    camItemTypeMap.put("EXT_DATE", "EXT_DATE")
    camItemTypeMap.put("CORP_PURPOSE", "CORP_PURPOSE")
    camItemTypeMap.put("FOUNDING_NORMS", "FOUNDING_NORMS")
    camItemTypeMap.put("SCOPE_NORMS", "SCOPE_NORMS")
    camItemTypeMap.put("CORP_STRUCTURE", "CORP_STRUCTURE")
    camItemTypeMap.put("NOTE_INTERNAL", "NOTE_INTERNAL")
    camItemTypeMap.put("SOURCE_INFO", "SOURCE_INFO")
    camItemTypeMap.put("SOURCE_LINK", "SOURCE_LINK")
    camItemTypeMap.put("HISTORY", "HISTORY")
    camItemTypeMap.put("GENEALOGY", "GENEALOGY")
    camItemTypeMap.put("BIOGRAPHY", "BIOGRAPHY")
    camItemTypeMap.put("DESCRIPTION", "DESCRIPTION")

    return camItemTypeMap
}
