package cz.tacr.elza.controller.vo;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * Třída obsahující seznam vrácený na základě hledání (ten může být omezen např.
 * maximálním vráceným počtem), dále obsahuje
 * celkový počet všech záznamů na základě daného filtru.
 * 
 * Třída objekty ukládá ve vlastní kopii seznamu
 *
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

    /**
     * Kopirovaci konstruktor z jineho seznamu
     * 
     * @param srcRows
     * @param count
     */
    public FilteredResultVO(final List<T> srcRows, final long count) {
        this.count = count;
        this.rows = new ArrayList<>(srcRows);
    }

    /**
     * Mapovaci konstruktor
     * 
     * @param srcRows
     * @param mapper
     * @param count
     */
    public <OTHER> FilteredResultVO(final List<OTHER> srcRows, Function<OTHER, T> mapper, final long count) {
        this.count = count;
        this.rows = new ArrayList<>(srcRows.size());
        for (OTHER other : srcRows) {
            T t = mapper.apply(other);
            rows.add(t);
        }
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
