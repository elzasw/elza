package cz.tacr.elza.controller.vo.descitems;


import cz.tacr.elza.controller.vo.ParPartyVO;


/**
 * VO hodnoty atributu - party.
 *
 * @author Martin Šlapa
 * @since 8.1.2016
 */
public class ArrDescItemPartyRefVO extends ArrDescItemVO {

    /**
     * osoba
     */
    private ParPartyVO party;

    public ParPartyVO getParty() {
        return party;
    }

    public void setParty(final ParPartyVO party) {
        this.party = party;
    }
}