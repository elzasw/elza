package cz.tacr.elza.controller.vo;

import java.util.List;

/**
 * Třída obsahující seznam vrácený na základě hledání (ten může být omezen např. maximálním vráceným počtem), dále obsahuje
 * celkový počet všech záznamů na základě daného filtru.
 *
 * @author Pavel Stánek
 * @since 15.06.2016
 */
public class FilteredResultVO<T> {
    /** Celkový počet záznamů pro daná kriteria hledání. */
    private long totalCount;
    /** Seznam objektů. */
    private List<T> list;

    public FilteredResultVO(final List<T> list, final long totalCount) {
        this.totalCount = totalCount;
        this.list = list;
    }

    public long getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(final long totalCount) {
        this.totalCount = totalCount;
    }

    public List<T> getList() {
        return list;
    }

    public void setList(final List<T> list) {
        this.list = list;
    }
}
