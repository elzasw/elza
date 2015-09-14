package cz.tacr.elza.api.controller;

import cz.tacr.elza.api.ParParty;
import cz.tacr.elza.api.ParPartyTypeExt;

import java.util.List;

/**
 * Rozhraní operací pro osoby a rejstřík.
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 12. 8. 2015
 *
 * @param <PAPV> {@link ParParty}
 */
public interface PartyManager<PAPV extends ParParty> {


    /**
     * Vrátí všechny typy osob včetně podtypů.
     *
     * @return typy osob včetně podtypu.
     */
    List<? extends ParPartyTypeExt> getPartyTypes();

    /**
     * Vloží záznam o osobě. Je umožněno vložit návázané rejstříkové heslo a podtyp.
     *
     * @param party data o abstraktní osobě
     * @return Založený záznam.
     */
    ParParty insertParty(PAPV party);

    /**
     * Upraví záznam abstraktní osoby. Je umožněna změna rejstříkového hesla a podtypu.
     *
     * @param party záznamu pro aktualizaci.
     * @return Aktualizovaný záznam.
     */
    ParParty updateParty(PAPV party);

    /**
     * Smaže abstraktní osobu.
     * 
     * @param partyId id záznamu pro samzání.
     */
    void deleteParty(Integer partyId);

    /**
     * Vyhledá osobu daného typu podle zadaného názvu. Vrátí seznam osob vyhovující zadané frázi.
     * Osobu vyhledává podle hesla v rejstříku včetně variantních hesel. Výsledek je stránkovaný, je
     * vrácen zadaný počet záznamů od from záznamu.
     *
     * @param search fráze pro vyhledávání.
     * @param from pořadí prvního záznamu.
     * @param count počet záznamů pro vrácení.
     * @param partyTypeId id typu.
     * @param originator        původce - true, není původce - false, null - neaplikuje filtr - obě možnosti
     * @return seznam osob vyhovující zadané frázi.
     */
    List<? extends ParParty> findParty(String search, Integer from, Integer count,
                                       Integer partyTypeId, Boolean originator);

    /**
     * Vrátí počet osob vyhovující zadané frázi. Osobu vyhledává podle hesla v rejstříku včetně
     * variantních hesel.
     *
     * @param search fráze pro vyhledávání.
     * @param partyTypeId id typu.
     * @param originator        původce - true, není původce - false, null - neaplikuje filtr - obě možnosti
     * @return počet osob vyhovující zadané frázi.
     */
    Long findPartyCount(String search, Integer partyTypeId, Boolean originator);

    /**
     * Vrátí abstraktní osobu na základě identifikátoru.
     * 
     * @param partyId identifikátor osoby
     * @return nalezená abstraktní osoba
     */
    ParParty getParty(Integer partyId);
}
