package cz.tacr.elza.domain.vo;

import cz.tacr.elza.domain.ParParty;

import java.util.ArrayList;
import java.util.List;

/**
 * Zapouzdření kolekce vracených osob a celkového počtu za danou vyhledávací podmínku.
 *
 * @author <a href="mailto:martin.kuzel@marbes.cz">Martin Kužel</a>
 */
public class ParPartyWithCount implements cz.tacr.elza.api.vo.ParPartyWithCount<ParParty> {

    /**
     * Seznam osob.
     */
    private List<ParParty> partyList = new ArrayList<>();

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
     * @param partyList     list osob
     * @param count         počet celkem za minulý dotaz
     */
    public ParPartyWithCount(final List<ParParty> partyList, final Long count) {
        this.partyList = partyList;
        this.count = count;
    }

    /**
     * List osob.
     * @param partyList list osob
     */
    public void setRecordList(final List<ParParty> partyList) {
        this.partyList = partyList;
    }

    /**
     * List osob.
     * @return  list osob
     */
    public List<ParParty> getRecordList() {
        return partyList;
    }

    /**
     * Celkový počet za použitý výraz (be zohledu na ořez aktuálního vraceného listu).
     * @return  celkový počet za použitý výraz (be zohledu na ořez aktuálního vraceného listu)
     */
    public Long getCount() {
        return count;
    }
}
