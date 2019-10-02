package cz.tacr.elza.repository;

import java.util.List;

/**
 * Třída obsahuje informaci o výsledku hledání s omezením vráceného počtu s tránkováním, včetně informace o celkovém počtu záznamů.
 *
 * @param <T> jaké objekty se vracejí
 */
public class FilteredResult<T> {
    /** Od jakého záznamu se data vracely. */
	private int firstResult;
    /** Maximální počet vrácených záznamů. */
	private int maxResults;
    /** Celkový počet záznamů pro daná kriteria hledání. */
	private int totalCount;
    /** Seznam objektů. */
    private List<T> list;

	public FilteredResult(final int firstResult, final int maxResults, final int totalCount, final List<T> list) {
        this.firstResult = firstResult;
        this.maxResults = maxResults;
        this.totalCount = totalCount;
        this.list = list;
    }

    /** Od jakého záznamu se data vracely. */
	public int getFirstResult() {
        return firstResult;
    }

    /** Maximální počet vrácených záznamů. */
	public int getMaxResults() {
        return maxResults;
    }

    /** Celkový počet záznamů pro daná kriteria hledání. */
	public int getTotalCount() {
        return totalCount;
    }

    /** Seznam objektů. */
    public List<T> getList() {
        return list;
    }
}
