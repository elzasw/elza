package cz.tacr.elza.repository;

import cz.tacr.elza.domain.UsrGroup;

/**
 * @author Pavel Stánek
 * @since 15.06.2016
 */
public interface GroupRepositoryCustom {
    /**
     * Hledání skupin na základě podmínek.
     *
     * @param search      hledaný text
     * @param firstResult od jakého záznamu
     * @param maxResults  maximální počet vrácených záznamů
     * @return výsledky hledání
     */
    FilteredResult<UsrGroup> findGroupByTextCount(String search, Integer firstResult, Integer maxResults);
    /**
     * Hledání skupin na základě podmínek.
     *
     * @param search      hledaný text
     * @param firstResult od jakého záznamu
     * @param maxResults  maximální počet vrácených záznamů, pokud je -1 neomezuje se
     * @return výsledky hledání
     */
    FilteredResult<UsrGroup> findGroupWithFundCreateByTextCount(String search, Integer firstResult, Integer maxResults);
}
