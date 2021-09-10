package rul_rule_set.CAM.rules

import cz.tacr.elza.api.ApExternalSystemType
import groovy.transform.Field

return findItemSpecCode(EXT_SYSTEM_TYPE, ITEM_SPEC_CODE)

@Field static Map<String, Map<String, String>> itemSpecMap = createItemSpecMap()


static String findItemSpecCode(final String extSystemType, final String itemSpecCode) {
    String resultItemSpecCode = null
    Map<String, String> systemItemSpecMap = itemSpecMap.getOrDefault(extSystemType, null)

    if (systemItemSpecMap != null) {
        resultItemSpecCode = systemItemSpecMap.getOrDefault(itemSpecCode, null)
    }

    return resultItemSpecCode
}

static Map<String, Map<String, String>> createItemSpecMap() {
    Map<String, String> camItemSpecMap = createCamItemSpecMap()

    Map<String, Map<String, String>> itemSpecMap = new HashMap<>()
    itemSpecMap.put(ApExternalSystemType.CAM.toString(), camItemSpecMap)
    itemSpecMap.put(ApExternalSystemType.CAM_COMPLETE.toString(), camItemSpecMap)
    itemSpecMap.put(ApExternalSystemType.CAM_UUID.toString(), camItemSpecMap)
    return itemSpecMap
}

