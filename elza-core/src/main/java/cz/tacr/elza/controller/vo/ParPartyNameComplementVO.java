package cz.tacr.elza.controller.vo;


/**
 * Doplňky jmen osob.
 *
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 23.12.2015
 */
public class ParPartyNameComplementVO {

    private Integer partyNameComplementId;

    private ParComplementTypeVO complementType;

    private String complement;

    public Integer getPartyNameComplementId() {
        return partyNameComplementId;
    }

    public void setPartyNameComplementId(final Integer partyNameComplementId) {
        this.partyNameComplementId = partyNameComplementId;
    }

    public ParComplementTypeVO getComplementType() {
        return complementType;
    }

    public void setComplementType(final ParComplementTypeVO complementType) {
        this.complementType = complementType;
    }

    public String getComplement() {
        return complement;
    }

    public void setComplement(final String complement) {
        this.complement = complement;
    }
}
