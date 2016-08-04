package cz.tacr.elza.repository;

import cz.tacr.elza.domain.UsrUser;

/**
 * Rozšířené repository pro uživatele.
 *
 * @author Pavel Stánek
 * @since 15.06.2016
 */
public interface UserRepositoryCustom {
    /**
     * Hledání uživatelů na základě podmínek.
     *
     * @param search      hledaný text
     * @param active      aktivní uživatelé
     * @param disabled    zakázaní uživatelé
     * @param firstResult od jakého záznamu
     * @param maxResults  maximální počet vrácených záznamů
     * @param excludedGroupId Id skupiny která bude vynechána z vyhledávání
     * @return výsledky hledání
     */
    FilteredResult<UsrUser> findUserByTextAndStateCount(String search, Boolean active, Boolean disabled, Integer firstResult, Integer maxResults, Integer excludedGroupId);
}
