package cz.tacr.elza.api.controller;

import java.util.List;

import cz.tacr.elza.api.ArrArrangementType;
import cz.tacr.elza.api.RulDescItemType;
import cz.tacr.elza.api.RulRuleSet;


/**
 *
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 12. 8. 2015
 */
public interface RuleManager {

    /**
     * Vrátí všechny sady pravidel.
     *
     * @return všechny sady pravidel
     */
    List<? extends RulRuleSet> getRuleSets();


    /**
     * Vrátí všechny typy výstupu.
     *
     * @return všechny typy výstupu
     */
    List<? extends ArrArrangementType> getArrangementTypes();

    /**
     * Vrátí všechny typy atributů archivního popisu k zadaným pravidlům tvorby.
     * @param ruleSetId
     * @return
     */
    List<? extends RulDescItemType> getDescriptionItemTypes(Integer ruleSetId);

    /**
     * Vrátí všechny typy atributů archivního popisu k uzlu.
     * @param faVersionId
     * @param nodeId
     * @param mandatory
     * @return
     */
    List<? extends RulDescItemType> getDescriptionItemTypesForNodeId(Integer faVersionId, Integer nodeId, Boolean mandatory);

    /**
     * Vrátí seznam identifikátorů typů atributů archivního popisu,
     * které se mají pro verzi archivní pomůcky zobrazovat v hierarchickém seznamu uzlů.
     * @param faVersionId
     * @return
     */
    List<Integer> getFaViewDescItemTypes(Integer faVersionId);

    /**
     * Pro soubor pravidel a typ výstupu uloží seznam identifikátorů typů atributů archivního popisu,
     *  které se mají zobrazovat v hierarchickém seznamu uzlů.
     * @param ruleSetId
     * @param arrangementTypeId
     * @param descItemTypeIds
     * @return
     */
    List<Integer> saveFaViewDescItemTypes(Integer ruleSetId, Integer arrangementTypeId, Integer[] descItemTypeIds);
}
