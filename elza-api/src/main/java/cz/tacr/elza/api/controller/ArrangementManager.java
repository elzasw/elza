package cz.tacr.elza.api.controller;

import java.util.List;

import cz.tacr.elza.api.ArrDescItemExt;
import cz.tacr.elza.api.ArrFaLevel;
import cz.tacr.elza.api.ArrFaLevelExt;
import cz.tacr.elza.api.ArrFaVersion;
import cz.tacr.elza.api.ArrFindingAid;
import cz.tacr.elza.api.vo.ArrDescItemSavePack;


/**
 *
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 12. 8. 2015
 */
public interface ArrangementManager<DIE extends ArrDescItemExt, DISP extends ArrDescItemSavePack> {
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
     */
    ArrFindingAid updateFindingAid(Integer findingAidId, String name);

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
     * Schválí otevřenou verzi archivní pomůcky a otevře novou verzi.
     *
     * @param findingAidId id archivní pomůcky
     * @param arrangementTypeId id typu výstupu nové verze
     * @param ruleSetId         id pravidel podle kterých se vytváří popis v nové verzi
     * @return nová verze archivní pomůcky
     */
    ArrFaVersion approveVersion(Integer findingAidId, Integer arrangementTypeId, Integer ruleSetId);

    /**
     * Vytvoří nový uzel před předaným uzlem.
     *
     * @param nodeId        id uzlu před kterým se má vytvořit nový
     * @return              nový uzel
     */
    ArrFaLevel addLevelBefore(Integer nodeId);

    /**
     * Vytvoří nový uzel v první úrovni archivní položky
     *
     * @param findingAidId    id archivní pomůcky
     * @return                nový záznam z archivný pomůcky
     */
    ArrFaLevel addLevel(Integer findingAidId) ;

    /**
     * Vytvoří nový uzel za předaným uzlem.
     *
     * @param nodeId        id uzlu za kterým se má vytvořit nový
     * @return              nový uzel
     */
    ArrFaLevel addLevelAfter(Integer nodeId);

    /**
     * Vytvoří nový uzel na poslední pozici pod předaným uzlem.
     *
     * @param nodeId        id uzlu pod kterým se má vytvořit nový
     * @return              nový uzel
     */
    ArrFaLevel addLevelChild(Integer nodeId);

    /**
     * Přesune uzel před předaný uzel.
     *
     * @param nodeId            id uzlu který se přesouvá
     * @param followerNodeId    id uzlu před který se má uzel přesunout
     * @return                  přesunutý uzel
     */
    ArrFaLevel moveLevelBefore(Integer nodeId, Integer followerNodeId);

    /**
     * Přesune uzel na poslední pozici pod předaným uzlem.
     *
     * @param nodeId       id uzlu který se přesouvá
     * @param parentNodeId id uzlu pod který se má uzel přesunout
     * @return             přesunutý uzel
     */
    ArrFaLevel moveLevelUnder(Integer nodeId, Integer parentNodeId);

    /**
     * Přesune uzel za předaný uzel.
     *
     * @param nodeId            id uzlu který se přesouvá
     * @param predecessorNodeId id uzlu za který se má uzel přesunout
     * @return                   přesunutý uzel
     */
    ArrFaLevel moveLevelAfter(Integer nodeId, Integer predecessorNodeId);

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
     * @return                  uzel s daným identifikátorem
     */
    ArrFaLevel findLevelByNodeId(Integer nodeId);

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
