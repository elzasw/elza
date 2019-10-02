package cz.tacr.elza.controller.vo;

/**
 * Souhrn fyzických osob spojených příbuzenskou vazbou.
 *
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 23.12.2015
 */
public class ParDynastyVO extends ParPartyVO {

    private String genealogy;

    public String getGenealogy() {
        return genealogy;
    }

    public void setGenealogy(final String genealogy) {
        this.genealogy = genealogy;
    }
}
