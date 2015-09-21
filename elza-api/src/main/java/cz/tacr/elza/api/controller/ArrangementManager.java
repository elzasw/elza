package cz.tacr.elza.api.controller;

import java.util.List;

import cz.tacr.elza.api.ArrDescItem;
import cz.tacr.elza.api.ArrFindingAid;
import cz.tacr.elza.api.ArrFindingAidVersion;
import cz.tacr.elza.api.ArrLevel;
import cz.tacr.elza.api.ArrLevelExt;
import cz.tacr.elza.api.ArrNode;
import cz.tacr.elza.api.exception.ConcurrentUpdateException;
import cz.tacr.elza.api.vo.ArrDescItemSavePack;
import cz.tacr.elza.api.vo.ArrDescItems;
import cz.tacr.elza.api.vo.ArrLevelPack;


/**
 * Rozhraní operací pro archivní pomůcku a hierarchický přehled včetně atributů.
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 12. 8. 2015
 */
public interface ArrangementManager<FA extends ArrFindingAid, FV extends ArrFindingAidVersion, DI extends ArrDescItem,
    DISP extends ArrDescItemSavePack, FL extends ArrLevel, FLP extends ArrLevelPack, N extends ArrNode, DIS extends ArrDescItems> {

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
    ArrFindingAid createFindingAid(String name, Integer arrangementTypeId, Integer ruleSetId);

    /**
     * Smaže archivní pomůcku se zadaným id. Maže kompletní strukturu se všemi závislostmi.
     *
     * @param findingAidId id archivní pomůcky
     */
    void deleteFindingAid(Integer findingAidId);

    /**
     * Vrátí všechny archivní pomůcky.
     *
     * @return všechny archivní pomůcky
     */
    List<? extends ArrFindingAid> getFindingAids();

    /**
     * Aktualizace názvu archivní pomůcky.
     *
     * @param findingAid archivní pomůcka
     * @return aktualizovaná archivní pomůcka
     * @throws ConcurrentUpdateException chyba při současné manipulaci s položkou více uživateli.
     */
    ArrFindingAid updateFindingAid(FA findingAid) throws ConcurrentUpdateException;

    /**
     * Vrátí seznam verzí pro danou archivní pomůcku seřazený od nejnovější k nejstarší.
     *
     * @param findingAidId id archivní pomůcky
     * @return seznam verzí pro danou archivní pomůcku seřazený od nejnovější k nejstarší
     */
    List<? extends ArrFindingAidVersion> getFindingAidVersions(Integer findingAidId);

    /**
     * Vrátí archivní pomůcku.
     *
     * @param findingAidId id archivní pomůcky
     * @return archivní pomůcka
     */
    ArrFindingAid getFindingAid(Integer findingAidId);

    /**
     * Uzavře otevřenou verzi archivní pomůcky a otevře novou verzi.
     *
     * @param version verze, která se má uzavřít
     * @param arrangementTypeId id typu výstupu nové verze
     * @param ruleSetId         id pravidel podle kterých se vytváří popis v nové verzi
     * @return nová verze archivní pomůcky
     * @throws ConcurrentUpdateException chyba při současné manipulaci s položkou více uživateli
     */
    ArrFindingAidVersion approveVersion(FV version, Integer arrangementTypeId, Integer ruleSetId) throws ConcurrentUpdateException;

    /**
     * Vytvoří nový uzel před předaným uzlem.
     *
     * @param node        object obsahující uzel a uzel(node) rodiče
     * @return              nový uzel
     */
    FLP addLevelBefore(FLP node);

    /**
     * Vytvoří nový uzel za předaným uzlem.
     *
     * @param node        object obsahující uzel a uzel(node) rodiče
     * @return              nový uzel
     */
    FLP addLevelAfter(FLP node);

    /**
     * Vytvoří nový uzel na poslední pozici pod předaným uzlem.
     *
     * @param node        object obsahující uzel a uzel(node) rodiče
     * @return            object obsahující nový uzel a uzel(node) rodiče s upravenou verzí
     */
    FLP addLevelChild(FLP node);

    /**
     * Přesune uzel před předaný uzel.
     *
     * @param node            uzel který se přesouvá s uzlem před který se přesouvá
     * @return                  přesunutý uzel
     */
    FLP moveLevelBefore(FLP node);

    /**
     * Přesune uzel na poslední pozici pod předaným uzlem.
     *
     * @param node       uzel který se přesouvá s uzlem pod který se přesouvá
     * @return             přesunutý uzel
     */
    FLP moveLevelUnder(FLP node);

    /**
     * Přesune uzel za předaný uzel.
     *
     * @param node            uzel který se přesouvá s uzlem za který se přesouvá
     * @return                   přesunutý uzel
     */
    FLP moveLevelAfter(FLP node);

    /**
     * Smaže uzel.
     *
     * @param node            uzel který se maže s uzlem rodiče
     * @return                  smazaný uzel
     */
    FLP deleteLevel(FLP node);

    /**
     * Načte neuzavřenou verzi archivní pomůcky.
     *
     * @param findingAidId      id archivní pomůcky
     * @return                  verze
     */
    ArrFindingAidVersion getOpenVersionByFindingAidId(Integer findingAidId);

    /**
     * Načte verzi podle identifikátoru.
     *
     * @param versionId      id verze
     * @return               verze s daným identifikátorem
     */
    ArrFindingAidVersion getFaVersionById(Integer versionId);

    /**
     * Načte potomky daného uzlu v konkrétní verzi. Pokud není identifikátor verze předaný načítají se potomci
     * z neuzavřené verze.
     *
     * @param nodeId          id rodiče
     * @param versionId       id verze, může být null
     * @param formatData      formátování dat, může být null - SHORT, FULL
     * @param descItemTypeIds typy atributů, může být null
     * @return            potomci předaného uzlu
     */
    List<? extends ArrLevelExt> findSubLevels(Integer nodeId, Integer versionId, String formatData, Integer[] descItemTypeIds);

    /**
     * Načte potomky daného uzlu v konkrétní verzi. Pokud není identifikátor verze předaný načítají se potomci
     * z neuzavřené verze.
     *
     * @param nodeId          id rodiče
     * @param versionId       id verze, může být null
     * @return            potomci předaného uzlu
     */
    List<? extends ArrLevel> findSubLevels(Integer nodeId, Integer versionId);

    /**
     * Načte uzel podle identifikátoru. K uzlu doplní jeho Atributy.
     *
     * @param nodeId           id uzlu
     * @param versionId        id verze, může být null
     * @param descItemTypeIds  typy atributů, může být null
     * @return uzel s daným identifikátorem
     */
    ArrLevelExt getLevel(Integer nodeId, Integer versionId, Integer[] descItemTypeIds);

    /**
     * Přidá atribut archivního popisu včetně hodnoty k existující jednotce archivního popisu.
     *
     * @param descItemExt       atribut archivního popisu k vytvoření
     * @param faVersionId       id verze
     * @return                  vytvořený atribut archivního popisu
     */
    DI createDescriptionItem(DI descItemExt, Integer faVersionId);

    /**
     * Upraví hodnotu existujícího atributu archivního popisu.
     *
     * @param descItemExt       atribut archivního popisu k upravení
     * @param faVersionId       id verze
     * @param createNewVersion  zda-li se má vytvářet nová verze
     * @return                  upravený atribut archivního popisu
     */
    DI updateDescriptionItem(DI descItemExt, Integer faVersionId, Boolean createNewVersion);

    /**
     * Vymaže atribut archivního popisu.
     *
     * @param descItemExt       atribut archivního popisu ke smazání
     * @return                  upravený(smazaný) atribut archivního popisu
     */
    DI deleteDescriptionItem(DI descItemExt);


    /**
     * Hromadně upraví archivní popis, resp. uloží hodnoty předaných atributů.
     *
     * @param descItemSavePack  object nesoucí předávané atributy archivního popisu k vytvoření/úpravě/smazání
     * @return                  upravené atributy archivního popisu
     */
    DIS saveDescriptionItems(DISP descItemSavePack);

    /**
     * Vrátí všechny hodnoty atributu archivního popisu k uzlu.
     * @param faVersionId       Identifikátor verze
     * @param nodeId            Identifikátor uzlu
     * @param rulDescItemTypeId Identifikátor typu atributu
     * @return  Seznam hodnot atrubutu
     */
    List<DI> getDescriptionItemsForAttribute(Integer faVersionId, Integer nodeId, Integer rulDescItemTypeId);

}
