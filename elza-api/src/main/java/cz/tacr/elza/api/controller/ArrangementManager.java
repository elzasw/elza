package cz.tacr.elza.api.controller;

import java.util.List;

import cz.tacr.elza.api.ArrArrangementType;
import cz.tacr.elza.api.ArrFaLevel;
import cz.tacr.elza.api.ArrFaVersion;
import cz.tacr.elza.api.ArrFindingAid;


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
     * Vrátí všechny typy výstupu.
     *
     * @return všechny typy výstupu
     */
    List<? extends ArrArrangementType> getArrangementTypes();

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
     * @param nodeId      id rodiče
     * @param versionId   id verze, může být null
     * @return            potomci předaného uzlu
     */
    List<? extends ArrFaLevel> findSubLevels(Integer nodeId, Integer versionId);
}
