package rul_rule_set.CAM.rules

import java.util.Collections
import cz.tacr.elza.domain.ApItem
import groovy.transform.Field

@Field static String[] typesArray = [
        "COORD_POINT", "COORD_BORDER", "COORD_NOTE",
        "GEO_TYPE", "GEO_ADMIN_CLASS", "NOTE", 
        "IDN_TYPE", "IDN_VALID_FROM", "IDN_VALID_TO", 
        "IDN_VALUE", "IDN_VERIFIED", 
        "NM_MAIN", "NM_MINOR", "NM_SUP_GEN", "NM_SUP_GEO", 
        "NM_SUP_CHRO", "NM_SUP_DIFF",
        "NM_ORDER", "NM_AUTH", 
        "NM_TYPE", "NM_USED_FROM", "NM_USED_TO", "NM_LANG", 
        "NM_DEGREE_PRE", "NM_DEGREE_POST",
        "BRIEF_DESC", 
        "LANG", 
        "EV_TYPE", "EV_BEGIN", "EV_END", 
        "REL_ENTITY", "REL_BEGIN", "REL_END", 
        "CRE_CLASS", "CRE_TYPE", "CRE_DATE", 
        "EXT_CLASS", "EXT_TYPE", "EXT_DATE", 
        "CORP_PURPOSE", "FOUNDING_NORMS", "SCOPE_NORMS", 
        "CORP_STRUCTURE", "NOTE_INTERNAL", "SOURCE_INFO", 
        "SOURCE_LINK", "HISTORY", "GENEALOGY", "BIOGRAPHY", 
        "DESCRIPTION"    
    ];

Set<Integer> itemTypes = createItemTypes(DATA_PROVIDER);
Set<Integer> itemSpecs = createItemSpecs(DATA_PROVIDER);
Map<String, Integer> itemCodeMap = createItemCodeMap(DATA_PROVIDER);

// Filter to include only known types and specs
List<ApItem> result = new ArrayList<>();
for(def item: ITEMS) {
    if(itemTypes.contains(item.getItemTypeId())) {
        // check spec
        if(item.getItemSpec()==null) {
            result.add(item);
        } else 
        if(itemSpecs.contains(item.getItemSpecId())) {
            result.add(item);
        } else {
            // missing spec
            // 
            // well known type is used with other meaning
            // -> part as whole cannot be sent
            // e.g. PT_IDENT with some unsupported ident type
            return Collections.emptyList(); 
        }
    } else {
        // unknown item type
        //
        // if some unknown type is removed
        // part still can be processed
    }
}

// TODO: Check if part includes key types and can be posted

return result;

//@Field static Set<Integer> itemTypes = createItemTypes()
//@Field static Set<Integer> itemSpecs = createItemSpecs()

Map<String, Integer> createItemCodeMap(dataProvider) {
    Map<String, Integer> codeMap = new HashMap<>();

    typesArray.each {
        {
            t ->
            def itemType = dataProvider.getItemType(t); 
            codeMap.put(itemType.getCode(), itemType.getItemTypeId());
        }
    }

    return codeMap;
}

Set<String> createItemTypes(dataProvider) {
    Set<Integer> itemTypes = new HashSet<>();
    
    typesArray.each {
        t ->  itemTypes.add(dataProvider.getItemType(t).getItemTypeId())
    }

    return itemTypes;
}

