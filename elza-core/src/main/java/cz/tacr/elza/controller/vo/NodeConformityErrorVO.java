package cz.tacr.elza.controller.vo;

/**
 * VO pro chyby z validace.
 *
 * @author Martin Šlapa
 * @since 26.1.2016
 */
public class NodeConformityErrorVO {

    /**
     * Identifikátor hodnoty atributu.
     */
    private Integer descItemObjectId;

    /**
     * Popis chyby.
     */
    private String description;

    public Integer getDescItemObjectId() {
        return descItemObjectId;
    }

    public void setDescItemObjectId(final Integer descItemObjectId) {
        this.descItemObjectId = descItemObjectId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(final String description) {
        this.description = description;
    }
}
