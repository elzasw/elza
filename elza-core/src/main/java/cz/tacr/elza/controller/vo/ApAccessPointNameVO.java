package cz.tacr.elza.controller.vo;

import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.domain.ApName;
import cz.tacr.elza.domain.SysLanguage;

/**
 * Jméno přístupového bodu.
 *
 * @since 11.07.2018
 */
public class ApAccessPointNameVO {

    /**
     * Identifikátor záznamu
     */
    private Integer id;

    /**
     * Id rejstříkového hesla.
     */
    private Integer accessPointId;

    /**
     * Název.
     */
    private String name;

    /**
     * Doplněk.
     */
    private String complement;

    /**
     * Celé jméno.
     */
    private String fullName;

    /**
     * Jedná se o preferované jméno?
     */
    private Boolean preferredName;

    /**
     * Kód jazyku jména.
     */
    private String languageCode;

    public Integer getId() {
        return id;
    }

    public void setId(final Integer id) {
        this.id = id;
    }

    public Integer getAccessPointId() {
        return accessPointId;
    }

    public void setAccessPointId(final Integer accessPointId) {
        this.accessPointId = accessPointId;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public String getComplement() {
        return complement;
    }

    public void setComplement(final String complement) {
        this.complement = complement;
    }

    public Boolean getPreferredName() {
        return preferredName;
    }

    public void setPreferredName(final Boolean preferredName) {
        this.preferredName = preferredName;
    }

    public String getLanguageCode() {
        return languageCode;
    }

    public void setLanguageCode(final String languageCode) {
        this.languageCode = languageCode;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(final String fullName) {
        this.fullName = fullName;
    }
    
    /**
     * Creates value object from AP name.
     */
    public static ApAccessPointNameVO newInstance(ApName src, StaticDataProvider staticData) {
        ApAccessPointNameVO vo = new ApAccessPointNameVO();
        vo.setAccessPointId(src.getAccessPointId());
        vo.setComplement(src.getComplement());
        vo.setFullName(src.getFullName());
        vo.setId(src.getNameId());
        vo.setName(src.getName());
        vo.setPreferredName(src.isPreferredName());
        if (src.getLanguageId() != null) {
            SysLanguage lang = staticData.getSysLanguageById(src.getLanguageId());
            vo.setLanguageCode(lang.getCode());
        }
        return vo;
    }
}