Set<String> createItemSpecs(dataProvider) {
    String[] specArray = [
        "CRC_BIRTH", "CRC_FIRSTMBIRTH", "CRC_FIRSTWMENTION", "CRC_BEGINSCOPE", "CRC_RISE", 
        "CRC_ORIGIN", "CRC_BEGINVALIDNESS", 
        "CRT_UNSPECIFIED", "CRT_VALIDITYBEGIN", "CRT_FIRSTREALIZATION", "CRT_EDITION", 
        "CRT_CREATION", "CRT_COMMENCEMENT", "CRT_RECORDSENTRY", "CRT_PREDECESSOR", "CRT_ESTAB", 
        "ET_MEMBERSHIP", "ET_MILESTONES", "ET_MARRIAGE", "ET_REASSIGN", "ET_STUDY", 
        "ET_AWARD", "ET_HOBBY", "ET_JOB", 
        "EXC_END", "EXC_ENDVALIDNESS", "EXC_LASTWMENTION", "EXC_ENDSCOPE", "EXC_DEATH", 
        "EXC_LASTMDEATH", "EXC_EXTINCTION", 
        "EXT_OTHER", "EXT_UNSPECIFIED", "EXT_LASTREALIZATION", "EXT_STOPPUBLISHING", 
        "EXT_ACTIVITYEND", "EXT_REGISTRYDELETION", "EXT_MININGACTIVITY", "EXT_INDUSTRIALACTIVITY", 
        "EXT_BATTLE", "EXT_DISPLACEMENT", "EXT_WATERWORK", "EXT_MILITARYAREA", "EXT_DISASTER", 
        "EXT_CANCELCHANGE", "EXT_CHANGE", "EXT_CANCEL", "EXT_DESTRUCTION", 
        "GT_ARCHSITE", "GT_AUTONOMOUSPART", "GT_TERRITORIALUNIT", "GT_MUNIPPART",
        "GT_DEPARTEMENT", "GT_GALAXY", "GT_MOUNTAIN", "GT_SHIRE", "GT_STAR", "GT_PROTNATPART", 
        "GT_CAVE", "GT_LAKE", "GT_COSMOSPART", "GT_WATERAREA", "GT_NATUREPART", "GT_OTHERAREA", 
        "GT_NAMEDFORMATION", "GT_NATFORMATION", "GT_CANAL", "GT_CANTON", "GT_CADASTRALTERRITORY", 
        "GT_CONTINENT", "GT_HILL", "GT_ADMREGION", "GT_CRATER", "GT_FOREST", "GT_FORESTPARK", 
        "GT_MOON", "GT_CITYDISTRICT", "GT_PEAKBOG", "GT_SEA", "GT_HEADLAND", 
        "GT_WATERFRONT", "GT_SQUARE", "GT_ETHNOGRAPHICAREA", "GT_LOWLAND", 
        "GT_SETTLEMENT", "GT_OASIS", "GT_MUNIP", "GT_MUNIPDISTR", "GT_AREA", 
        "GT_OCEAN", "GT_DISTRICT", "GT_OKRUH", "GT_ISLAND", "GT_HILLYAREA", 
        "GT_MONUMENTZONE", "GT_MANOR", "GT_RAPIDS", "GT_PLANET", "GT_PLATEAU", 
        "GT_MOUNTAINS", "GT_PENINSULA", "GT_BROOK", "GT_DESERT", 
        "GT_CATCHMENTAREA", "GT_VIRGINFOREST", "GT_SPRING", "GT_CHASM", 
        "GT_PROVINCE", "GT_NAVIGATIONCANAL", "GT_PASS", "GT_REGION", 
        "GT_RECREATIONAREA", "GT_POND", "GT_RIVER", "GT_ROCKCITY", 
        "GT_ROCK", "GT_TREEGROUP", "GT_VOLCANO", "GT_COURTDISTRICT", 
        "GT_CONSTELLATION", "GT_ARCHIPELAGO", "GT_RAVINE", 
        "GT_COUNTRY", "GT_TREE", "GT_CLIMATICPHEN", "GT_VALLEY", "GT_STREET", 
        "GT_CLIFF", "GT_SEABOTSHAPE", "GT_DAM", "GT_WATERFALL",
        "GT_MILITARYAREA", "GT_VOJVODSTVI", "GT_HIGHLANDS", "GT_GARDEN",
        "GT_BAY", "GT_LAND", "GT_COUNTY", 
        "ARCHNUM", "VAT", "IC", "INTERPI", "ISO3166_2", "ISO3166_3", 
        "ISO3166_NUM", "ISO3166_PART2", "CZ_RETRO", "TAXONOMY", "NUTSLAU", 
        "ORCID_ID", "PEVA", "RUIAN", "RUIAN", "AUT", "VIAF", "LCNAF", 
        "LNG_heb", 
        "LNG_per", 
        "LNG_abk", 
        "LNG_afr", 
        "LNG_akk", 
        "LNG_alb", 
        "LNG_gsw", 
        "LNG_amh", 
        "LNG_eng", 
        "LNG_ara", 
        "LNG_arc", 
        "LNG_arm", 
        "LNG_asm", 
        "LNG_0as", 
        "LNG_aze", 
        "LNG_0ba", 
        "LNG_bur", 
        "LNG_baq", 
        "LNG_bak", 
        "LNG_bel", 
        "LNG_ben", 
        "LNG_dzo", 
        "LNG_bih", 
        "LNG_afa", 
        "LNG_tut", 
        "LNG_aav", 
        "LNG_map", 
        "LNG_bat", 
        "LNG_dra", 
        "LNG_esx", 
        "LNG_gem", 
        "LNG_0ni", 
        "LNG_inc", 
        "LNG_ine", 
        "LNG_iir", 
        "LNG_0nz", 
        "LNG_ccs", 
        "LNG_cel", 
        "LNG_crs", 
        "LNG_xgn", 
        "LNG_mun", 
        "LNG_nic", 
        "LNG_roa", 
        "LNG_sem", 
        "LNG_ccn", 
        "LNG_sla", 
        "LNG_tai", 
        "LNG_sit", 
        "LNG_trk", 
        "LNG_0uj", 
        "LNG_urj", 
        "LNG_brx", 
        "LNG_bos", 
        "LNG_bre", 
        "LNG_bul", 
        "LNG_cze", 
        "LNG_chi", 
        "LNG_dan", 
        "LNG_doi", 
        "LNG_nds", 
        "LNG_egy", 
        "LNG_epo", 
        "LNG_est", 
        "LNG_fao", 
        "LNG_fin", 
        "LNG_fre", 
        "LNG_fry", 
        "LNG_gag", 
        "LNG_glg", 
        "LNG_kal", 
        "LNG_geo", 
        "LNG_guj", 
        "LNG_hau", 
        "LNG_zkz", 
        "LNG_hin", 
        "LNG_dut", 
        "LNG_hit", 
        "LNG_hrv", 
        "LNG_ido", 
        "LNG_ind", 
        "LNG_ina", 
        "LNG_ile", 
        "LNG_gle", 
        "LNG_ice", 
        "LNG_ita", 
        "LNG_jpn", 
        "LNG_yid", 
        "LNG_0jd", 
        "LNG_kan", 
        "LNG_kas", 
        "LNG_csb", 
        "LNG_cat", 
        "LNG_kaz", 
        "LNG_khm", 
        "LNG_kok", 
        "LNG_cop", 
        "LNG_kor", 
        "LNG_cos", 
        "LNG_crh", 
        "LNG_kur", 
        "LNG_kir", 
        "LNG_lao", 
        "LNG_lat", 
        "LNG_lit", 
        "LNG_liv", 
        "LNG_lav", 
        "LNG_ltz", 
        "LNG_dsb", 
        "LNG_hsb", 
        "LNG_hun", 
        "LNG_mac", 
        "LNG_mal", 
        "LNG_may", 
        "LNG_mlt", 
        "LNG_mni", 
        "LNG_mar", 
        "LNG_mol", 
        "LNG_mon", 
        "LNG_ger", 
        "LNG_nor", 
        "LNG_oci", 
        "LNG_nep", 
        "LNG_pan", 
        "LNG_pus", 
        "LNG_pol", 
        "LNG_por", 
        "LNG_roh", 
        "LNG_rom", 
        "LNG_rum", 
        "LNG_0ru", 
        "LNG_rus", 
        "LNG_gre", 
        "LNG_san", 
        "LNG_sat", 
        "LNG_snd", 
        "LNG_sin", 
        "LNG_gla", 
        "LNG_slo", 
        "LNG_slv", 
        "LNG_0sl", 
        "LNG_0sc", 
        "LNG_srp", 
        "LNG_hbo", 
        "LNG_peo", 
        "LNG_chu", 
        "LNG_pal", 
        "LNG_sux", 
        "LNG_swa", 
        "LNG_spa", 
        "LNG_swe", 
        "LNG_tgk", 
        "LNG_tam", 
        "LNG_tat", 
        "LNG_tel", 
        "LNG_tha", 
        "LNG_tib", 
        "LNG_tur", 
        "LNG_tuk", 
        "LNG_uig", 
        "LNG_ukr", 
        "LNG_urd", 
        "LNG_ori", 
        "LNG_uzb", 
        "LNG_wel", 
        "LNG_vie", 
        "LNG_vol", 
        "NT_AUTHORCIPHER", "NT_RELIGIOUS", "NT_EQUIV", "NT_HISTORICAL", 
        "NT_FORMER", "NT_HOMONYMUM", "NT_INCIPIT", "NT_INVERTED", 
        "NT_ONLYKNOWN", "NT_ORIGINAL", "NT_INAPPROPRIATE", "NT_TERM", 
        "NT_PLURAL", "NT_OTHERRULES", "NT_HONOR", "NT_TAKEN", "NT_TRANSLATED", 
        "NT_ALIAS", "NT_ACCEPTED", "NT_DIRECT", "NT_PSEUDONYM", "NT_NATIV", 
        "NT_SINGULAR", "NT_ACTUAL", "NT_SECULAR", "NT_ARTIFICIAL", 
        "NT_OFFICIAL", "NT_NARROWER", "NT_SIMPLIFIED", "NT_GARBLED", 
        "NT_ACRONYM", "NT_FOLK", 
        "RT_AUTHOR", "RT_AUTHOROFCHANGE", "RT_GRANDMOTHER", 
        "RT_BROTHER", "RT_HECOUSIN", "RT_WHOLE", "RT_CEREMONY", 
        "RT_MEMBERORG", "RT_RELATIONS", "RT_GRANDFATHER", "RT_DOCUMENT", 
        "RT_ENTITYEND", "RT_ENTITYBIRTH", "RT_ENTITYDEATH", 
        "RT_ENTITYRISE", "RT_ENTITYORIGIN", "RT_ENTITYEXTINCTION", 
        "RT_FUNCTION", "RT_GEOSCOPE", "RT_MILESTONE", "RT_ISPART", 
        "RT_ISMEMBER", "RT_GENUSMEMBER", "RT_OTHERNAME", "RT_ANCESTOR", 
        "RT_ACTIVITYCORP", "RT_LIQUIDATOR", "RT_OWNER", "RT_HOLDER", 
        "RT_HUSBAND", "RT_WIFE", "RT_MOTHER", "RT_PLACE", "RT_VENUE", 
        "RT_PLACEEND", "RT_PLACESTART", "RT_SUPCORP", "RT_SUPTERM",
        "RT_SENIOR", "RT_SUCCESSOR", "RT_ACTIVITYFIELD", "RT_STUDYFIELD", 
        "RT_AWARD", "RT_ORGANIZER", "RT_FATHER", "RT_PARTNER", 
        "RT_SHEPARTNER", "RT_SUBTERM", "RT_NAMEDAFTER", "RT_LASTKMEMBER", 
        "RT_FIRSTKMEMBER", "RT_PREDECESSOR", "RT_AFFILIATION", "RT_SISTER", 
        "RT_SHECOUSIN", "RT_RESIDENCE", "RT_COMPEVENT", "RT_RELATED", 
        "RT_RELATEDTERM", "RT_COLLABORATOR", "RT_SCHOOLMATE", "RT_UNCLE", 
        "RT_SCHOOL", "RT_BROTHERINLAW", "RT_SISTERINLAW", "RT_CATEGORY", 
        "RT_THEME", "RT_AUNT", "RT_FATHERINLAW", "RT_MOTHERINLAW", 
        "RT_TEACHER", "RT_GRANTAUTH", "RT_STORAGE", "RT_LOCATION", 
        "RT_OBJECT", "RT_ACTIVITIES", "RT_FOUNDER", "RT_EMPLOYER", 
        "RT_ROOF", "RT_REPRESENT", "RT_NAMECHANGE", 
        "RT_MENTION", "RT_ORIGINATOR", "RT_GEOPARTNER"
    ];
    Set<Integer> itemSpecs = new HashSet<>();

    specArray.each {
        t ->  itemSpecs.add(dataProvider.getItemSpec(t).getItemSpecId())
    }

    return itemSpecs;
}
