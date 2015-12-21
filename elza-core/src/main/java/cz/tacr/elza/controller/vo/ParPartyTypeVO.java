package cz.tacr.elza.controller.vo;

import cz.tacr.elza.domain.ParPartyType;


/**
 * VO objekt pro {@link ParPartyType}.
 *
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 21.12.2015
 */
public class ParPartyTypeVO {

    /**
     * Id.
     */
    private Integer partyTypeId;

    /**
     * Kod typu osoby.
     */
    private String code;
    /**
     * Název typu osoby.
     */
    private String name;

    /**
     * Popis typu osoby.
     */
    private String description;

    public Integer getPartyTypeId() {
        return partyTypeId;
    }

    public void setPartyTypeId(final Integer partyTypeId) {
        this.partyTypeId = partyTypeId;
    }

    public String getCode() {
        return code;
    }

    public void setCode(final String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(final String description) {
        this.description = description;
    }
}
