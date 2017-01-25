package cz.tacr.elza.repository;

import java.util.List;
import java.util.Set;

import cz.tacr.elza.domain.ParParty;
import cz.tacr.elza.domain.UsrUser;


/**
 * Repository pro osoby.
 *
 * @author <a href="mailto:martin.kuzel@marbes.cz">Martin Kužel</a>
 */
public interface PartyRepositoryCustom {

    /**
     * Vyhledá osobu daného typu podle zadaného názvu. Vrátí seznam osob vyhovující zadané frázi.
     * Osobu vyhledává podle hesla v rejstříku včetně variantních hesel. Výsledek je stránkovaný, je
     * vrácen zadaný počet záznamů od from záznamu.
     *  @param searchRecord hledaný řetězec, může být null
     * @param partyTypeId  typ záznamu
     * @param firstResult  id prvního záznamu
     * @param maxResults   max počet záznamů
     * @param scopeIds     seznam tříd rejstříků, ve kterých se vyhledává
     * @param readAllScopes
     * @param user
     */
    List<ParParty> findPartyByTextAndType(String searchRecord,
                                          Integer partyTypeId,
                                          Set<Integer> registerTypeIds,
                                          Integer firstResult,
                                          Integer maxResults,
                                          Set<Integer> scopeIds,
                                          final boolean readAllScopes,
                                          final UsrUser user);


    /**
     * Vrátí počet osob vyhovující zadané frázi. Osobu vyhledává podle hesla v rejstříku včetně variantních hesel.
     *  @param searchRecord hledaný řetězec, může být null
     * @param partyTypeId  typ záznamu
     * @param scopeIds     seznam tříd rejstříků, ve kterých se vyhledává
     * @param readAllScopes
     * @param user
     */
    long findPartyByTextAndTypeCount(String searchRecord, Integer partyTypeId, Set<Integer> registerTypeIds, Set<Integer> scopeIds, final boolean readAllScopes, final UsrUser user);


    /**
     * Nastavi všechny preferred_name na null.
     */
    void unsetAllPreferredName();
}
