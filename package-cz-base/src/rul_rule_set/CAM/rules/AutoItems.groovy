package rul_rule_set.CAM.rules

import java.util.Arrays
import java.util.List

import cz.tacr.elza.groovy.GroovyAe
import cz.tacr.elza.groovy.GroovyPart
import cz.tacr.elza.groovy.GroovyItem
import cz.tacr.elza.groovy.GroovyUtils

import cz.tacr.elza.domain.ArrDataRecordRef;
import cz.tacr.elza.domain.ArrDataString;

import cz.tacr.elza.service.cache.AccessPointCacheProvider
import cz.tacr.elza.service.cache.CachedAccessPoint

import cz.tacr.elza.exception.codes.BaseCode
import cz.tacr.elza.exception.ObjectNotFoundException

return generate(AE, AP_CACHE_PROVIDER)

// Získání seznamu názvů míst do celé hloubky vnoření: Kladno, Kladno, Česko
static String getGeoName(GroovyItem item, AccessPointCacheProvider apcp) {

    // seznam vyloučených typů území
    List<String> excludeTerritory = Arrays.asList(
        "GT_LAND",       // země
        "GT_AREA",       // oblast
        "GT_VOJVODSTVI", // vojvodství
        "GT_COUNTY",     // župa
        "GT_ADMREGION"   // kraj
        )

    // seznam omezených zemí
	List<String> excludeLands = Arrays.asList(
        "slovensko",
		"polsko",
		"rakousko",
		"německo",
		"maďarsko",
		"ukrajina"
        )

    // získání seznamu geografických objektů a názvu země
    String country
    String value
    int limitItems = 10
    ArrDataRecordRef dataRecord
    CachedAccessPoint cap = item.getAccessPoint()
    List<CachedAccessPoint> caps = new ArrayList<>()
    while (cap != null && limitItems > 0) {
        caps.add(cap)
        limitItems--

        String geoType = GroovyUtils.findItemSpecCodeByItemTypeCode(cap, "GEO_TYPE")
        if (Objects.equals(geoType, "GT_COUNTRY")) {
            country = GroovyUtils.findDataByRulItemTypeCode(cap, GroovyPart.PreferredFilter.YES, "NM_MAIN")
            				.getStringValue()
        }

        dataRecord = GroovyUtils.findDataByRulItemTypeCode(cap, GroovyPart.PreferredFilter.ALL, "GEO_ADMIN_CLASS")
        cap = dataRecord == null? null : apcp.get(dataRecord.getRecordId())
    }

    // převést seznam CachedAccessPoint na řetězec
    for (CachedAccessPoint ap : caps) {
        ArrDataString dataString = GroovyUtils.findDataByRulItemTypeCode(ap, GroovyPart.PreferredFilter.YES, "NM_MAIN")
        String geoType = GroovyUtils.findItemSpecCodeByItemTypeCode(ap, "GEO_TYPE")
        //System.out.println(geoType)
        // v České republice tento typ (GT_ADMREGION) nevykazujeme
        if (isCesko(country) && Objects.equals(geoType, "GT_ADMREGION")) {
            continue
        }
        // nezobrazené typy území pro seznam zemí
        if (excludeLands.contains(country.toLowerCase()) && excludeTerritory.contains(geoType)) {
            continue
        }
        if (Objects.equals(geoType, "GT_CONTINENT") || Objects.equals(geoType, "GT_PLANET")) {
            break;
        }
        //System.out.println(geoType)
        if (value == null) {
        	value = dataString.getStringValue()
        } else {
            value += ", " + dataString.getStringValue()
        }
    }

    return value
}

static boolean isCesko(String country) {
    if (country != null) {
        return Objects.equals(country.toLowerCase(), "česko")
    }
    return false
}

// konverze řetězců pro výstup
static String convertAuthString(GroovyItem item) {
    Integer typeId = item.getApTypeId()
    if (typeId != null) {
        String nmMain = GroovyUtils.findDataByRulItemTypeCode(item, GroovyPart.PreferredFilter.YES, "NM_MAIN").getStringValue()
        ArrDataString dataString = GroovyUtils.findDataByRulItemTypeCode(item, GroovyPart.PreferredFilter.YES, "NM_MINOR")
        String nmMinor = dataString == null ? null : dataString.getStringValue()
        // konverze pro osoby: Svoboda Karel -> Karel Svoboda
        if (GroovyUtils.hasParent(typeId, "PERSON")) {
            return nmMinor + " " + nmMain
        }
        // konverze pro korporace : Hlavní část jména. Vedlejší část jména
        if (GroovyUtils.hasParent(typeId, "PARTY_GROUP")) {
            return nmMain + (nmMinor == null? "" : ". " + nmMinor)
        }
    }
    return item.getValue()
}

