package cz.tacr.elza.api.controller;

import java.util.List;

import cz.tacr.elza.api.RulArrangementType;
import cz.tacr.elza.api.ArrDescItem;
import cz.tacr.elza.api.RulDataType;
import cz.tacr.elza.api.RulDescItemSpec;
import cz.tacr.elza.api.RulDescItemType;
import cz.tacr.elza.api.RulFaView;
import cz.tacr.elza.api.RulRuleSet;
import cz.tacr.elza.api.vo.FaViewDescItemTypes;


/**
 *
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 12. 8. 2015
 */
public interface RuleManager<DT extends RulDataType, DIT extends RulDescItemType, DIS extends RulDescItemSpec, RFV extends RulFaView> {

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
    List<? extends RulArrangementType> getArrangementTypes();

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
     * Vrátí všechny hodnoty attrubutu archivního popisu k uzlu.
     * @param faVersionId
     * @param nodeId
     * @param rulDescItemTypeId
     * @return
     */
    List<? extends ArrDescItem> getDescriptionItemsForAttribute(Integer faVersionId, Integer nodeId, Integer rulDescItemTypeId);


    /**
     * TODO
     * @param rulDescItemType
     * @return
     */
    List<DIS> getDescItemSpecsFortDescItemType(DIT rulDescItemType);


    /**
     * TODO
     * @param rulDescItemType
     * @return
     */
    DT getDataTypeForDescItemType(DIT rulDescItemType);


    /**
     * Vrátí seznam identifikátorů typů atributů archivního popisu,
     * které se mají pro verzi archivní pomůcky zobrazovat v hierarchickém seznamu uzlů.
     * @param faVersionId
     * @return
     */
    FaViewDescItemTypes getFaViewDescItemTypes(Integer faVersionId);

    /**
     * Pro soubor pravidel a typ výstupu uloží seznam identifikátorů typů atributů archivního popisu,
     *  které se mají zobrazovat v hierarchickém seznamu uzlů.
     * @param ruleSetId
     * @param arrangementTypeId
     * @param descItemTypeIds
     * @return
     */
    List<Integer> saveFaViewDescItemTypes(RFV rulFaView, Integer[] descItemTypeIds);
}