static Map<String, String> createCamItemSpecMap() {
    Map<String, String> camItemSpecMap = new HashMap<>()
    camItemSpecMap.put("CRC_BIRTH", "CRC_BIRTH")
    camItemSpecMap.put("CRC_FIRSTMBIRTH", "CRC_FIRSTMBIRTH")
    camItemSpecMap.put("CRC_FIRSTWMENTION", "CRC_FIRSTWMENTION")
    camItemSpecMap.put("CRC_BEGINSCOPE", "CRC_BEGINSCOPE")
    camItemSpecMap.put("CRC_RISE", "CRC_RISE")
    camItemSpecMap.put("CRC_ORIGIN", "CRC_ORIGIN")
    camItemSpecMap.put("CRC_BEGINVALIDNESS", "CRC_BEGINVALIDNESS")
    camItemSpecMap.put("CRT_UNSPECIFIED", "CRT_UNSPECIFIED")
    camItemSpecMap.put("CRT_VALIDITYBEGIN", "CRT_VALIDITYBEGIN")
    camItemSpecMap.put("CRT_FIRSTREALIZATION", "CRT_FIRSTREALIZATION")
    camItemSpecMap.put("CRT_EDITION", "CRT_EDITION")
    camItemSpecMap.put("CRT_CREATION", "CRT_CREATION")
    camItemSpecMap.put("CRT_COMMENCEMENT", "CRT_COMMENCEMENT")
    camItemSpecMap.put("CRT_RECORDSENTRY", "CRT_RECORDSENTRY")
    camItemSpecMap.put("CRT_PREDECESSOR", "CRT_PREDECESSOR")
    camItemSpecMap.put("CRT_ESTAB", "CRT_ESTAB")
    camItemSpecMap.put("ET_MEMBERSHIP", "ET_MEMBERSHIP")
    camItemSpecMap.put("ET_MILESTONES", "ET_MILESTONES")
    camItemSpecMap.put("ET_MARRIAGE", "ET_MARRIAGE")
    camItemSpecMap.put("ET_REASSIGN", "ET_REASSIGN")
    camItemSpecMap.put("ET_STUDY", "ET_STUDY")
    camItemSpecMap.put("ET_AWARD", "ET_AWARD")
    camItemSpecMap.put("ET_HOBBY", "ET_HOBBY")
    camItemSpecMap.put("ET_JOB", "ET_JOB")
    camItemSpecMap.put("EXC_END", "EXC_END")
    camItemSpecMap.put("EXC_ENDVALIDNESS", "EXC_ENDVALIDNESS")
    camItemSpecMap.put("EXC_LASTWMENTION", "EXC_LASTWMENTION")
    camItemSpecMap.put("EXC_ENDSCOPE", "EXC_ENDSCOPE")
    camItemSpecMap.put("EXC_DEATH", "EXC_DEATH")
    camItemSpecMap.put("EXC_LASTMDEATH", "EXC_LASTMDEATH")
    camItemSpecMap.put("EXC_EXTINCTION", "EXC_EXTINCTION")
    camItemSpecMap.put("EXT_OTHER", "EXT_OTHER")
    camItemSpecMap.put("EXT_UNSPECIFIED", "EXT_UNSPECIFIED")
    camItemSpecMap.put("EXT_LASTREALIZATION", "EXT_LASTREALIZATION")
    camItemSpecMap.put("EXT_STOPPUBLISHING", "EXT_STOPPUBLISHING")
    camItemSpecMap.put("EXT_ACTIVITYEND", "EXT_ACTIVITYEND")
    camItemSpecMap.put("EXT_REGISTRYDELETION", "EXT_REGISTRYDELETION")
    camItemSpecMap.put("EXT_MININGACTIVITY", "EXT_MININGACTIVITY")
    camItemSpecMap.put("EXT_INDUSTRIALACTIVITY", "EXT_INDUSTRIALACTIVITY")
    camItemSpecMap.put("EXT_BATTLE", "EXT_BATTLE")
    camItemSpecMap.put("EXT_DISPLACEMENT", "EXT_DISPLACEMENT")
    camItemSpecMap.put("EXT_WATERWORK", "EXT_WATERWORK")
    camItemSpecMap.put("EXT_MILITARYAREA", "EXT_MILITARYAREA")
    camItemSpecMap.put("EXT_DISASTER", "EXT_DISASTER")
    camItemSpecMap.put("EXT_CHANGE", "EXT_CHANGE")
    camItemSpecMap.put("EXT_CANCEL", "EXT_CANCEL")
    camItemSpecMap.put("EXT_DESTRUCTION", "EXT_DESTRUCTION")
    camItemSpecMap.put("GT_ARCHSITE", "GT_ARCHSITE")
    camItemSpecMap.put("GT_AUTONOMOUSPART", "GT_AUTONOMOUSPART")
    camItemSpecMap.put("GT_TERRITORIALUNIT", "GT_TERRITORIALUNIT")
    camItemSpecMap.put("GT_MUNIPPART", "GT_MUNIPPART")
    camItemSpecMap.put("GT_DEPARTEMENT", "GT_DEPARTEMENT")
    camItemSpecMap.put("GT_GALAXY", "GT_GALAXY")
    camItemSpecMap.put("GT_MOUNTAIN", "GT_MOUNTAIN")
    camItemSpecMap.put("GT_SHIRE", "GT_SHIRE")
    camItemSpecMap.put("GT_STAR", "GT_STAR")
    camItemSpecMap.put("GT_PROTNATPART", "GT_PROTNATPART")
    camItemSpecMap.put("GT_CAVE", "GT_CAVE")
    camItemSpecMap.put("GT_LAKE", "GT_LAKE")
    camItemSpecMap.put("GT_COSMOSPART", "GT_COSMOSPART")
    camItemSpecMap.put("GT_WATERAREA", "GT_WATERAREA")
    camItemSpecMap.put("GT_NATUREPART", "GT_NATUREPART")
    camItemSpecMap.put("GT_OTHERAREA", "GT_OTHERAREA")
    camItemSpecMap.put("GT_NAMEDFORMATION", "GT_NAMEDFORMATION")
    camItemSpecMap.put("GT_NATFORMATION", "GT_NATFORMATION")
    camItemSpecMap.put("GT_CANAL", "GT_CANAL")
    camItemSpecMap.put("GT_CANTON", "GT_CANTON")
    camItemSpecMap.put("GT_CADASTRALTERRITORY", "GT_CADASTRALTERRITORY")
    camItemSpecMap.put("GT_CONTINENT", "GT_CONTINENT")
    camItemSpecMap.put("GT_HILL", "GT_HILL")
    camItemSpecMap.put("GT_ADMREGION", "GT_ADMREGION")
    camItemSpecMap.put("GT_CRATER", "GT_CRATER")
    camItemSpecMap.put("GT_FOREST", "GT_FOREST")
    camItemSpecMap.put("GT_FORESTPARK", "GT_FORESTPARK")
    camItemSpecMap.put("GT_MOON", "GT_MOON")
    camItemSpecMap.put("GT_CITYDISTRICT", "GT_CITYDISTRICT")
    camItemSpecMap.put("GT_PEAKBOG", "GT_PEAKBOG")
    camItemSpecMap.put("GT_SEA", "GT_SEA")
    camItemSpecMap.put("GT_HEADLAND", "GT_HEADLAND")
    camItemSpecMap.put("GT_WATERFRONT", "GT_WATERFRONT")
    camItemSpecMap.put("GT_SQUARE", "GT_SQUARE")
    camItemSpecMap.put("GT_ETHNOGRAPHICAREA", "GT_ETHNOGRAPHICAREA")
    camItemSpecMap.put("GT_LOWLAND", "GT_LOWLAND")
    camItemSpecMap.put("GT_SETTLEMENT", "GT_SETTLEMENT")
    camItemSpecMap.put("GT_OASIS", "GT_OASIS")
    camItemSpecMap.put("GT_MUNIP", "GT_MUNIP")
    camItemSpecMap.put("GT_MUNIPDISTR", "GT_MUNIPDISTR")
    camItemSpecMap.put("GT_AREA", "GT_AREA")
    camItemSpecMap.put("GT_OCEAN", "GT_OCEAN")
    camItemSpecMap.put("GT_DISTRICT", "GT_DISTRICT")
    camItemSpecMap.put("GT_OKRUH", "GT_OKRUH")
    camItemSpecMap.put("GT_ISLAND", "GT_ISLAND")
    camItemSpecMap.put("GT_HILLYAREA", "GT_HILLYAREA")
    camItemSpecMap.put("GT_MONUMENTZONE", "GT_MONUMENTZONE")
    camItemSpecMap.put("GT_MANOR", "GT_MANOR")
    camItemSpecMap.put("GT_RAPIDS", "GT_RAPIDS")
    camItemSpecMap.put("GT_PLANET", "GT_PLANET")
    camItemSpecMap.put("GT_PLATEAU", "GT_PLATEAU")
    camItemSpecMap.put("GT_MOUNTAINS", "GT_MOUNTAINS")
    camItemSpecMap.put("GT_PENINSULA", "GT_PENINSULA")
    camItemSpecMap.put("GT_BROOK", "GT_BROOK")
    camItemSpecMap.put("GT_DESERT", "GT_DESERT")
    camItemSpecMap.put("GT_CATCHMENTAREA", "GT_CATCHMENTAREA")
    camItemSpecMap.put("GT_VIRGINFOREST", "GT_VIRGINFOREST")
    camItemSpecMap.put("GT_SPRING", "GT_SPRING")
    camItemSpecMap.put("GT_CHASM", "GT_CHASM")
    camItemSpecMap.put("GT_PROVINCE", "GT_PROVINCE")
    camItemSpecMap.put("GT_NAVIGATIONCANAL", "GT_NAVIGATIONCANAL")
    camItemSpecMap.put("GT_PASS", "GT_PASS")
    camItemSpecMap.put("GT_REGION", "GT_REGION")
    camItemSpecMap.put("GT_RECREATIONAREA", "GT_RECREATIONAREA")
    camItemSpecMap.put("GT_POND", "GT_POND")
    camItemSpecMap.put("GT_RIVER", "GT_RIVER")
    camItemSpecMap.put("GT_ROCKCITY", "GT_ROCKCITY")
    camItemSpecMap.put("GT_ROCK", "GT_ROCK")
    camItemSpecMap.put("GT_TREEGROUP", "GT_TREEGROUP")
    camItemSpecMap.put("GT_VOLCANO", "GT_VOLCANO")
    camItemSpecMap.put("GT_COURTDISTRICT", "GT_COURTDISTRICT")
    camItemSpecMap.put("GT_CONSTELLATION", "GT_CONSTELLATION")
    camItemSpecMap.put("GT_ARCHIPELAGO", "GT_ARCHIPELAGO")
    camItemSpecMap.put("GT_RAVINE", "GT_RAVINE")
    camItemSpecMap.put("GT_COUNTRY", "GT_COUNTRY")
    camItemSpecMap.put("GT_TREE", "GT_TREE")
    camItemSpecMap.put("GT_CLIMATICPHEN", "GT_CLIMATICPHEN")
    camItemSpecMap.put("GT_VALLEY", "GT_VALLEY")
    camItemSpecMap.put("GT_STREET", "GT_STREET")
    camItemSpecMap.put("GT_CLIFF", "GT_CLIFF")
    camItemSpecMap.put("GT_SEABOTSHAPE", "GT_SEABOTSHAPE")
    camItemSpecMap.put("GT_DAM", "GT_DAM")
    camItemSpecMap.put("GT_WATERFALL", "GT_WATERFALL")
    camItemSpecMap.put("GT_MILITARYAREA", "GT_MILITARYAREA")
    camItemSpecMap.put("GT_VOJVODSTVI", "GT_VOJVODSTVI")
    camItemSpecMap.put("GT_HIGHLANDS", "GT_HIGHLANDS")
    camItemSpecMap.put("GT_GARDEN", "GT_GARDEN")
    camItemSpecMap.put("GT_BAY", "GT_BAY")
    camItemSpecMap.put("GT_LAND", "GT_LAND")
    camItemSpecMap.put("GT_COUNTY", "GT_COUNTY")
    camItemSpecMap.put("ARCHNUM", "ARCHNUM")
    camItemSpecMap.put("VAT", "VAT")
    camItemSpecMap.put("IC", "IC")
    camItemSpecMap.put("INTERPI", "INTERPI")
    camItemSpecMap.put("ISO3166_2", "ISO3166_2")
    camItemSpecMap.put("ISO3166_3", "ISO3166_3")
    camItemSpecMap.put("ISO3166_NUM", "ISO3166_NUM")
    camItemSpecMap.put("ISO3166_PART2", "ISO3166_PART2")
    camItemSpecMap.put("CZ_RETRO", "CZ_RETRO")
    camItemSpecMap.put("TAXONOMY", "TAXONOMY")
    camItemSpecMap.put("NUTSLAU", "NUTSLAU")
    camItemSpecMap.put("ORCID_ID", "ORCID_ID")
    camItemSpecMap.put("PEVA", "PEVA")
    camItemSpecMap.put("RUIAN", "RUIAN")
    camItemSpecMap.put("AUT", "AUT")
    camItemSpecMap.put("VIAF", "VIAF")
    camItemSpecMap.put("LCNAF", "LCNAF")
    camItemSpecMap.put("LNG_heb", "LNG_heb")
    camItemSpecMap.put("LNG_per", "LNG_per")
    camItemSpecMap.put("LNG_abk", "LNG_abk")
    camItemSpecMap.put("LNG_afr", "LNG_afr")
    camItemSpecMap.put("LNG_akk", "LNG_akk")
    camItemSpecMap.put("LNG_alb", "LNG_alb")
    camItemSpecMap.put("LNG_gsw", "LNG_gsw")
    camItemSpecMap.put("LNG_amh", "LNG_amh")
    camItemSpecMap.put("LNG_eng", "LNG_eng")
    camItemSpecMap.put("LNG_ara", "LNG_ara")
    camItemSpecMap.put("LNG_arc", "LNG_arc")
    camItemSpecMap.put("LNG_arm", "LNG_arm")
    camItemSpecMap.put("LNG_asm", "LNG_asm")
    camItemSpecMap.put("LNG_0as", "LNG_0as")
    camItemSpecMap.put("LNG_aze", "LNG_aze")
    camItemSpecMap.put("LNG_0ba", "LNG_0ba")
    camItemSpecMap.put("LNG_bur", "LNG_bur")
    camItemSpecMap.put("LNG_baq", "LNG_baq")
    camItemSpecMap.put("LNG_bak", "LNG_bak")
    camItemSpecMap.put("LNG_bel", "LNG_bel")
    camItemSpecMap.put("LNG_ben", "LNG_ben")
    camItemSpecMap.put("LNG_dzo", "LNG_dzo")
    camItemSpecMap.put("LNG_bih", "LNG_bih")
    camItemSpecMap.put("LNG_afa", "LNG_afa")
    camItemSpecMap.put("LNG_tut", "LNG_tut")
    camItemSpecMap.put("LNG_aav", "LNG_aav")
    camItemSpecMap.put("LNG_map", "LNG_map")
    camItemSpecMap.put("LNG_bat", "LNG_bat")
    camItemSpecMap.put("LNG_dra", "LNG_dra")
    camItemSpecMap.put("LNG_esx", "LNG_esx")
    camItemSpecMap.put("LNG_gem", "LNG_gem")
    camItemSpecMap.put("LNG_0ni", "LNG_0ni")
    camItemSpecMap.put("LNG_inc", "LNG_inc")
    camItemSpecMap.put("LNG_ine", "LNG_ine")
    camItemSpecMap.put("LNG_iir", "LNG_iir")
    camItemSpecMap.put("LNG_0nz", "LNG_0nz")
    camItemSpecMap.put("LNG_ccs", "LNG_ccs")
    camItemSpecMap.put("LNG_cel", "LNG_cel")
    camItemSpecMap.put("LNG_crs", "LNG_crs")
    camItemSpecMap.put("LNG_xgn", "LNG_xgn")
    camItemSpecMap.put("LNG_mun", "LNG_mun")
    camItemSpecMap.put("LNG_nic", "LNG_nic")
    camItemSpecMap.put("LNG_roa", "LNG_roa")
    camItemSpecMap.put("LNG_sem", "LNG_sem")
    camItemSpecMap.put("LNG_ccn", "LNG_ccn")
    camItemSpecMap.put("LNG_sla", "LNG_sla")
    camItemSpecMap.put("LNG_tai", "LNG_tai")
    camItemSpecMap.put("LNG_sit", "LNG_sit")
    camItemSpecMap.put("LNG_trk", "LNG_trk")
    camItemSpecMap.put("LNG_0uj", "LNG_0uj")
    camItemSpecMap.put("LNG_urj", "LNG_urj")
    camItemSpecMap.put("LNG_brx", "LNG_brx")
    camItemSpecMap.put("LNG_bos", "LNG_bos")
    camItemSpecMap.put("LNG_bre", "LNG_bre")
    camItemSpecMap.put("LNG_bul", "LNG_bul")
    camItemSpecMap.put("LNG_cze", "LNG_cze")
    camItemSpecMap.put("LNG_chi", "LNG_chi")
    camItemSpecMap.put("LNG_dan", "LNG_dan")
    camItemSpecMap.put("LNG_doi", "LNG_doi")
    camItemSpecMap.put("LNG_nds", "LNG_nds")
    camItemSpecMap.put("LNG_egy", "LNG_egy")
    camItemSpecMap.put("LNG_epo", "LNG_epo")
    camItemSpecMap.put("LNG_est", "LNG_est")
    camItemSpecMap.put("LNG_fao", "LNG_fao")
    camItemSpecMap.put("LNG_fin", "LNG_fin")
    camItemSpecMap.put("LNG_fre", "LNG_fre")
    camItemSpecMap.put("LNG_fry", "LNG_fry")
    camItemSpecMap.put("LNG_gag", "LNG_gag")
    camItemSpecMap.put("LNG_glg", "LNG_glg")
    camItemSpecMap.put("LNG_kal", "LNG_kal")
    camItemSpecMap.put("LNG_geo", "LNG_geo")
    camItemSpecMap.put("LNG_guj", "LNG_guj")
    camItemSpecMap.put("LNG_hau", "LNG_hau")
    camItemSpecMap.put("LNG_zkz", "LNG_zkz")
    camItemSpecMap.put("LNG_hin", "LNG_hin")
    camItemSpecMap.put("LNG_dut", "LNG_dut")
    camItemSpecMap.put("LNG_hit", "LNG_hit")
    camItemSpecMap.put("LNG_hrv", "LNG_hrv")
    camItemSpecMap.put("LNG_ido", "LNG_ido")
    camItemSpecMap.put("LNG_ind", "LNG_ind")
    camItemSpecMap.put("LNG_ina", "LNG_ina")
    camItemSpecMap.put("LNG_ile", "LNG_ile")
    camItemSpecMap.put("LNG_gle", "LNG_gle")
    camItemSpecMap.put("LNG_ice", "LNG_ice")
    camItemSpecMap.put("LNG_ita", "LNG_ita")
    camItemSpecMap.put("LNG_jpn", "LNG_jpn")
    camItemSpecMap.put("LNG_yid", "LNG_yid")
    camItemSpecMap.put("LNG_0jd", "LNG_0jd")
    camItemSpecMap.put("LNG_kan", "LNG_kan")
    camItemSpecMap.put("LNG_kas", "LNG_kas")
    camItemSpecMap.put("LNG_csb", "LNG_csb")
    camItemSpecMap.put("LNG_cat", "LNG_cat")
    camItemSpecMap.put("LNG_kaz", "LNG_kaz")
    camItemSpecMap.put("LNG_khm", "LNG_khm")
    camItemSpecMap.put("LNG_kok", "LNG_kok")
    camItemSpecMap.put("LNG_cop", "LNG_cop")
    camItemSpecMap.put("LNG_kor", "LNG_kor")
    camItemSpecMap.put("LNG_cos", "LNG_cos")
    camItemSpecMap.put("LNG_crh", "LNG_crh")
    camItemSpecMap.put("LNG_kur", "LNG_kur")
    camItemSpecMap.put("LNG_kir", "LNG_kir")
    camItemSpecMap.put("LNG_lao", "LNG_lao")
    camItemSpecMap.put("LNG_lat", "LNG_lat")
    camItemSpecMap.put("LNG_lit", "LNG_lit")
    camItemSpecMap.put("LNG_liv", "LNG_liv")
    camItemSpecMap.put("LNG_lav", "LNG_lav")
    camItemSpecMap.put("LNG_ltz", "LNG_ltz")
    camItemSpecMap.put("LNG_dsb", "LNG_dsb")
    camItemSpecMap.put("LNG_hsb", "LNG_hsb")
    camItemSpecMap.put("LNG_hun", "LNG_hun")
    camItemSpecMap.put("LNG_mac", "LNG_mac")
    camItemSpecMap.put("LNG_mal", "LNG_mal")
    camItemSpecMap.put("LNG_may", "LNG_may")
    camItemSpecMap.put("LNG_mlt", "LNG_mlt")
    camItemSpecMap.put("LNG_mni", "LNG_mni")
    camItemSpecMap.put("LNG_mar", "LNG_mar")
    camItemSpecMap.put("LNG_mol", "LNG_mol")
    camItemSpecMap.put("LNG_mon", "LNG_mon")
    camItemSpecMap.put("LNG_ger", "LNG_ger")
    camItemSpecMap.put("LNG_nor", "LNG_nor")
    camItemSpecMap.put("LNG_oci", "LNG_oci")
    camItemSpecMap.put("LNG_nep", "LNG_nep")
    camItemSpecMap.put("LNG_pan", "LNG_pan")
    camItemSpecMap.put("LNG_pus", "LNG_pus")
    camItemSpecMap.put("LNG_pol", "LNG_pol")
    camItemSpecMap.put("LNG_por", "LNG_por")
    camItemSpecMap.put("LNG_roh", "LNG_roh")
    camItemSpecMap.put("LNG_rom", "LNG_rom")
    camItemSpecMap.put("LNG_rum", "LNG_rum")
    camItemSpecMap.put("LNG_0ru", "LNG_0ru")
    camItemSpecMap.put("LNG_rus", "LNG_rus")
    camItemSpecMap.put("LNG_gre", "LNG_gre")
    camItemSpecMap.put("LNG_san", "LNG_san")
    camItemSpecMap.put("LNG_sat", "LNG_sat")
    camItemSpecMap.put("LNG_snd", "LNG_snd")
    camItemSpecMap.put("LNG_sin", "LNG_sin")
    camItemSpecMap.put("LNG_gla", "LNG_gla")
    camItemSpecMap.put("LNG_slo", "LNG_slo")
    camItemSpecMap.put("LNG_slv", "LNG_slv")
    camItemSpecMap.put("LNG_0sl", "LNG_0sl")
    camItemSpecMap.put("LNG_0sc", "LNG_0sc")
    camItemSpecMap.put("LNG_srp", "LNG_srp")
    camItemSpecMap.put("LNG_hbo", "LNG_hbo")
    camItemSpecMap.put("LNG_peo", "LNG_peo")
    camItemSpecMap.put("LNG_chu", "LNG_chu")
    camItemSpecMap.put("LNG_pal", "LNG_pal")
    camItemSpecMap.put("LNG_sux", "LNG_sux")
    camItemSpecMap.put("LNG_swa", "LNG_swa")
    camItemSpecMap.put("LNG_spa", "LNG_spa")
    camItemSpecMap.put("LNG_swe", "LNG_swe")
    camItemSpecMap.put("LNG_tgk", "LNG_tgk")
    camItemSpecMap.put("LNG_tam", "LNG_tam")
    camItemSpecMap.put("LNG_tat", "LNG_tat")
    camItemSpecMap.put("LNG_tel", "LNG_tel")
    camItemSpecMap.put("LNG_tha", "LNG_tha")
    camItemSpecMap.put("LNG_tib", "LNG_tib")
    camItemSpecMap.put("LNG_tur", "LNG_tur")
    camItemSpecMap.put("LNG_tuk", "LNG_tuk")
    camItemSpecMap.put("LNG_uig", "LNG_uig")
    camItemSpecMap.put("LNG_ukr", "LNG_ukr")
    camItemSpecMap.put("LNG_urd", "LNG_urd")
    camItemSpecMap.put("LNG_ori", "LNG_ori")
    camItemSpecMap.put("LNG_uzb", "LNG_uzb")
    camItemSpecMap.put("LNG_wel", "LNG_wel")
    camItemSpecMap.put("LNG_vie", "LNG_vie")
    camItemSpecMap.put("LNG_vol", "LNG_vol")
    camItemSpecMap.put("NT_AUTHORCIPHER", "NT_AUTHORCIPHER")
    camItemSpecMap.put("NT_RELIGIOUS", "NT_RELIGIOUS")
    camItemSpecMap.put("NT_EQUIV", "NT_EQUIV")
    camItemSpecMap.put("NT_HISTORICAL", "NT_HISTORICAL")
    camItemSpecMap.put("NT_FORMER", "NT_FORMER")
    camItemSpecMap.put("NT_HOMONYMUM", "NT_HOMONYMUM")
    camItemSpecMap.put("NT_INCIPIT", "NT_INCIPIT")
    camItemSpecMap.put("NT_INVERTED", "NT_INVERTED")
    camItemSpecMap.put("NT_ONLYKNOWN", "NT_ONLYKNOWN")
    camItemSpecMap.put("NT_ORIGINAL", "NT_ORIGINAL")
    camItemSpecMap.put("NT_INAPPROPRIATE", "NT_INAPPROPRIATE")
    camItemSpecMap.put("NT_TERM", "NT_TERM")
    camItemSpecMap.put("NT_PLURAL", "NT_PLURAL")
    camItemSpecMap.put("NT_OTHERRULES", "NT_OTHERRULES")
    camItemSpecMap.put("NT_HONOR", "NT_HONOR")
    camItemSpecMap.put("NT_TAKEN", "NT_TAKEN")
    camItemSpecMap.put("NT_TRANSLATED", "NT_TRANSLATED")
    camItemSpecMap.put("NT_ALIAS", "NT_ALIAS")
    camItemSpecMap.put("NT_ACCEPTED", "NT_ACCEPTED")
    camItemSpecMap.put("NT_DIRECT", "NT_DIRECT")
    camItemSpecMap.put("NT_PSEUDONYM", "NT_PSEUDONYM")
    camItemSpecMap.put("NT_NATIV", "NT_NATIV")
    camItemSpecMap.put("NT_SINGULAR", "NT_SINGULAR")
    camItemSpecMap.put("NT_ACTUAL", "NT_ACTUAL")
    camItemSpecMap.put("NT_SECULAR", "NT_SECULAR")
    camItemSpecMap.put("NT_ARTIFICIAL", "NT_ARTIFICIAL")
    camItemSpecMap.put("NT_OFFICIAL", "NT_OFFICIAL")
    camItemSpecMap.put("NT_NARROWER", "NT_NARROWER")
    camItemSpecMap.put("NT_SIMPLIFIED", "NT_SIMPLIFIED")
    camItemSpecMap.put("NT_GARBLED", "NT_GARBLED")
    camItemSpecMap.put("NT_ACRONYM", "NT_ACRONYM")
    camItemSpecMap.put("NT_FOLK", "NT_FOLK")
    camItemSpecMap.put("RT_AUTHOR", "RT_AUTHOR")
    camItemSpecMap.put("RT_AUTHOROFCHANGE", "RT_AUTHOROFCHANGE")
    camItemSpecMap.put("RT_GRANDMOTHER", "RT_GRANDMOTHER")
    camItemSpecMap.put("RT_BROTHER", "RT_BROTHER")
    camItemSpecMap.put("RT_HECOUSIN", "RT_HECOUSIN")
    camItemSpecMap.put("RT_WHOLE", "RT_WHOLE")
    camItemSpecMap.put("RT_CEREMONY", "RT_CEREMONY")
    camItemSpecMap.put("RT_MEMBERORG", "RT_MEMBERORG")
    camItemSpecMap.put("RT_RELATIONS", "RT_RELATIONS")
    camItemSpecMap.put("RT_GRANDFATHER", "RT_GRANDFATHER")
    camItemSpecMap.put("RT_DOCUMENT", "RT_DOCUMENT")
    camItemSpecMap.put("RT_ENTITYEND", "RT_ENTITYEND")
    camItemSpecMap.put("RT_ENTITYBIRTH", "RT_ENTITYBIRTH")
    camItemSpecMap.put("RT_ENTITYDEATH", "RT_ENTITYDEATH")
    camItemSpecMap.put("RT_ENTINTYRISE", "RT_ENTINTYRISE")
    camItemSpecMap.put("RT_ENTITYORIGIN", "RT_ENTITYORIGIN")
    camItemSpecMap.put("RT_ENTITYEXTINCTION", "RT_ENTITYEXTINCTION")
    camItemSpecMap.put("RT_FUNCTION", "RT_FUNCTION")
    camItemSpecMap.put("RT_GEOSCOPE", "RT_GEOSCOPE")
    camItemSpecMap.put("RT_MILESTONE", "RT_MILESTONE")
    camItemSpecMap.put("RT_ISPART", "RT_ISPART")
    camItemSpecMap.put("RT_ISMEMBER", "RT_ISMEMBER")
    camItemSpecMap.put("RT_GENUSMEMBER", "RT_GENUSMEMBER")
    camItemSpecMap.put("RT_OTHERNAME", "RT_OTHERNAME")
    camItemSpecMap.put("RT_ANCESTOR", "RT_ANCESTOR")
    camItemSpecMap.put("RT_ACTIVITYCORP", "RT_ACTIVITYCORP")
    camItemSpecMap.put("RT_LIQUIDATOR", "RT_LIQUIDATOR")
    camItemSpecMap.put("RT_OWNER", "RT_OWNER")
    camItemSpecMap.put("RT_HOLDER", "RT_HOLDER")
    camItemSpecMap.put("RT_HUSBAND", "RT_HUSBAND")
    camItemSpecMap.put("RT_WIFE", "RT_WIFE")
    camItemSpecMap.put("RT_MOTHER", "RT_MOTHER")
    camItemSpecMap.put("RT_PLACE", "RT_PLACE")
    camItemSpecMap.put("RT_VENUE", "RT_VENUE")
    camItemSpecMap.put("RT_PLACEEND", "RT_PLACEEND")
    camItemSpecMap.put("RT_PLACESTART", "RT_PLACESTART")
    camItemSpecMap.put("RT_SUPCORP", "RT_SUPCORP")
    camItemSpecMap.put("RT_SUPTERM", "RT_SUPTERM")
    camItemSpecMap.put("RT_SENIOR", "RT_SENIOR")
    camItemSpecMap.put("RT_SUCCESSOR", "RT_SUCCESSOR")
    camItemSpecMap.put("RT_ACTIVITYFIELD", "RT_ACTIVITYFIELD")
    camItemSpecMap.put("RT_STUDYFIELD", "RT_STUDYFIELD")
    camItemSpecMap.put("RT_AWARD", "RT_AWARD")
    camItemSpecMap.put("RT_ORGANIZER", "RT_ORGANIZER")
    camItemSpecMap.put("RT_FATHER", "RT_FATHER")
    camItemSpecMap.put("RT_PARTNER", "RT_PARTNER")
    camItemSpecMap.put("RT_SHEPARTNER", "RT_SHEPARTNER")
    camItemSpecMap.put("RT_SUBTERM", "RT_SUBTERM")
    camItemSpecMap.put("RT_NAMEDAFTER", "RT_NAMEDAFTER")
    camItemSpecMap.put("RT_LASTKMEMBER", "RT_LASTKMEMBER")
    camItemSpecMap.put("RT_FIRSTKMEMBER", "RT_FIRSTKMEMBER")
    camItemSpecMap.put("RT_PREDECESSOR", "RT_PREDECESSOR")
    camItemSpecMap.put("RT_AFFILIATION", "RT_AFFILIATION")
    camItemSpecMap.put("RT_SISTER", "RT_SISTER")
    camItemSpecMap.put("RT_SHECOUSIN", "RT_SHECOUSIN")
    camItemSpecMap.put("RT_RESIDENCE", "RT_RESIDENCE")
    camItemSpecMap.put("RT_COMPEVENT", "RT_COMPEVENT")
    camItemSpecMap.put("RT_RELATED", "RT_RELATED")
    camItemSpecMap.put("RT_RELATEDTERM", "RT_RELATEDTERM")
    camItemSpecMap.put("RT_COLLABORATOR", "RT_COLLABORATOR")
    camItemSpecMap.put("RT_SCHOOLMATE", "RT_SCHOOLMATE")
    camItemSpecMap.put("RT_UNCLE", "RT_UNCLE")
    camItemSpecMap.put("RT_SCHOOL", "RT_SCHOOL")
    camItemSpecMap.put("RT_BROTHERINLAW", "RT_BROTHERINLAW")
    camItemSpecMap.put("RT_SISTERINLAW", "RT_SISTERINLAW")
    camItemSpecMap.put("RT_CATEGORY", "RT_CATEGORY")
    camItemSpecMap.put("RT_THEME", "RT_THEME")
    camItemSpecMap.put("RT_AUNT", "RT_AUNT")
    camItemSpecMap.put("RT_FATHERINLAW", "RT_FATHERINLAW")
    camItemSpecMap.put("RT_MOTHERINLAW", "RT_MOTHERINLAW")
    camItemSpecMap.put("RT_TEACHER", "RT_TEACHER")
    camItemSpecMap.put("RT_GRANTAUTH", "RT_GRANTAUTH")
    camItemSpecMap.put("RT_STORAGE", "RT_STORAGE")
    camItemSpecMap.put("RT_LOCATION", "RT_LOCATION")
    camItemSpecMap.put("RT_OBJECT", "RT_OBJECT")
    camItemSpecMap.put("RT_ACTIVITIES", "RT_ACTIVITIES")
    camItemSpecMap.put("RT_FOUNDER", "RT_FOUNDER")
    camItemSpecMap.put("RT_EMPLOYER", "RT_EMPLOYER")
    camItemSpecMap.put("RT_ROOF", "RT_ROOF")
    camItemSpecMap.put("RT_REPRESENT", "RT_REPRESENT")
    camItemSpecMap.put("RT_NAMECHANGE", "RT_NAMECHANGE")
    camItemSpecMap.put("RT_MENTION", "RT_MENTION")
    camItemSpecMap.put("RT_ORIGINATOR", "RT_ORIGINATOR")
    camItemSpecMap.put("RT_GEOPARTNER", "RT_GEOPARTNER")

    return camItemSpecMap
}