// přidáme nový záznam do seznamu nebo přidání hodnoty k existující
static void addGroovyItem(List<GroovyItem> items, GroovyItem groovyItem) {
    addGroovyItem(items, groovyItem, "; ")
}

// přidáme nový záznam nebo hodnoty se speciálním oddělovačem
static void addGroovyItem(List<GroovyItem> items, GroovyItem groovyItem, String separator) {
    for (GroovyItem item : items) {
        if (item.getTypeCode().equals(groovyItem.getTypeCode())) {
            item.addValue(groovyItem, separator)
            return
        }
    }
    items.add(groovyItem)
}

// získání zobrazení separátoru
static String getSeperator(GroovyItem item) {
    Integer typeId = item.getApTypeId()
    if (typeId != null) {
        // osoby jsou od sebe odděleny písmenem "a"
        if (GroovyUtils.hasParent(typeId, "PERSON")) {
            return " a "
        }
    }
    // korporace a další jsou odděleny ";"
    return "; "
}

static List<GroovyItem> generate(final GroovyAe ae, final AccessPointCacheProvider apcp) {
    List<GroovyItem> items = new ArrayList<>()

    // dočasně, jen pro ladění
    for (GroovyPart part : ae.getParts()) {
        //System.out.println(part.getPartType().getCode())
        for (GroovyItem item : part.getItems()) {
            //System.out.println(item)
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
        "GT_MUNIPDISTR",
        "GT_MUNIPPART",
        "GT_CADASTRALTERRITORY",
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
    GroovyItem genItem = GroovyUtils.findFirstItem(ae, "PT_BODY", GroovyPart.PreferredFilter.ALL, "GEO_TYPE")
    if (genItem != null) {
        if (!excludeTerritory.contains(genItem.getSpecCode())) {
            GroovyItem obecItem = new GroovyItem("NM_SUP_GEN", null, genItem.getValue())
            items.add(obecItem)
        }
    }

    // chronologický doplněk
    GroovyItem chroItem = new GroovyItem("NM_SUP_CHRO", null, nmSupChro)
    if (!chroItem.getValue().isEmpty()) {
        // pokud se jedná o geo objekt
        if (GroovyUtils.hasParent(ae.getAeType(), "GEO")) {
            // pokud objekt je GEO_UNIT
            if (ae.getAeType().equals("GEO_UNIT")) {
                // pokud objekt zanikl
                if (to != null) {
                    // pokud objekt je zařazen do seznamu excludeTerritory
                    if (genItem != null && disappearedTerritory.contains(genItem.getSpecCode())) {
                    	items.add(new GroovyItem("NM_SUP_CHRO", null, "zaniklo"))
                	} else {
	                    items.add(chroItem)
                	}    
                }
                // pokud objekt nezanikl - nic neoznačujeme
            }
        } else {
            items.add(chroItem)
        }
    }

    // geografický doplněk
    GroovyItem geo = GroovyUtils.findFirstItem(ae, "PT_BODY", GroovyPart.PreferredFilter.ALL, "GEO_ADMIN_CLASS")
    if (geo != null) {
        if (geo.getValue() != null) {
            if (geo.getIntValue() > 0) {
                String geoName = getGeoName(geo, apcp)
				if (geoName != null) {
                	GroovyItem geoItem = new GroovyItem("NM_SUP_GEO", null, geoName)
                	items.add(geoItem)
				}
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
                if (rel.getIntValue() > 0) {
                	String geoName = getGeoName(rel, apcp)
                	if (geoName != null) {
	                    GroovyItem relGeoItem = new GroovyItem("NM_SUP_GEO", null, geoName)
                    	addGroovyItem(items, relGeoItem)
                	}
                } else {
                    throw new ObjectNotFoundException("Entita nebyla načtena z externího systému", BaseCode.DB_INTEGRITY_PROBLEM)
                        .set("entityId", rel.getValue())
                }
            }
        }
        // autor/tvůrce pro podtřídu autorská a umělecká díla
        if (ae.getAeType().equals("ARTWORK_ARTWORK")) {
            if (rel.getSpecCode().equals("RT_AUTHOR")) {
                GroovyItem itemAuth = new GroovyItem("NM_AUTH", null, convertAuthString(rel))
                addGroovyItem(items, itemAuth, getSeperator(rel))
            }
        }
    }

    return items
}