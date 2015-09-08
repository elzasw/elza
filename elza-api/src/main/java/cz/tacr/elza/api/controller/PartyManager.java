package cz.tacr.elza.api.controller;

import cz.tacr.elza.api.ParAbstractParty;
import cz.tacr.elza.api.ParAbstractPartyVals;
import cz.tacr.elza.api.ParPartyTypeExt;

import java.util.List;

/**
 * Rozhraní operací pro osoby a rejstřík.
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 12. 8. 2015
 *
 * @param <PAPV> {@link ParAbstractPartyVals}
 */
public interface PartyManager<PAPV extends ParAbstractPartyVals> {


    /**
     * Vrátí všechny typy osob včetně podtypů.
     * 
     * @return typy osob včetně podtypu.
     */
    List<? extends ParPartyTypeExt> getPartyTypes();

    /**
     * Vloží záznam o abstraktní osobě. Je umožněno vložit návázané rejstříkové heslo a podtyp.
     * 
     * @param abstractParty data o abstraktní osobě
     * @return Založený záznam.
     */
    ParAbstractParty insertAbstractParty(PAPV abstractParty);

    /**
     * Upraví záznam abstraktní osoby. Je umožněna změna rejstříkového hesla a podtypu.
     * 
     * @param abstractPartyId id záznamu pro aktualizaci.
     * @param abstractParty data o abstraktní osobě pro aktualizaci {@link ParAbstractPartyVals}.
     * @return Aktualizovaný záznam.
     */
    ParAbstractParty updateAbstractParty(Integer abstractPartyId, PAPV abstractParty);

    /**
     * Smaže abstraktní osobu.
     * 
     * @param abstractPartyId id záznamu pro samzání.
     */
    void deleteAbstractParty(Integer abstractPartyId);

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
    List<? extends ParAbstractParty> findAbstractParty(String search, Integer from, Integer count,
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
    Long findAbstractPartyCount(String search, Integer partyTypeId, Boolean originator);

    /**
     * Vrátí abstraktní osobu na základě identifikátoru.
     * 
     * @param abstractPartyId identifikátor abstraktní osoby.
     * @return nalezená abstraktní osoba.
     */
    ParAbstractParty getAbstractParty(Integer abstractPartyId);
}
