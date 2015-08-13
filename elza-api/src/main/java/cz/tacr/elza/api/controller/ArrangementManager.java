package cz.tacr.elza.api.controller;

import java.util.List;

import cz.tacr.elza.api.ArrangementType;
import cz.tacr.elza.api.FaLevel;
import cz.tacr.elza.api.FaVersion;
import cz.tacr.elza.api.FindingAid;

/**
 *
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 12. 8. 2015
 */
public interface ArrangementManager {

    /**
     * Vytvoří novou archivní pomůcku se zadaným názvem. Jako datum založení vyplní aktuální datum a čas.
     *
     * @param name              název archivní pomůcky
     * @param arrangementTypeId id typu výstupu
     * @param ruleSetId         id pravidel podle kterých se vytváří popis
     * @return nová archivní pomůcka
     */
    FindingAid createFindingAid(String name, Integer arrangementTypeId, Integer ruleSetId);

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
    List<? extends FindingAid> getFindingAids();

    /**
     * Aktualizace názvu archivní pomůcky.
     *
     * @param findingAidId id archivní pomůcky
     * @param name         název arhivní pomůcky
     * @return aktualizovaná archivní pomůcka
     */
    FindingAid updateFindingAid(Integer findingAidId, String name);

    /**
     * Vrátí všechny typy výstupu.
     *
     * @return všechny typy výstupu
     */
    List<? extends ArrangementType> getArrangementTypes();

    /**
     * Vrátí seznam verzí pro danou archivní pomůcku seřazený od nejnovější k nejstarší.
     *
     * @param findingAidId id archivní pomůcky
     * @return seznam verzí pro danou archivní pomůcku seřazený od nejnovější k nejstarší
     */
    List<? extends FaVersion> getFindingAidVersions(Integer findingAidId);

    /**
     * Vrátí archivní pomůcku.
     *
     * @param findingAidId id archivní pomůcky
     * @return archivní pomůcka
     */
    FindingAid getFindingAid(Integer findingAidId);

    /**
     * Schválí otevřenou verzi archivní pomůcky a otevře novou verzi.
     *
     * @param findingAidId id archivní pomůcky
     * @param arrangementTypeId id typu výstupu nové verze
     * @param ruleSetId         id pravidel podle kterých se vytváří popis v nové verzi
     * @return nová verze archivní pomůcky
     */
    FaVersion approveVersion(Integer findingAidId, Integer arrangementTypeId, Integer ruleSetId);

    /**
     * Vytvoří nový uzel v první úrovni archivní položky
     *
     * @param findingAidId    id archivní pomůcky
     * @return                nový záznam z archivný pomůcky
     */
    FaLevel addLevel(Integer findingAidId) ;

    /**
     * Vytvoří nový uzel za předaným uzlem.
     *
     * @param nodeId        id uzlu za kterým se má vytvořit nový
     * @return              nový uzel
     */
    FaLevel addLevelAfter(Integer nodeId);

    /**
     * Vytvoří nový uzel na poslední pozici pod předaným uzlem.
     *
     * @param nodeId        id uzlu pod kterým se má vytvořit nový
     * @return              nový uzel
     */
    FaLevel addLevelChild(Integer nodeId);

    /**
     * Přesune uzel na poslední pozici pod předaným uzlem.
     *
     * @param nodeId       id uzlu který se přesouvá
     * @param parentNodeId id uzlu pod který se má uzel přesunout
     * @return             přesunutý uzel
     */
    FaLevel moveLevelUnder(Integer nodeId, Integer parentNodeId);

    /**
     * Přesune uzel za předaný uzel.
     *
     * @param nodeId            id uzlu který se přesouvá
     * @param predecessorNodeId id uzlu za který se má uzel přesunout
     * @return                   přesunutý uzel
     */
    FaLevel moveLevelAfter(Integer nodeId, Integer predecessorNodeId);

    /**
     * Smaže uzel.
     *
     * @param nodeId            id uzlu který maže
     * @return                  smazaný uzel
     */
    FaLevel deleteLevel(Integer nodeId);

    /**
     * Načte uzel podle identifikátoru.
     *
     * @param nodeId            id uzlu
     * @return                  uzel s daným identifikátorem
     */
    FaLevel findLevelByNodeId(Integer nodeId);

    /**
     * Načte neuzavřenou verzi archivní pomůcky.
     *
     * @param findingAidId      id archivní pomůcky
     * @return                  verze
     */
    FaVersion getOpenVersionByFindingAidId(Integer findingAidId);

    /**
     * Načte verzi podle identifikátoru.
     *
     * @param versionId      id verze
     * @return               verze s daným identifikátorem
     */
    FaVersion getFaVersionById(Integer versionId);

    /**
     * Načte potomky daného uzlu v konkrétní verzi. Pokud není identifikátor verze předaný načítají se potomci
     * z neuzavřené verze.
     *
     * @param nodeId      id rodiče
     * @param versionId   id verze, může být null
     * @return            potomci předaného uzlu
     */
    List<? extends FaLevel> findSubLevels(Integer nodeId, Integer versionId);
}
