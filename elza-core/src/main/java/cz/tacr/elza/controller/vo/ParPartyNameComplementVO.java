package cz.tacr.elza.controller.vo;


/**
 * Doplňky jmen osob.
 *
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 23.12.2015
 */
public class ParPartyNameComplementVO {

    private Integer id;

    private Integer complementTypeId;

    private String complement;

    public Integer getId() {
        return id;
    }

    public void setId(final Integer id) {
        this.id = id;
    }

    public Integer getComplementTypeId() {
        return complementTypeId;
    }

    public void setComplementTypeId(final Integer complementTypeId) {
        this.complementTypeId = complementTypeId;
    }

    public String getComplement() {
        return complement;
    }

    public void setComplement(final String complement) {
        this.complement = complement;
    }
}
