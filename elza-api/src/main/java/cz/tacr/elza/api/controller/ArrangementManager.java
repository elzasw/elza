package cz.tacr.elza.api.controller;

import java.util.List;

import cz.tacr.elza.api.ArrDescItemExt;
import cz.tacr.elza.api.ArrFaLevel;
import cz.tacr.elza.api.ArrFaLevelExt;
import cz.tacr.elza.api.ArrFaVersion;
import cz.tacr.elza.api.ArrFindingAid;
import cz.tacr.elza.api.exception.ConcurrentUpdateException;
import cz.tacr.elza.api.vo.ArrDescItemSavePack;


/**
 *
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 12. 8. 2015
 */
public interface ArrangementManager<FA extends ArrFindingAid, FV extends ArrFaVersion, DIE extends ArrDescItemExt, DISP extends ArrDescItemSavePack, FL extends ArrFaLevel> {
    public static final String FORMAT_ATTRIBUTE_FULL = "FULL";
    public static final String FORMAT_ATTRIBUTE_SHORT = "SHORT";

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
     * @param findingAidId id archivní pomůcky
     * @param name         název arhivní pomůcky
     * @return aktualizovaná archivní pomůcka
     * @throws ConcurrentUpdateException chyba při současné manipulaci s položkou více uživateli
     */
    ArrFindingAid updateFindingAid(FA findingAid) throws ConcurrentUpdateException;

    /**
     * Vrátí seznam verzí pro danou archivní pomůcku seřazený od nejnovější k nejstarší.
     *
     * @param findingAidId id archivní pomůcky
     * @return seznam verzí pro danou archivní pomůcku seřazený od nejnovější k nejstarší
     */
    List<? extends ArrFaVersion> getFindingAidVersions(Integer findingAidId);

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
    ArrFaVersion approveVersion(FV version, Integer arrangementTypeId, Integer ruleSetId) throws ConcurrentUpdateException;

    /**
     * Vytvoří nový uzel před předaným uzlem.
     *
     * @param node        uzel před kterým se má vytvořit nový
     * @return              nový uzel
     */
    ArrFaLevel addLevelBefore(FL node);

    /**
     * Vytvoří nový uzel za předaným uzlem.
     *
     * @param node        uzel za kterým se má vytvořit nový
     * @return              nový uzel
     */
    ArrFaLevel addLevelAfter(FL node);

    /**
     * Vytvoří nový uzel na poslední pozici pod předaným uzlem.
     *
     * @param node        uzel pod kterým se má vytvořit nový
     * @return            nový uzel
     */
    ArrFaLevel addLevelChild(FL node);

    /**
     * Přesune uzel před předaný uzel.
     *
     * @param node            uzel který se přesouvá
     * @param followerNode    id uzlu před který se má uzel přesunout
     * @return                  přesunutý uzel
     */
    ArrFaLevel moveLevelBefore(FL node, Integer followerNodeId);

    /**
     * Přesune uzel na poslední pozici pod předaným uzlem.
     *
     * @param node       uzel který se přesouvá
     * @param parentNode id uzlu pod který se má uzel přesunout
     * @return             přesunutý uzel
     */
    ArrFaLevel moveLevelUnder(FL node, Integer parentNodeId);

    /**
     * Přesune uzel za předaný uzel.
     *
     * @param node            uzel který se přesouvá
     * @param predecessorNode id uzlu za který se má uzel přesunout
     * @return                   přesunutý uzel
     */
    ArrFaLevel moveLevelAfter(FL node, Integer predecessorNodeId);

    /**
     * Smaže uzel.
     *
     * @param nodeId            id uzlu který maže
     * @return                  smazaný uzel
     */
    ArrFaLevel deleteLevel(Integer nodeId);

    /**
     * Načte uzel podle identifikátoru.
     *
     * @param nodeId            id uzlu
     * @param versionId         id verze, může být null
     * @return                  uzel s daným identifikátorem
     */
    ArrFaLevel findLevelByNodeId(Integer nodeId, Integer versionId);

    /**
     * Načte neuzavřenou verzi archivní pomůcky.
     *
     * @param findingAidId      id archivní pomůcky
     * @return                  verze
     */
    ArrFaVersion getOpenVersionByFindingAidId(Integer findingAidId);

    /**
     * Načte verzi podle identifikátoru.
     *
     * @param versionId      id verze
     * @return               verze s daným identifikátorem
     */
    ArrFaVersion getFaVersionById(Integer versionId);

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
    List<? extends ArrFaLevelExt> findSubLevels(Integer nodeId, Integer versionId, String formatData, Integer[] descItemTypeIds);

    /**
     * Načte potomky daného uzlu v konkrétní verzi. Pokud není identifikátor verze předaný načítají se potomci
     * z neuzavřené verze.
     *
     * @param nodeId          id rodiče
     * @param versionId       id verze, může být null
     * @return            potomci předaného uzlu
     */
    List<? extends ArrFaLevel> findSubLevels(Integer nodeId, Integer versionId);

    /**
     * Načte uzel podle identifikátoru. K uzlu doplní jeho Atributy.
     *
     * @param nodeId           id uzlu
     * @param versionId        id verze, může být null
     * @param descItemTypeIds  typy atributů, může být null
     * @return uzel s daným identifikátorem
     */
    ArrFaLevelExt getLevel(Integer nodeId, Integer versionId, Integer[] descItemTypeIds);

    /**
     * Přidá atribut archivního popisu včetně hodnoty k existující jednotce archivního popisu.
     *
     * @param descItemExt       atribut archivního popisu k vytvoření
     * @param faVersionId       id verze
     * @return                  vytvořený atribut archivního popisu
     */
    DIE createDescriptionItem(DIE descItemExt, Integer faVersionId);

    /**
     * Upraví hodnotu existujícího atributu archivního popisu.
     *
     * @param descItemExt       atribut archivního popisu k upravení
     * @param faVersionId       id verze
     * @param createNewVersion  zda-li se má vytvářet nová verze
     * @return                  upravený atribut archivního popisu
     */
    DIE updateDescriptionItem(DIE descItemExt, Integer faVersionId, Boolean createNewVersion);

    /**
     * Vymaže atribut archivního popisu.
     *
     * @param descItemObjectId  id atributu archivního popisu ke smazání
     * @return                  upravený(smazaný) atribut archivního popisu
     */
    DIE deleteDescriptionItem(Integer descItemObjectId);


    /**
     * Hromadně upraví archivní popis, resp. uloží hodnoty předaných atributů.
     *
     * @param descItemSavePack  object nesoucí předávané atributy archivního popisu k vytvoření/úpravě/smazání
     * @return                  upravené atributy archivního popisu
     */
    List<DIE> saveDescriptionItems(DISP descItemSavePack);

}
