package rul_rule_set.CAM.rule

import groovy.transform.Field
import java.time.LocalDateTime
import java.util.Arrays
import java.util.List

import org.apache.commons.lang3.StringUtils

import cz.tacr.elza.groovy.GroovyAe
import cz.tacr.elza.groovy.GroovyPart
import cz.tacr.elza.groovy.GroovyItem
import cz.tacr.elza.groovy.GroovyUtils

import cz.tacr.elza.core.data.StaticDataProvider;

import cz.tacr.elza.domain.ArrDataRecordRef;
import cz.tacr.elza.domain.ArrDataString;

import cz.tacr.elza.drools.model.DrlUtils;

import cz.tacr.elza.exception.codes.BaseCode
import cz.tacr.elza.exception.ObjectNotFoundException

import cz.tacr.elza.service.cache.AccessPointCacheProvider
import cz.tacr.elza.service.cache.CachedAccessPoint

@Field static StaticDataProvider sdp;
@Field static boolean debug = false;
        
sdp = DATA_PROVIDER;
return generate(AE, AP_CACHE_PROVIDER);

// Získání seznamu názvů míst směrem k rodiči
// Priklad: Kladno, Kladno, Česko
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
    String country = null;
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
        if(dataRecord==null) {
            break;
        }
        cap = apcp.get(dataRecord.getRecordId());
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
        if (country!=null && excludeLands.contains(country.toLowerCase()) && excludeTerritory.contains(geoType)) {
            continue
        }
        if (Objects.equals(geoType, "GT_CONTINENT") || Objects.equals(geoType, "GT_PLANET")) {
            break;
        }
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

// generovani obecneho doplnku
GroovyItem generateObecnyDoplnek(final GroovyAe ae) {
    GroovyItem genItem = GroovyUtils.findFirstItem(ae, "PT_BODY", GroovyPart.PreferredFilter.ALL, "GEO_TYPE")
    if (genItem != null) {
        // seznam typů geo objektů, kde se automaticky nevytváří obecný doplněk
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
        
        if (!excludeTerritory.contains(genItem.getSpecCode())) {
             
            def value;
            switch(genItem.getSpecCode()) {
            case "GT_AUTONOMOUSPART":
                value = "autonomní část"; 
                break;
            case "GT_TERRITORIALUNIT":
                value = "část státu";
                break;
            default:
                // standardne se prevezme
                value = genItem.getValue();
            }
            return new GroovyItem(sdp.getItemTypeByCode("NM_SUP_GEN"), null, value)
        }
    }
    return null;
}

// generovani chronologickeho doplnku
GroovyItem generateChronolDoplnek(final GroovyAe ae) {
    
    // načtení vzniku a zániku pro chronologický doplněk
    GroovyItem from = GroovyUtils.findFirstItem(ae, "PT_CRE", GroovyPart.PreferredFilter.ALL, "CRE_DATE")
    GroovyItem to = GroovyUtils.findFirstItem(ae, "PT_EXT", GroovyPart.PreferredFilter.ALL, "EXT_DATE")
    if(from==null&&to==null) {
        return null;
    }

    GroovyItem fromClass = GroovyUtils.findFirstItem(ae, "PT_CRE", GroovyPart.PreferredFilter.ALL, "CRE_CLASS")
    GroovyItem toClass = GroovyUtils.findFirstItem(ae, "PT_EXT", GroovyPart.PreferredFilter.ALL, "EXT_CLASS")
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

    // Pokud vůbec není uveden vznik nebo je uveden vznik bez datace a u zániku není uvedena datace 
    // nesmí být chronologický doplněk uveden, respektive je kontrolována jeho „prázdná“ hodnota. 
    // Výjimkou je pouze hodnota „zaniklo“ u entit podřídy GEO_UNIT, viz výjimka výše.
    if (GroovyUtils.hasParent(ae.getAeType(), "GEO")) {
        // pokud objekt je GEO_UNIT
        if (ae.getAeType().equals("GEO_UNIT")) {
            // pokud objekt zanikl
            if (toClass != null) {
                GroovyItem geoType = GroovyUtils.findFirstItem(ae, "PT_BODY", GroovyPart.PreferredFilter.ALL, "GEO_TYPE");                
                // pokud objekt je zařazen do seznamu
                if(geoType!=null&&
                    Arrays.asList("GT_MUNIPDISTR", "GT_MUNIP", "GT_MILITARYAREA", "GT_CADASTRALTERRITORY",
                        "GT_MUNIPPART", "GT_STREET", "GT_SQUARE", "GT_WATERFRONT", "GT_SETTLEMENT").contains(geoType.specCode)) {
                    return new GroovyItem(sdp.getItemTypeByCode("NM_SUP_CHRO"), null, "zaniklo");
                }                
            }
            // pokud objekt nezanikl a není znám vznik -> doplněk není
            if(from==null) {
                return null;
            }
        }
    }

    // definice prefixu, když je shodný rok u datace od i do
    def generYearEqual = false
    //definice prefixu, když je shodný rok u datace od i do, a příznaku, zda se má generovat jen jako rok nebo jako od-do
    def prefixYearEqual = ""
    def sameEstimates = from != null && to != null &&
            ((from.getUnitdateValue().getValueFromEstimated() && to.getUnitdateValue().getValueToEstimated())
            || (!from.getUnitdateValue().getValueFromEstimated() && !to.getUnitdateValue().getValueToEstimated()))
    if(sameEstimates) {
        generYearEqual = true
        if (prefixFrom == "uváděno od " && prefixTo == "uváděno do ") {
            prefixYearEqual = "uváděno "
        } else if (prefixFrom == "působnost od " && prefixTo == "působnost do ") {
            prefixYearEqual = "působnost "
        }
    }

    // vytvářet hodnotu pro chronologický doplněk
    String nmSupChro = GroovyUtils.formatUnitdate(from, to)
                .prefixFrom(prefixFrom)
                .prefixTo(prefixTo)
                .formatYear()
                .yearEqual(generYearEqual, prefixYearEqual)
                .build()
     
    boolean addQuestionmark = false;
    // Pokud existuje událost zániku, ale není vyplněna datace – musí být použit 
    // znak „?“ signalizující neznámé datum zániku.          
    if (from != null && to == null) {
        if(toClass != null) {
            addQuestionmark = true;
        } else
        // Není uveden zánik, ale může být osoba
        if (ae.getAeType().equals("PERSON_INDIVIDUAL")) {
          // Pokud je starsi nez 120let
          if(DrlUtils.lessThan(from.getValue(), LocalDateTime.now().minusYears(120))) {
            addQuestionmark = true;
          }
        }
    }
    if(addQuestionmark) {
       nmSupChro += "?";
    }


    // chronologický doplněk
    if (StringUtils.isBlank(nmSupChro)) {
        // Asi nikdy nenastatne
        return null;
    }

    return new GroovyItem(sdp.getItemTypeByCode("NM_SUP_CHRO"), null, nmSupChro);
}

