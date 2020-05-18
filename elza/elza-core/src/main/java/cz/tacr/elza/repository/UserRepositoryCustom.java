package cz.tacr.elza.repository;

import cz.tacr.elza.core.data.SearchType;
import cz.tacr.elza.domain.UsrUser;

/**
 * Rozšířené repository pro uživatele.
 *
 */
public interface UserRepositoryCustom {

	FilteredResult<UsrUser> findUserByText(String search, boolean active, boolean disabled, int firstResult,
                                           int maxResults, Integer excludedGroupId, SearchType searchTypeName, SearchType searchTypeUsername);

	/**
	 * Hledání uživatelů na základě podmínek.
	 *
	 * @param search
	 *            hledaný text
	 * @param active
	 *            aktivní uživatelé
	 * @param disabled
	 *            zakázaní uživatelé
	 * @param firstResult
	 *            od jakého záznamu
	 * @param maxResults
	 *            maximální počet vrácených záznamů
	 * @param excludedGroupId
	 *            Id skupiny která bude vynechána z vyhledávání
	 * @param userId
	 *            identifikátor uživatele, podle kterého filtrujeme (výsledek je z jeho pohledu)
	 * @param includeUser
	 *             Flag if userId should be included in considered users by the query
	 * @return výsledky hledání
	 */
	FilteredResult<UsrUser> findUserByTextAndStateCount(String search, boolean active, boolean disabled,
	        int firstResult, int maxResults, Integer excludedGroupId, int userId, boolean includeUser,
                                                       SearchType searchTypeName, SearchType searchTypeUsername);
}
