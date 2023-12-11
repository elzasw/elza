package cz.tacr.elza.controller.vo;

import jakarta.annotation.Nullable;

/**
 * Třída pro založení přístupového bodu.
 *
 * @since 11.07.2018
 */
public class ApAccessPointCreateVO {

    /**
     * Identifikátor typu AP.
     */
    private Integer typeId;

    /**
     * Identifikátor třídy.
     */
    private Integer scopeId;

    /**
     * Identifikátor přístupového bodu
     */
    @Nullable
    private Integer accessPointId;

    /**
     * Formulář části
     */
    private ApPartFormVO partForm;

    public Integer getTypeId() {
        return typeId;
    }

    public void setTypeId(final Integer typeId) {
        this.typeId = typeId;
    }

    public Integer getScopeId() {
        return scopeId;
    }

    public void setScopeId(final Integer scopeId) {
        this.scopeId = scopeId;
    }

    public Integer getAccessPointId() {
        return accessPointId;
    }

    public void setAccessPointId(Integer accessPointId) {
        this.accessPointId = accessPointId;
    }

    public ApPartFormVO getPartForm() {
        return partForm;
    }

    public void setPartForm(ApPartFormVO partForm) {
        this.partForm = partForm;
    }
}
