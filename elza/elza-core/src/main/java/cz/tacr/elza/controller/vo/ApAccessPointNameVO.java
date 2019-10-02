package cz.tacr.elza.controller.vo;

import cz.tacr.elza.controller.vo.ap.ApStateVO;
import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.domain.ApName;
import cz.tacr.elza.domain.SysLanguage;

import cz.tacr.elza.controller.vo.ap.ApFormVO;

import javax.annotation.Nullable;

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
     * Identifikátor jmena (nemění se při odverování)
     */
    private Integer objectId;

    /**
     * Název.
     */
    @Nullable
    private String name;

    /**
     * Doplněk.
     */
    @Nullable
    private String complement;

    /**
     * Celé jméno.
     */
    @Nullable
    private String fullName;

    /**
     * Jedná se o preferované jméno?
     */
    private Boolean preferredName;

    /**
     * Kód jazyku jména.
     */
    @Nullable
    private String languageCode;

    /**
     * Stav jména.
     */
    @Nullable
    private ApStateVO state;

    /**
     * Chyby ve jméně.
     */
    @Nullable
    private String errorDescription;

    /**
     * Strukturované data formuláře pro jméno. Vyplněné pouze v případě, že se jedná o strukturovaný typ a že se jedná o editační detail.
     */
    @Nullable
    private ApFormVO form;

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

    public Integer getObjectId() {
        return objectId;
    }

    public void setObjectId(final Integer objectId) {
        this.objectId = objectId;
    }

    @Nullable
    public String getName() {
        return name;
    }

    public void setName(@Nullable final String name) {
        this.name = name;
    }

    @Nullable
    public String getComplement() {
        return complement;
    }

    public void setComplement(@Nullable final String complement) {
        this.complement = complement;
    }

    public Boolean getPreferredName() {
        return preferredName;
    }

    public void setPreferredName(final Boolean preferredName) {
        this.preferredName = preferredName;
    }

    @Nullable
    public String getLanguageCode() {
        return languageCode;
    }

    public void setLanguageCode(@Nullable final String languageCode) {
        this.languageCode = languageCode;
    }

    @Nullable
    public String getFullName() {
        return fullName;
    }

    public void setFullName(@Nullable final String fullName) {
        this.fullName = fullName;
    }

    @Nullable
    public ApStateVO getState() {
        return state;
    }

    public void setState(@Nullable final ApStateVO state) {
        this.state = state;
    }

    @Nullable
    public String getErrorDescription() {
        return errorDescription;
    }

    public void setErrorDescription(@Nullable final String errorDescription) {
        this.errorDescription = errorDescription;
    }

    @Nullable
    public ApFormVO getForm() {
        return form;
    }

    public void setForm(@Nullable final ApFormVO form) {
        this.form = form;
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
        vo.setObjectId(src.getObjectId());
        vo.setName(src.getName());
        vo.setPreferredName(src.isPreferredName());
        vo.setState(src.getState() == null ? null : ApStateVO.valueOf(src.getState().name()));
        vo.setErrorDescription(src.getErrorDescription());
        if (src.getLanguageId() != null) {
            SysLanguage lang = staticData.getSysLanguageById(src.getLanguageId());
            vo.setLanguageCode(lang.getCode());
        }
        return vo;
    }
}
