package cz.tacr.elza.controller.vo;

import java.util.ArrayList;
import java.util.List;


/**
 * Seznam rejstříkových hesel s počtem všech hesel.
 *
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 21.12.2015
 */
public class RegRecordWithCount<T extends AbstractRegRecord> {

    /**
     * Seznam osob.
     */
    private List<T> recordList = new ArrayList<>();

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
     * @param recordList list záznamů
     * @param count      počet celkem za minulý dotaz
     */
    public RegRecordWithCount(final List<T> recordList, final Long count) {
        this.recordList = recordList;
        this.count = count;
    }

    /**
     * List záznamů.
     *
     * @param recordList list záznamů
     */
    public void setRecordList(final List<T> recordList) {
        this.recordList = recordList;
    }

    /**
     * List záznamů.
     *
     * @return list záznamů
     */
    public List<T> getRecordList() {
        return recordList;
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
