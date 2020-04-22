package cz.tacr.elza.dataexchange.output.writer.cam;

import java.util.HashSet;
import java.util.Set;

public enum CamItemType {

    NM_MAIN(CamDataType.TEXT_250),
    NM_MINOR(CamDataType.TEXT_250),
    NM_DEGREE_PRE(CamDataType.TEXT_50),
    NM_DEGREE_POST(CamDataType.TEXT_50),
    NM_TYPE(
            CamDataType.ENUM,
            true),
    NM_ORDER(CamDataType.NUMBER),
    NM_SUP_GEN(CamDataType.TEXT_50),
    NM_SUP_CHRO(CamDataType.TEXT_50),
    NM_SUP_AUTH(CamDataType.TEXT_250),
    NM_SUP_GEO(CamDataType.TEXT_250),
    NM_USED_FROM(CamDataType.UNITDATE),
    NM_USED_TO(CamDataType.UNITDATE),
    NM_LANG(CamDataType.ENUM, true);

    static final String nmTypes[] = {
            "NT_EQUIV",
            "NT_OFFICIAL",
            "NT_ACRONYM",
            "NT_FORMER",
            "NT_ONLYKNOWN",
            "NT_ARTIFICIAL",
            "NT_TRANSLATED",
            "NT_PSEUDONYM",
            "NT_RELIGIOUS",
            "NT_AUTHORCIPHER",
            "NT_DIRECT",
            "NT_SINGULAR",
            "NT_PLURAL,TERM",
            "NT_TAKEN,TERM",
            "NT_INAPPROPRIATE",
            "NT_NARROWER",
            "NT_TERM,TERM",
            "NT_INVERTED,TERM",
            "NT_ANTONYMUM",
            "NT_HOMONYMUM,TERM",
            "NT_ALIAS",
            "NT_SIMPLIFIED",
            "NT_GARBLED",
            "NT_HONOR",
            "NT_OTHERRULES",
            "NT_HISTORICAL",
            "NT_NATIV",
            "NT_ACCEPTED,PERSON_INDIVIDUAL",
            "NT_SECULAR",
            "NT_ACTUAL,DYNASTY",
            "NT_FOLK,TERM",
            "NT_ORIGINAL,ARTWORK"
    };

    static final String langs[] = {
            "LNG_heb",
            "LNG_akk",
            "LNG_amh",
            "LNG_ara",
            "LNG_arc",
            "LNG_0as",
            "LNG_0ba",
            "LNG_sem",
            "LNG_hau",
            "LNG_hit",
            "LNG_mlt",
            "LNG_hbo",
            "LNG_sux",
            "LNG_afa",
            "LNG_egy",
            "LNG_cop",
            "LNG_xgn",
            "LNG_mon",
            "LNG_aze",
            "LNG_bak",
            "LNG_trk",
            "LNG_gag",
            "LNG_zkz",
            "LNG_crh",
            "LNG_kir",
            "LNG_tat",
            "LNG_tur",
            "LNG_tuk",
            "LNG_uig",
            "LNG_uzb",
            "LNG_tut",
            "LNG_jpn",
            "LNG_kor",
            "LNG_mun",
            "LNG_sat",
            "LNG_aav",
            "LNG_khm",
            "LNG_vie",
            "LNG_map",
            "LNG_ind",
            "LNG_may",
            "LNG_dra",
            "LNG_kan",
            "LNG_mal",
            "LNG_tam",
            "LNG_tel",
            "LNG_esx",
            "LNG_kal",
            "LNG_0ni",
            "LNG_bat",
            "LNG_lit",
            "LNG_lav",
            "LNG_afr",
            "LNG_gsw",
            "LNG_eng",
            "LNG_gem",
            "LNG_dan",
            "LNG_nds",
            "LNG_fao",
            "LNG_fry",
            "LNG_dut",
            "LNG_ice",
            "LNG_yid",
            "LNG_0jd",
            "LNG_ltz",
            "LNG_ger",
            "LNG_nor",
            "LNG_swe",
            "LNG_asm",
            "LNG_ben",
            "LNG_bih",
            "LNG_inc",
            "LNG_doi",
            "LNG_guj",
            "LNG_hin",
            "LNG_kas",
            "LNG_mar",
            "LNG_nep",
            "LNG_pan",
            "LNG_rom",
            "LNG_san",
            "LNG_snd",
            "LNG_sin",
            "LNG_urd",
            "LNG_ori",
            "LNG_per",
            "LNG_iir",
            "LNG_kok",
            "LNG_kur",
            "LNG_pus",
            "LNG_peo",
            "LNG_pal",
            "LNG_tgk",
            "LNG_cel",
            "LNG_bre",
            "LNG_gle",
            "LNG_gla",
            "LNG_wel",
            "LNG_alb",
            "LNG_arm",
            "LNG_gre",
            "LNG_roa",
            "LNG_fre",
            "LNG_glg",
            "LNG_ita",
            "LNG_cat",
            "LNG_cos",
            "LNG_lat",
            "LNG_mol",
            "LNG_oci",
            "LNG_por",
            "LNG_roh",
            "LNG_rum",
            "LNG_spa",
            "LNG_bel",
            "LNG_sla",
            "LNG_bos",
            "LNG_bul",
            "LNG_cze",
            "LNG_hrv",
            "LNG_csb",
            "LNG_dsb",
            "LNG_hsb",
            "LNG_mac",
            "LNG_pol",
            "LNG_0ru",
            "LNG_rus",
            "LNG_slo",
            "LNG_slv",
            "LNG_0sc",
            "LNG_srp",
            "LNG_chu",
            "LNG_ukr",
            "LNG_ine",
            "LNG_baq",
            "LNG_0nz",
            "LNG_ccs",
            "LNG_geo",
            "LNG_crs",
            "LNG_nic",
            "LNG_swa",
            "LNG_abk",
            "LNG_ccn",
            "LNG_tai",
            "LNG_lao",
            "LNG_tha",
            "LNG_bur",
            "LNG_dzo",
            "LNG_sit",
            "LNG_brx",
            "LNG_chi",
            "LNG_mni",
            "LNG_tib",
            "LNG_0uj",
            "LNG_epo",
            "LNG_ido",
            "LNG_ina",
            "LNG_ile",
            "LNG_0sl",
            "LNG_vol",
            "LNG_urj",
            "LNG_est",
            "LNG_fin",
            "LNG_liv",
            "LNG_hun"
    };

    static {
        NM_TYPE.setSpecs(nmTypes);
        NM_LANG.setSpecs(langs);
    };

    private final CamDataType dataType;

    private final boolean useSpecification;

    private Set<String> specs;

    private CamItemType(final CamDataType dataType) {
        this.dataType = dataType;
        this.useSpecification = false;
    }

    void setSpecs(String[] specs) {
        for (String param : specs) {
            this.specs.add(param);
        }
    }

    private CamItemType(final CamDataType dataType, final boolean useSpec) {
        this.dataType = dataType;
        this.useSpecification = useSpec;
        if (useSpec) {
            specs = new HashSet<>();
        }
    }
    
    public CamDataType getDataType() {
        return dataType;
    }

    public boolean isValidSpec(String specType) {
        if (!useSpecification) {
            // only null is allowed if without spec
            return specType == null;
        } else {
            return specs.contains(specType);
        }
    }

    public boolean isUseSpecification() {
        return useSpecification;
    }
}
