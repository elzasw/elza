package cz.tacr.elza.repository;

import java.util.List;

/**
 * Třída obsahuje informaci o výsledku hledání s omezením vráceného počtu s tránkováním, včetně informace o celkovém počtu záznamů.
 *
 * @param <T> jaké objekty se vracejí
 * @author Pavel Stánek
 * @since 15.06.2016
 */
public class FilteredResult<T> {
    /** Od jakého záznamu se data vracely. */
    private long firstResult;
    /** Maximální počet vrácených záznamů. */
    private long maxResults;
    /** Celkový počet záznamů pro daná kriteria hledání. */
    private long totalCount;
    /** Seznam objektů. */
    private List<T> list;

    public FilteredResult(final long firstResult, final long maxResults, final long totalCount, final List<T> list) {
        this.firstResult = firstResult;
        this.maxResults = maxResults;
        this.totalCount = totalCount;
        this.list = list;
    }

    /** Od jakého záznamu se data vracely. */
    public long getFirstResult() {
        return firstResult;
    }

    /** Maximální počet vrácených záznamů. */
    public long getMaxResults() {
        return maxResults;
    }

    /** Celkový počet záznamů pro daná kriteria hledání. */
    public long getTotalCount() {
        return totalCount;
    }

    /** Seznam objektů. */
    public List<T> getList() {
        return list;
    }
}
