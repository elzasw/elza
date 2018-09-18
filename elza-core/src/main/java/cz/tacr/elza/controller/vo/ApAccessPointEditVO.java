package cz.tacr.elza.controller.vo;

/**
 * Třída pro editaci přístupového bodu.
 *
 * @since 11.07.2018
 */
public class ApAccessPointEditVO {

    /**
     * Identifikátor typu AP.
     */
    private Integer typeId;

    public Integer getTypeId() {
        return typeId;
    }

    public void setTypeId(final Integer typeId) {
        this.typeId = typeId;
    }
}
