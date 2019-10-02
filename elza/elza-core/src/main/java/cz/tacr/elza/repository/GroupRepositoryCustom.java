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
     * @param userId      identifikátor uživatele, podle kterého filtrujeme (pokud je null, nefiltrujeme)
     * @return výsledky hledání
     */
    FilteredResult<UsrGroup> findGroupByTextCount(String search, Integer firstResult, Integer maxResults, Integer userId);
}
