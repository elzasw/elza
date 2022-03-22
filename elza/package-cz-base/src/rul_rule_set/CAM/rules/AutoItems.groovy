package rul_rule_set.CAM.rules

import java.util.Arrays
import java.util.List

import cz.tacr.elza.groovy.GroovyAe
import cz.tacr.elza.groovy.GroovyPart
import cz.tacr.elza.groovy.GroovyItem
import cz.tacr.elza.groovy.GroovyUtils

import cz.tacr.elza.exception.codes.BaseCode
import cz.tacr.elza.exception.ObjectNotFoundException

return generate(AE)

// convert string like: Kladno (Kladno, Česko) -> Kladno, Kladno, Česko
static String convertString(String str) {
    return str.replaceAll(" \\(", ", ").replaceAll("\\)", "")
}

static List<GroovyItem> generate(final GroovyAe ae) {
    List<GroovyItem> items = new ArrayList<>()

    // dočasně, jen pro ladění
    for (GroovyPart part : ae.getParts()) {
        //System.out.println(part.getPartType().getCode())
        for (GroovyItem item : part.getItems()) {
            //System.out.println(item)
            //items.add(item)
        }
    }

    // seznam vyloučených typů území
    List<String> excludeTerritory = Arrays.asList(
        "GT_COUNTRY",
        "GT_LAND",
        "GT_VOJVODSTVI",
        "GT_MUNIPDISTR",
        "GT_MUNIP",
        "GT_MUNIPPART",
        "GT_SQUARE",
        "GT_WATERFRONT",
        "GT_SEABOTSHAPE",
        "GT_CITYDISTRICT",
        "GT_OTHERAREA",
        "GT_PROTNATPART",
        "GT_FORESTPARK",
        "GT_NATUREPART",
        "GT_NATFORMATION",
        "GT_WATERAREA",
        "GT_NAMEDFORMATION",
        "GT_COSMOSPART"
        )

    // seznam zaniklých typů území
    List<String> disappearedTerritory = Arrays.asList(
        "GT_MUNIP",
        "GT_MUNIPPART",
        "GT_CITYDISTRICT",
        "GT_STREET",
        "GT_SQUARE",
        "GT_WATERFRONT",
        "GT_SETTLEMENT",
        "GT_MILITARYAREA"
        )

    // načtení vzniku a zániku pro chronologický doplněk
    GroovyItem fromClass = GroovyUtils.findFirstItem(ae, "PT_CRE", GroovyPart.PreferredFilter.ALL, "CRE_CLASS")
    GroovyItem toClass = GroovyUtils.findFirstItem(ae, "PT_EXT", GroovyPart.PreferredFilter.ALL, "EXT_CLASS")
    GroovyItem from = GroovyUtils.findFirstItem(ae, "PT_CRE", GroovyPart.PreferredFilter.ALL, "CRE_DATE")
    GroovyItem to = GroovyUtils.findFirstItem(ae, "PT_EXT", GroovyPart.PreferredFilter.ALL, "EXT_DATE")

    // generování specifických prefixů pro vznik
    String prefixFrom = ""
    if (fromClass != null) {
        switch (fromClass.specCode) {
            case "CRC_FIRSTWMENTION":
                prefixFrom = "uváděno od "
                break
            case "CRC_BEGINSCOPE":
                prefixFrom = "působnost od "
                break
        }
    }

    // generování specifických prefixů pro zánik
    String prefixTo = ""
    if (toClass != null) {
        switch (toClass.specCode) {
            case "EXC_LASTWMENTION":
                prefixTo = "uváděno do "
                break
            case "EXC_ENDSCOPE":
                prefixTo = "působnost do "
                break
        }
    }

    // definice prefixu, když je shodný rok u datace od i do
    String prefixYearEqual = ""
    if (prefixFrom == "uváděno od " && prefixTo == "uváděno do ") {
        prefixYearEqual = "uváděno "
    } else {
        if (prefixFrom == "působnost od " && prefixTo == "působnost do ") {
            prefixYearEqual = "působnost "
        }
    }

    // vytvářet hodnotu pro chronologický doplněk
    String nmSupChro = GroovyUtils.formatUnitdate(from, to)
                .prefixFrom(prefixFrom)
                .prefixTo(prefixTo)
                .formatYear()
                .yearEqual(true, prefixYearEqual)
                .build()

    // obecný doplněk
    GroovyItem geoType = GroovyUtils.findFirstItem(ae, "PT_BODY", GroovyPart.PreferredFilter.ALL, "GEO_TYPE")
    if (geoType != null) {
        System.out.println(geoType)
        if (!excludeTerritory.contains(geoType.getSpecCode())) {
            GroovyItem obecItem = new GroovyItem("NM_SUP_GEN", null, geoType.getValue())
            items.add(obecItem)
        }
    }

    // chronologický doplněk
    GroovyItem itemChro = new GroovyItem("NM_SUP_CHRO", null, nmSupChro)
    if (!itemChro.getValue().isEmpty()) {
        // pokud objekt zanikl a je zařazen do seznamu excludeTerritory
        if (to != null && disappearedTerritory.contains(geoType.getSpecCode())) {
            items.add(new GroovyItem("NM_SUP_CHRO", null, "zaniklo"))
        } else {
            items.add(itemChro)
        }
    }

    // geografický doplněk
    GroovyItem geo = GroovyUtils.findFirstItem(ae, "PT_BODY", GroovyPart.PreferredFilter.ALL, "GEO_ADMIN_CLASS")
    if (geo != null) {
        if (geo.getValue() != null) {
            if (geo.getIntValue() > 0) {
                GroovyItem geoItem = new GroovyItem("NM_SUP_GEO", null, convertString(geo.getValue()))
                items.add(geoItem)
            } else {
                throw new ObjectNotFoundException("Entita nebyla načtena z externího systému", BaseCode.DB_INTEGRITY_PROBLEM)
                    .set("entityId", geo.getValue())
            }
        }
    }

    // odkazy na geografické doplňky a autory
    List<GroovyItem> rels = GroovyUtils.findAllItems(ae, "PT_REL", GroovyPart.PreferredFilter.ALL, "REL_ENTITY")
    for (GroovyItem rel : rels) {
        // geografický doplněk
        if (Arrays.asList("RT_RESIDENCE", "RT_VENUE", "RT_LOCATION").contains(rel.getSpecCode())) {
            if (rel.getValue() != null) {
                if (geo.getIntValue() > 0) {
                    GroovyItem itemGeo = new GroovyItem("NM_SUP_GEO", null, convertString(rel.getValue()))
                    items.add(itemGeo)
                } else {
                    throw new ObjectNotFoundException("Entita nebyla načtena z externího systému", BaseCode.DB_INTEGRITY_PROBLEM)
                        .set("entityId", rel.getValue())
                }
            }
        }
        // autor/tvůrce
        if (rel.getSpecCode().equals("RT_AUTHOROFCHANGE")) {
            GroovyItem itemAuth = new GroovyItem("NM_AUTH", null, rel.getValue())
            items.add(itemAuth)
        }
    }

    return items
}