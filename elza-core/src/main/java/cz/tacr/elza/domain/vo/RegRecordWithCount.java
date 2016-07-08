package cz.tacr.elza.domain.vo;

import cz.tacr.elza.domain.RegRecord;

import java.util.ArrayList;
import java.util.List;

/**
 * Zapouzdření kolekce vracených záznamů rejstříku a jejich celkového počtu za danou vyhledávací podmínku.
 *
 * @author <a href="mailto:martin.kuzel@marbes.cz">Martin Kužel</a>
 */
public class RegRecordWithCount implements cz.tacr.elza.api.vo.RegRecordWithCount<RegRecord> {

    /**
     * Seznam osob.
     */
    private List<RegRecord> recordList = new ArrayList<>();

    /**
     * Celkový počet dle užitého filtru. Nikoliv aktuální vracený.
     */
    private Long count;


    /**
     * Default pro JSON operace.
     */
    public RegRecordWithCount() {
    }

    /**
     * Konstruktor pro snažší použití.
     *
     * @param recordList    list záznamů
     * @param count         počet celkem za minulý dotaz
     */
    public RegRecordWithCount(final List<RegRecord> recordList, final Long count) {
        this.recordList = recordList;
        this.count = count;
    }

    /**
     * List záznamů.
     * @param recordList list záznamů
     */
    public void setRecordList(final List<RegRecord> recordList) {
        this.recordList = recordList;
    }

    /**
     * List záznamů.
     * @return  list záznamů
     */
    public List<RegRecord> getRecordList() {
        return recordList;
    }

    /**
     * Celkový počet za použitý výraz (be zohledu na ořez aktuálního vraceného listu).
     * @return  celkový počet za použitý výraz (be zohledu na ořez aktuálního vraceného listu)
     */
    public Long getCount() {
        return count;
    }
}
