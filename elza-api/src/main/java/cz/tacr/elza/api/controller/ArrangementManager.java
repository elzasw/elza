package cz.tacr.elza.api.controller;

import java.util.List;

import cz.tacr.elza.api.*;
import cz.tacr.elza.api.ArrFund;
import cz.tacr.elza.api.exception.ConcurrentUpdateException;
import cz.tacr.elza.api.vo.ArrCalendarTypes;
import cz.tacr.elza.api.vo.ArrDescItemSavePack;
import cz.tacr.elza.api.vo.ArrDescItems;
import cz.tacr.elza.api.vo.ArrLevelPack;
import cz.tacr.elza.api.vo.ArrNodeHistoryPack;
import cz.tacr.elza.api.vo.ArrNodeRegisterPack;
import cz.tacr.elza.api.vo.RelatedNodeDirectionWithDescItem;
import cz.tacr.elza.api.vo.RelatedNodeDirectionWithDescItems;
import cz.tacr.elza.api.vo.RelatedNodeDirectionWithLevelPack;
import cz.tacr.elza.api.vo.ScenarioOfNewLevel;


/**
 * Rozhraní operací pro archivní pomůcku a hierarchický přehled včetně atributů.
 *
 * @param <FA> {@link ArrFund} archivní pomůcka
 * @param <FV> {@link ArrFundVersion} verze archivní pomůcky
 * @param <DI> {@link ArrDescItem} archivní popis
 * @param <DISP> {@link ArrDescItemSavePack} zapouzdřuje archivní popisy k uložení spolu s těmi ke smazání,
 *              id archivní pomůcky a příznakem, zdali vytvářet novou změnu
 * @param <FL> {@link ArrLevel} úroveň hierarchického popisu
 * @param <FLP> {@link ArrLevelPack} zapouzdření hierarchického popisu (úrovně), cílové úrovně pro operace, kořenového uzlu,
 *             dodatečného uzlu (zámek pro úroveň) a id příslušné archivní pomůcky
 * @param <N> {@link ArrNode} uzel jako zámek pro hierarchický popis
 * @param <ANR> {@link ArrNodeRegister} vazba uzlu na rejstříkové heslo
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 12. 8. 2015
 */
