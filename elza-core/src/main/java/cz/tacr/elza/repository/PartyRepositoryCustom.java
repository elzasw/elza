package cz.tacr.elza.repository;

import cz.tacr.elza.domain.ParParty;

import java.util.List;

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
     * @param registerTypeId typ záznamu
     * @param firstResult id prvního záznamu
     * @param maxResults max počet záznamů
     * @param onlyLocal vyhledat pouze lokální nebo globální osoby
     * @return
     */
    List<ParParty> findPartyByTextAndType(String searchRecord, Integer registerTypeId,
                                                          Integer firstResult, Integer maxResults, Boolean onlyLocal);

    /**
     * Vrátí počet osob vyhovující zadané frázi. Osobu vyhledává podle hesla v rejstříku včetně variantních hesel.
     * @param searchRecord hledaný řetězec, může být null
     * @param registerTypeId typ záznamu
     * @param onlyLocal vyhledat pouze lokální nebo globální osoby
     * @return
     */
    long findPartyByTextAndTypeCount(String searchRecord, Integer registerTypeId, Boolean onlyLocal);

    /**
     * Nastavi všechny preferred_name na null.
     */
    void unsetAllPreferredName();
}
