package cz.tacr.elza.api.controller;

import cz.tacr.elza.api.ParParty;
import cz.tacr.elza.api.ParPartyTypeExt;
import cz.tacr.elza.api.vo.ParPartyWithCount;

import java.util.List;

/**
 * Rozhraní operací pro osoby a rejstřík.
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 12. 8. 2015
 *
 * @param <PAPV> {@link ParParty} osoba
 */
public interface PartyManager<PAPV extends ParParty> {

    /**
     * Vrátí všechny typy osob včetně podtypů.
     *
     * @return typy osob včetně navázaných podtypů
     */
    List<? extends ParPartyTypeExt> getPartyTypes();

    /**
     * Vloží záznam o osobě. Je nutné vložit návázané rejstříkové heslo a podtyp.
     *
     * @param party     data osoby s vyplněnými vazbami na heslo a podtyp
     * @return          založený záznam
     */
    ParParty insertParty(PAPV party);

    /**
     * Upraví záznam osoby. Je umožněna změna rejstříkového hesla a podtypu.
     *
     * @param party     data osoby s ID a vyplněnými vazbami na heslo a podtyp
     * @return          aktualizovaný záznam
     */
    ParParty updateParty(PAPV party);

    /**
     * Smaže abstraktní osobu.
     * 
     * @param partyId id záznamu pro samzání
     */
    void deleteParty(Integer partyId);

    /**
     * Vyhledá osobu daného typu podle zadaného názvu. Vrátí seznam osob vyhovující zadané frázi.
     * Osobu vyhledává podle hesla v rejstříku včetně variantních hesel. Výsledek je stránkovaný, je
     * vrácen zadaný počet záznamů od from záznamu.
     *
     * @param search        fráze pro vyhledávání
     * @param from          pořadí prvního záznamu
     * @param count         počet záznamů pro vrácení
     * @param partyTypeId   id typu.
     * @param originator    původce - true, není původce - false, null - neaplikuje filtr - vrací obě možnosti
     * @return              vo se seznamem osob (s vazbami na ostatní entity) vyhovující zadané frázi a podmínkám
     *                      s uvedeným celkovým počtem
     */
    ParPartyWithCount findParty(String search, Integer from, Integer count,
                                Integer partyTypeId, Boolean originator);

    /**
     * Vrátí abstraktní osobu na základě identifikátoru.
     * 
     * @param partyId   identifikátor osoby
     * @return          nalezená abstraktní osoba (s vazbami na ostatní entity)
     */
    ParParty getParty(Integer partyId);
}