public interface ArrangementManager<FA extends ArrFund, FV extends ArrFundVersion, DI extends ArrDescItem,
        DISP extends ArrDescItemSavePack, FL extends ArrLevel, FLP extends ArrLevelPack, N extends ArrNode,
        DIS extends ArrDescItems, NHP extends ArrNodeHistoryPack, CTL extends ArrCalendarTypes, ANR extends ArrNodeRegister,
        ANRP extends ArrNodeRegisterPack, AP extends ArrPacket, APT extends RulPacketType,
        RNDWDIS extends RelatedNodeDirectionWithDescItems, RNDWDI extends RelatedNodeDirectionWithDescItem,
        RNDWLFP extends RelatedNodeDirectionWithLevelPack, SONL extends ScenarioOfNewLevel> {

    /** Formát popisu atributu - dlouhá verze. */
    String FORMAT_ATTRIBUTE_FULL = "FULL";

    /** Formát popisu atributu - krátká verze. */
    String FORMAT_ATTRIBUTE_SHORT = "SHORT";

    /**
     * Vytvoří novou archivní pomůcku se zadaným názvem. Jako datum založení vyplní aktuální datum a čas.
     *
     * @param name              název archivní pomůcky
     * @param arrangementTypeId id typu výstupu
     * @param ruleSetId         id pravidel podle kterých se vytváří popis
     * @return nová archivní pomůcka
     */
    ArrFund createFund(String name, Integer arrangementTypeId, Integer ruleSetId);

    /**
     * Smaže archivní pomůcku se zadaným id. Maže kompletní strukturu se všemi závislostmi.
     *
     * @param fundId id archivní pomůcky
     */
    void deleteFund(Integer fundId);

    /**
     * Vrátí všechny archivní pomůcky.
     *
     * @return všechny archivní pomůcky
     */
    List<? extends ArrFund> getFunds();

    /**
     * Aktualizace archivní pomůcky.
     *
     * @param fund archivní pomůcka s vyplěnými údaji a ID
     * @return aktualizovaná archivní pomůcka
     * @throws ConcurrentUpdateException chyba při současné manipulaci s položkou více uživateli.
     */
    ArrFund updateFund(FA fund) throws ConcurrentUpdateException;

    /**
     * Vrátí seznam verzí pro danou archivní pomůcku seřazený od nejnovější k nejstarší.
     *
     * @param fundId id archivní pomůcky
     * @return seznam verzí pro danou archivní pomůcku seřazený od nejnovější k nejstarší
     */
    List<? extends ArrFundVersion> getFundVersions(Integer fundId);

    /**
     * Vrátí archivní pomůcku.
     *
     * @param fundId id archivní pomůcky
     * @return archivní pomůcka
     */
    ArrFund getFund(Integer fundId);

    /**
     * Uzavře otevřenou verzi archivní pomůcky a otevře novou verzi.
     *
     * @param version verze, která se má uzavřít
     * @param arrangementTypeId id typu výstupu nové verze
     * @param ruleSetId         id pravidel podle kterých se vytváří popis v nové verzi
     * @return nová verze archivní pomůcky
     * @throws ConcurrentUpdateException chyba při současné manipulaci s položkou více uživateli
     */
    ArrFundVersion approveVersion(FV version, Integer arrangementTypeId, Integer ruleSetId) throws ConcurrentUpdateException;

    /**
     * Vytvoří novou úroveň (level) před předanou úrovní (level).
     *
     * @param levelPack   object obsahující úroveň (level), její parent node (extra node) a id pomůcky
     * @return            objekt obsahující novou úroveň (level) a parent node (jako extra node)
     */
    RNDWLFP addLevelBefore(FLP levelPack);

    /**
     * Vytvoří novou úroveň (level) za předanou úrovní (level).
     *
     * @param levelPack        object obsahující úroveň (level), její parent node (extra node) a id pomůcky
     * @return            objekt obsahující novou úroveň (level) a parent node (jako extra node)
     */
    RNDWLFP addLevelAfter(FLP levelPack);

    /**
     * Vytvoří novou úroveň (level) na poslední pozici pod předanou úrovní.
     *
     * @param levelPack        object obsahující úroveň (level) a id pomůcky
     * @return            objekt obsahující novou úroveň (level) a node (jako extra node)
     */
    RNDWLFP addLevelChild(FLP levelPack);

    /**
     * Přesune úroveň před předanou úroveň.
     *
     * @param levelPack            úroveň která se přesouvá (level), před kterou se přesouvá (targetLevel) a id pomůcky
     * @return                přesunutá úroveň (level) a původní cílová (targetLevel)
     */
    RNDWLFP moveLevelBefore(FLP levelPack);

    /**
     * Přesune úroveň na poslední pozici pod předaným uzlem.
     *
     * @param levelPack       úroveň která se přesouvá (level), uzel před která se přesouvá (extraNode) a id pomůcky
     * @return           přesunutá úroveň (level) a původní uzel (extraNode)
     */
    RNDWLFP moveLevelUnder(FLP levelPack);

    /**
     * Přesune úroveň za předanou úroveň.
     *
     * @param levelPack  úroveň která se přesouvá (level), před kterou se přesouvá (targetLevel) a id pomůcky
     * @return      přesunutá úroveň (level) a původní úroveň (targetLevel)
     */
    RNDWLFP moveLevelAfter(FLP levelPack);

    /**
     * Smaže úroveň.
     *
     * @param levelPack            úroveň která se maže (level), uzel rodiče (extraNode) a id pomůcky
     * @return                smazaná úroveň, původní uzel rodiče (extraNode)
     */
    RNDWLFP deleteLevel(FLP levelPack);

    /**
     * Načte neuzavřenou verzi archivní pomůcky.
     *
     * @param fundId      id archivní pomůcky
     * @return                  verze
     */
    ArrFundVersion getOpenVersionByFundId(Integer fundId);

    /**
     * Načte verzi podle identifikátoru.
     *
     * @param fundVersionId      id verze
     * @return               verze s daným identifikátorem
     */
    ArrFundVersion getFundVersionById(Integer fundVersionId);

    /**
     * Načte potomky daného uzlu v konkrétní verzi. Pokud není identifikátor verze předaný načítají se potomci
     * z neuzavřené verze.
     *
     * @param nodeId          id rodiče
     * @param versionId       id verze, může být null
     * @param formatData      formátování dat, může být null - SHORT, FULL
     * @param descItemTypeIds typy atributů, může být null
     * @return                potomci předaného uzlu
     */
    List<? extends ArrLevelExt> findSubLevels(Integer nodeId, Integer versionId, String formatData, Integer[] descItemTypeIds);

    /**
     * Načte potomky dané úrovně v konkrétní verzi. Pokud není identifikátor verze předaný načítají se potomci
     * z neuzavřené verze.
     *
     * @param nodeId          id uzlu rodiče
     * @param versionId       id verze, může být null
     * @return                potomci předaného uzlu
     */
    List<? extends ArrLevel> findSubLevels(Integer nodeId, Integer versionId);

    /**
     * Načte úroveň podle identifikátoru. K ní doplní atributy.
     *
     * @param nodeId           id uzlu
     * @param versionId        id verze, může být null
     * @param descItemTypeIds  typy atributů, může být null
     * @return                 uzel s daným identifikátorem
     */
    ArrLevelExt getLevel(Integer nodeId, Integer versionId, Integer[] descItemTypeIds);

    /**
     * Přidá atribut archivního popisu včetně hodnoty k existující jednotce archivního popisu.
     *
     * @param descItemExt       atribut archivního popisu k vytvoření
     * @param fundVersionId       id verze
     * @return                  vytvořený atribut archivního popisu
     */
    RNDWDI createDescriptionItem(DI descItemExt, Integer fundVersionId);

    /**
     * Upraví hodnotu existujícího atributu archivního popisu.
     *
     * @param descItemExt       atribut archivního popisu k upravení
     * @param fundVersionId       id verze
     * @param createNewVersion  zda-li se má vytvářet nová verze
     * @return                  upravený atribut archivního popisu
     */
    RNDWDI updateDescriptionItem(DI descItemExt, Integer fundVersionId, Boolean createNewVersion);

    /**
     * Vymaže atribut archivního popisu.
     *
     * @param descItemExt       atribut archivního popisu ke smazání
     * @param fundVersionId       id verze
     * @return                  upravený(smazaný) atribut archivního popisu
     */
    RNDWDI deleteDescriptionItem(DI descItemExt, Integer fundVersionId);


    /**
     * Hromadně upraví archivní popis, resp. uloží hodnoty předaných atributů.
     *
     * @param descItemSavePack  object nesoucí předávané atributy archivního popisu k vytvoření/úpravě/smazání
     * @return                  upravené atributy archivního popisu
     */
    RNDWDIS saveDescriptionItems(DISP descItemSavePack);

    /**
     * Vrátí všechny hodnoty atributu archivního popisu k uzlu.
     * @param fundVersionId       Identifikátor verze
     * @param nodeId            Identifikátor uzlu
     * @param rulDescItemTypeId Identifikátor typu atributu
     * @return  Seznam hodnot atrubutu
     */
    List<DI> getDescriptionItemsForAttribute(Integer fundVersionId, Integer nodeId, Integer rulDescItemTypeId);


    /**
     * Vrátí seznam změn uzlů a atributů v jednotlivých verzích.
     * @param nodeId        Identifikátor uzlu
     * @param fundId  Identifikátor archivní pomůcky
     * @return              Seznam změn
     */
    NHP getHistoryForNode(Integer nodeId, Integer fundId);


    /**
     * Vrátí seznam typů kalendářů.
     * @return  Seznam typů kalendářů
     */
    CTL getCalendarTypes();

    /**
     * Vrátí vazby mezi uzlem a rejstříkovými hesly za danou verzi.
     *
     * @param versionId     id verze
     * @param nodeId        id uzlu
     * @return              seznam vazeb, může být prázdný
     */
    List<ANR> findNodeRegisterLinks(Integer versionId, Integer nodeId);

    /**
     * Uloží vazby mezi uzlem a hesly rejstříku. Provede založení změny.
     *
     * @param   arrNodeRegisterPack zapouzření kolekece k uložení či smazání vazeb
     * @param versionId id verze
     */
    void modifyArrNodeRegisterLinks(ANRP arrNodeRegisterPack, Integer versionId);

    /**
     * Vyhledá obal daného typu podle zadaného názvu. Vrátí seznam obalů vyhovující zadané frázi.
     * Výsledek je stránkovaný, je vrácen zadaný počet záznamů od from záznamu.
     * @param search fráze pro vyhledávání
     * @param from pořadí prvního záznamu
     * @param count počet záznamů pro vrácení
     * @param packetTypeId id typu
     * @return
     */
    List<AP> findPacket(String search, Integer from, Integer count, Integer packetTypeId);

    /**
     *  Vrátí počet obalů vyhovující zadané frázi.
     * @param search fráze pro vyhledávání
     * @param packetTypeId id typu
     * @return
     */
    Long findPacketCount(String search, Integer packetTypeId);

    /**
     * Vloží záznam o obalu s vyplněnou archyvní pomůckou a podtypem.
     * @param packet data obalu s vyplněnými vazbami na archyvní pomůcku a podtyp.
     * @return založený záznam
     */
    AP insertPacket(AP packet);

    /**
     * Upravý záznam obalu.
     * @param packet data obalu s ID a vyplněnými vazbami na archyvní pomůcku a podtyp.
     * @return aktualizovaný záznam
     */
    AP updatePacket(AP packet);

    /**
     * Zneaktivní obal.
     * @param packetId id obalu.
     */
    void deactivatePacket(Integer packetId);

    /**
     * Vrátí všechny typy obalů.
     * @return všechny typy obalů.
     */
    List<APT> getPacketTypes();


    /**
     * Informace o možných scénářích založení nového uzlu - před.
     *
     * @param nodeId      id uzlu
     * @param fundVersionId id verze
     * @return seznam možných scénařů
     */
    List<SONL> getDescriptionItemTypesForNewLevelBefore(Integer nodeId, Integer fundVersionId);


    /**
     * Informace o možných scénářích založení nového uzlu - po.
     *
     * @param nodeId      id uzlu
     * @param fundVersionId id verze
     * @return seznam možných scénařů
     */
    List<SONL> getDescriptionItemTypesForNewLevelAfter(Integer nodeId, Integer fundVersionId);


    /**
     * Informace o možných scénářích založení nového uzlu - pod.
     *
     * @param nodeId      id uzlu
     * @param fundVersionId id verze
     * @return seznam možných scénařů
     */
    List<SONL> getDescriptionItemTypesForNewLevelChild(Integer nodeId, Integer fundVersionId);
}
