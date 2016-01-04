package cz.tacr.elza.controller.vo;

import java.util.ArrayList;
import java.util.List;

import cz.tacr.elza.domain.ParParty;


/**
 * Seznam nalezených osob s celkovým počtem osob.
 *
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 04.01.2016
 */
public class ParPartyWithCount {

    /**
     * Seznam osob.
     */
    private List<ParPartyVO> partyList = new ArrayList<>();

    /**
     * Celkový počet dle užitého filtru. Nikoliv aktuální vracený.
     */
    private Long count;


    /**
     * Default pro JSON operace.
     */
    public ParPartyWithCount() {
    }

    /**
     * Konstruktor pro snažší použití.
     *
     * @param partyList list osob
     * @param count     počet celkem za minulý dotaz
     */
    public ParPartyWithCount(final List<ParPartyVO> partyList, final Long count) {
        this.partyList = partyList;
        this.count = count;
    }

    /**
     * List osob.
     *
     * @param partyList list osob
     */
    public void setRecordList(final List<ParPartyVO> partyList) {
        this.partyList = partyList;
    }

    /**
     * List osob.
     *
     * @return list osob
     */
    public List<ParPartyVO> getRecordList() {
        return partyList;
    }

    /**
     * Celkový počet za použitý výraz (be zohledu na ořez aktuálního vraceného listu).
     *
     * @return celkový počet za použitý výraz (be zohledu na ořez aktuálního vraceného listu)
     */
    public Long getCount() {
        return count;
    }
}
