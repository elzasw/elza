package cz.tacr.elza.repository;

import cz.tacr.elza.domain.ParParty;

import java.util.List;
import java.util.Set;


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
     * @param searchRecord hledaný řetězec, může být null
     * @param partyTypeId typ záznamu
     * @param firstResult id prvního záznamu
     * @param maxResults max počet záznamů
     * @param onlyLocal vyhledat pouze lokální nebo globální osoby
     * @param scopeIds seznam tříd rejstříků, ve kterých se vyhledává
     * @return
     */
    List<ParParty> findPartyByTextAndType(String searchRecord, Integer partyTypeId,
                                                          Integer firstResult, Integer maxResults, Boolean onlyLocal,
    Set<Integer> scopeIds);

    /**
     * Vrátí počet osob vyhovující zadané frázi. Osobu vyhledává podle hesla v rejstříku včetně variantních hesel.
     * @param searchRecord hledaný řetězec, může být null
     * @param partyTypeId typ záznamu
     * @param onlyLocal vyhledat pouze lokální nebo globální osoby
     * @param scopeIds seznam tříd rejstříků, ve kterých se vyhledává
     * @return
     */
    long findPartyByTextAndTypeCount(String searchRecord, Integer partyTypeId, Boolean onlyLocal, Set<Integer> scopeIds);

    /**
     * Nastavi všechny preferred_name na null.
     */
    void unsetAllPreferredName();
}