// generovani doplnku
List<GroovyItem> generate(final GroovyAe ae, final AccessPointCacheProvider apcp) {
    List<GroovyItem> items = new ArrayList<>()

    // dočasně, jen pro ladění
    if(debug) {
      for (GroovyPart part : ae.getParts()) {
          System.out.println(part.getPartType().getCode())
          for (GroovyItem item : part.getItems()) {
              System.out.println(item);
          }
      }
    }

    // obecný doplněk
    GroovyItem obecnyDoplnek = generateObecnyDoplnek(ae)
    if(obecnyDoplnek!=null) {
        items.add(obecnyDoplnek);
    }

    // geografický doplněk
    GroovyItem geo = GroovyUtils.findFirstItem(ae, "PT_BODY", GroovyPart.PreferredFilter.ALL, "GEO_ADMIN_CLASS")
    if (geo != null) {
        if (geo.getIntValue() > 0) {
            String geoName = getGeoName(geo, apcp)
            if (geoName != null) {
                GroovyItem geoItem = new GroovyItem(sdp.getItemTypeByCode("NM_SUP_GEO"), null, geoName)
                items.add(geoItem)
            }
        }
        // If parent now knonwn -> cannot create sup_geo
        // } else {
        //    throw new ObjectNotFoundException("Entita nebyla načtena z externího systému", BaseCode.DB_INTEGRITY_PROBLEM)
        //    .set("entityId", geo.getValue())
        // }
    }
    
    // chronologicky doplnek
    GroovyItem chronolDoplnek = generateChronolDoplnek(ae);
    if(chronolDoplnek!=null) {
        items.add(chronolDoplnek);
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
	                    GroovyItem relGeoItem = new GroovyItem(sdp.getItemTypeByCode("NM_SUP_GEO"), null, geoName)
                    	addGroovyItem(items, relGeoItem)
                	}
                }
                // if parent not known -> cannot create sup_geo
                //} else {
                //    throw new ObjectNotFoundException("Entita nebyla načtena z externího systému", BaseCode.DB_INTEGRITY_PROBLEM)
                //        .set("entityId", rel.getValue())
                //}
            }
        }
        // autor/tvůrce pro podtřídu autorská a umělecká díla
        if (ae.getAeType().equals("ARTWORK_ARTWORK")) {
            if (rel.getSpecCode().equals("RT_AUTHOR")) {
                GroovyItem itemAuth = new GroovyItem(sdp.getItemTypeByCode("NM_AUTH"), null, convertAuthString(rel))
                addGroovyItem(items, itemAuth, getSeperator(rel))
            }
        }
    }

    return items
}
