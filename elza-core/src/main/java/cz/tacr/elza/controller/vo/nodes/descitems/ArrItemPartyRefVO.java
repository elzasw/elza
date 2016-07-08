package cz.tacr.elza.controller.vo.nodes.descitems;


import cz.tacr.elza.controller.vo.ParPartyVO;


/**
 * VO hodnoty atributu - party.
 *
 * @author Martin Šlapa
 * @since 8.1.2016
 */
public class ArrItemPartyRefVO extends ArrItemVO {

    /**
     * osoba
     */
    private ParPartyVO party;

    private Integer value;

    public ParPartyVO getParty() {
        return party;
    }

    public void setParty(final ParPartyVO party) {
        this.party = party;
    }

    public Integer getValue() {
        return value;
    }

    public void setValue(final Integer value) {
        this.value = value;
    }
}