package cz.tacr.elza.controller.vo;

import java.util.ArrayList;
import java.util.List;


/**
 * Seznam DMS souborů včetně jejich počtu
 *
 * @author Petr Compel <petr.compel@marbes.cz>
 * @since 20.6.16
 */
public class VOWithCount<T> {

    /**
     * Seznam osob.
     */
    private List<T> list = new ArrayList<>();

    /**
     * Celkový počet dle užitého filtru. Nikoliv aktuální vracený.
     */
    private Long count;


    /**
     * Default pro JSON operace.
     */
    public VOWithCount() {
    }

    /**
     * Konstruktor pro snažší použití.
     *
     * @param list list záznamů
     * @param count      počet celkem za minulý dotaz
     */
    public VOWithCount(final List<T> list, final Long count) {
        this.list = list;
        this.count = count;
    }

    /**
     * List záznamů.
     *
     * @param list list záznamů
     */
    public void setList(final List<T> list) {
        this.list = list;
    }

    /**
     * List záznamů.
     *
     * @return list záznamů
     */
    public List<T> getList() {
        return list;
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
