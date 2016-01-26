package cz.tacr.elza.controller.vo;

/**
 * VO pro chybějící položky z validace.
 *
 * @author Martin Šlapa
 * @since 26.1.2016
 */
public class NodeConformityMissingVO {

    /**
     * Identifikátor typu.
     */
    private Integer descItemTypeId;

    /**
     * Identifikátor specifikace.
     */
    private Integer descItemSpecId;

    /**
     * Popis chybějící položky.
     */
    private String description;

    public Integer getDescItemTypeId() {
        return descItemTypeId;
    }

    public void setDescItemTypeId(final Integer descItemTypeId) {
        this.descItemTypeId = descItemTypeId;
    }

    public Integer getDescItemSpecId() {
        return descItemSpecId;
    }

    public void setDescItemSpecId(final Integer descItemSpecId) {
        this.descItemSpecId = descItemSpecId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(final String description) {
        this.description = description;
    }
}
