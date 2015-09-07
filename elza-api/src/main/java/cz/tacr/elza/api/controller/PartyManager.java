package cz.tacr.elza.api.controller;

import java.util.List;

import cz.tacr.elza.api.ParAbstractParty;
import cz.tacr.elza.api.ParAbstractPartyVals;
import cz.tacr.elza.api.ParPartyType;

/**
 * Rozhraní operací pro osoby a rejstřík.
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 12. 8. 2015
 */
public interface PartyManager<PAPV extends ParAbstractPartyVals> {


    /**
     * Vrátí všechny typy osob včetně podtypu.
     * 
     * @return
     */
    List<? extends ParPartyType> getPartyTypes();

    /**
     * Vloží záznam o abstraktní osobě. Je umožněno vložit návázané rejstříkové heslo a podtyp.
     * 
     * @param abstractParty
     * @return
     */
    ParAbstractParty insertAbstractParty(PAPV abstractParty);

    /**
     * Upraví záznam abstraktní osoby. Je umožněna změna rejstříkového hesla a podtypu.
     * 
     * @param abstractPartyId
     * @param abstractParty
     * @return
     */
    ParAbstractParty updateAbstractParty(Integer abstractPartyId, PAPV abstractParty);

    /**
     * Smaže abstraktní osobu.
     * 
     * @param abstractPartyId
     */
    void deleteAbstractParty(Integer abstractPartyId);

    /**
     * Vyhledá osobu daného typu podle zadaného názvu. Vrátí seznam osob vyhovující zadané frázi.
     * Osobu vyhledává podle hesla v rejstříku včetně variantních hesel. Výsledek je stránkovaný, je
     * vrácen zadaný počet záznamů od from záznamu.
     * 
     * @param search
     * @param from
     * @param count
     * @param partyTypeId
     * @return
     */
    List<? extends ParAbstractParty> findAbstractParty(String search, Integer from, Integer count,
            Integer partyTypeId);

    /**
     * Vrátí seznam osob vyhovující zadané frázi. Osobu vyhledává podle hesla v rejstříku včetně
     * variantních hesel.
     * 
     * @param search
     * @param partyTypeId
     * @return
     */
    Long findAbstractPartyCount(String search, Integer partyTypeId);

    /**
     * Vrátí abstraktní osobu na základě identifikátoru.
     * 
     * @param abstractPartyId
     * @return
     */
    ParAbstractParty getAbstractParty(Integer abstractPartyId);
}
