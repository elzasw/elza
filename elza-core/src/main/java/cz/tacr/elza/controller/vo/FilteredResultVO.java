package cz.tacr.elza.controller.vo;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

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
    private long count;

    /** Seznam objektů. */
    @JsonTypeInfo(use=JsonTypeInfo.Id.CLASS, include=JsonTypeInfo.As.PROPERTY, property="@class")
    private List<T> rows;

    public FilteredResultVO() {
    }

    public FilteredResultVO(final List<T> rows, final long count) {
        this.count = count;
        this.rows = rows;
    }

    public long getCount() {
        return count;
    }

    public void setCount(final long count) {
        this.count = count;
    }

    public List<T> getRows() {
        return rows;
    }

    public void setRows(final List<T> rows) {
        this.rows = rows;
    }
}
