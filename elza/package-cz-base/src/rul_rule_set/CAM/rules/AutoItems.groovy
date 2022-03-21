package rul_rule_set.CAM.rules

import java.util.Arrays

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

    // chronologický doplněk
    GroovyItem from = GroovyUtils.findFirstItem(ae, "PT_CRE", GroovyPart.PreferredFilter.ALL, "CRE_DATE")
    GroovyItem to = GroovyUtils.findFirstItem(ae, "PT_EXT", GroovyPart.PreferredFilter.ALL, "EXT_DATE")
    String nmSupChro = GroovyUtils.formatUnitdate(from, to).formatYear().build()

    GroovyItem itemChro = new GroovyItem("NM_SUP_CHRO", null, nmSupChro)
    if (!itemChro.getValue().isEmpty()) {
        items.add(itemChro)
    }

    // geografický doplněk
    GroovyItem geo = GroovyUtils.findFirstItem(ae, "PT_BODY", GroovyPart.PreferredFilter.ALL, "GEO_ADMIN_CLASS")
    if (geo != null) {
        //System.out.println(geo)
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

    // obecný doplněk
    GroovyItem geoType = GroovyUtils.findFirstItem(ae, "PT_BODY", GroovyPart.PreferredFilter.ALL, "GEO_TYPE")
    // TODO vytvořit seznam výjimek
    if (geoType != null) {
        GroovyItem obecItem = new GroovyItem("NM_SUP_GEN", null, geoType.getValue())
        items.add(obecItem)
    }

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