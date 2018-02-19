package cz.tacr.elza.domain.vo;

import java.util.ArrayList;
import java.util.List;

import cz.tacr.elza.domain.ApRecord;

/**
 * Zapouzdření kolekce vracených záznamů rejstříku a jejich celkového počtu za danou vyhledávací podmínku.
 *
 * @author <a href="mailto:martin.kuzel@marbes.cz">Martin Kužel</a>
 */
public class ApRecordWithCount {

    /**
     * Seznam osob.
     */
    private List<ApRecord> recordList = new ArrayList<>();

    /**
     * Celkový počet dle užitého filtru. Nikoliv aktuální vracený.
     */
    private Long count;


    /**
     * Default pro JSON operace.
     */
    public ApRecordWithCount() {
    }

    /**
     * Konstruktor pro snažší použití.
     *
     * @param recordList    list záznamů
     * @param count         počet celkem za minulý dotaz
     */
    public ApRecordWithCount(final List<ApRecord> recordList, final Long count) {
        this.recordList = recordList;
        this.count = count;
    }

    /**
     * List záznamů.
     * @param recordList list záznamů
     */
    public void setRecordList(final List<ApRecord> recordList) {
        this.recordList = recordList;
    }

    /**
     * List záznamů.
     * @return  list záznamů
     */
    public List<ApRecord> getRecordList() {
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
