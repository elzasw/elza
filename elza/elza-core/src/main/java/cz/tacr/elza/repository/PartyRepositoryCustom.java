package cz.tacr.elza.repository;

import java.util.List;
import java.util.Set;

import cz.tacr.elza.domain.ApState;
import cz.tacr.elza.domain.ParParty;


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
     *
     * @param searchRecord hledaný řetězec, může být null
     * @param partyTypeId typ záznamu
     * @param firstResult id prvního záznamu
     * @param maxResults max počet záznamů
     * @param scopeIds seznam tříd rejstříků, ve kterých se vyhledává
     */
    List<ParParty> findPartyByTextAndType(
            String searchRecord,
            Integer partyTypeId,
            Set<Integer> apTypeIds,
            Integer firstResult,
            Integer maxResults,
            Set<Integer> scopeIds,
            Set<ApState.StateApproval> approvalStates);


    /**
     * Vrátí počet osob vyhovující zadané frázi. Osobu vyhledává podle hesla v rejstříku včetně variantních hesel.
     *
     * @param searchRecord hledaný řetězec, může být null
     * @param partyTypeId typ záznamu
     * @param scopeIds seznam tříd rejstříků, ve kterých se vyhledává
     */
    long findPartyByTextAndTypeCount(
            String searchRecord,
            Integer partyTypeId,
            Set<Integer> apTypeIds,
            Set<Integer> scopeIds,
            Set<ApState.StateApproval> approvalStates);

    /**
     * Nastavi všechny preferred_name na null.
     */
    void unsetAllPreferredName();
}
