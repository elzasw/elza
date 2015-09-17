package cz.tacr.elza.api.controller;

import cz.tacr.elza.api.RulArrangementType;
import cz.tacr.elza.api.RulDataType;
import cz.tacr.elza.api.RulDescItemSpec;
import cz.tacr.elza.api.RulDescItemType;
import cz.tacr.elza.api.RulFaView;
import cz.tacr.elza.api.RulRuleSet;
import cz.tacr.elza.api.vo.FaViewDescItemTypes;

import java.util.List;


/**
 * Rozhraní operací pro pravidla.
 *
 * @param <DT> {@link RulDataType} datový typ atribut arch. popisu
 * @param <DIT> {@link RulDescItemType} datový typ atributů arch. popisu
 * @param <DIS> {@link RulDescItemSpec} datový typ specifických atributů arch. popisu
 * @param <RFV> {@link RulFaView} nastavení zobrazení atributů archivního popisu pomůcky
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 12. 8. 2015
 */
public interface RuleManager<DT extends RulDataType, DIT extends RulDescItemType, DIS extends RulDescItemSpec, RFV extends RulFaView> {

    /**
     * Najde specifikaci podle id.
     *
     * @param descItemSpecId id specifikace
     * @return nalezená specifikace podle id nebo null, pokud není nalezena
     */
    DIS getDescItemSpecById(Integer descItemSpecId);

    /**
     * Vrátí všechny sady pravidel.
     *
     * @return všechny sady pravidel
     */
    List<? extends RulRuleSet> getRuleSets();

    /**
     * Vrátí všechny typy výstupu pro daná pravidla tvorby.
     *
     * @param ruleSetId id pravidel tvorby
     *
     * @return typy výstupu pro daná pravidla
     */
    List<? extends RulArrangementType> getArrangementTypes(Integer ruleSetId);

    /**
     * Vrátí všechny typy atributů archivního popisu k zadaným pravidlům tvorby.
     * @param ruleSetId     Identifikátor sady pravidel
     * @return  seznam typů hodnot atrubutů
     */
    List<? extends RulDescItemType> getDescriptionItemTypes(Integer ruleSetId);

    /**
     * Vrátí všechny typy hodnot atributů archivního popisu k uzlu.
     * @param faVersionId   Identifikátor verze
     * @param nodeId        Identifikátor uzlu
     * @param mandatory     true - vrací všechny povinné typy, false - vrací všechny nepovinné typy, null - vrací všechno
     * @return  Seznam typů hodnot atributů
     */
    List<? extends RulDescItemType> getDescriptionItemTypesForNodeId(Integer faVersionId, Integer nodeId, Boolean mandatory);

    /**
     * Vrací specifikace podle typu atributu.
     * @param rulDescItemType   Typ hodnoty atributu
     * @return  Seznam specifikací
     */
    List<DIS> getDescItemSpecsFortDescItemType(DIT rulDescItemType);

    /**
     * Vrací datový typ podle typu hodnoty atributu.
     * @param rulDescItemType   Typ hodnoty atributu
     * @return                  Datový typ
     */
    DT getDataTypeForDescItemType(DIT rulDescItemType);

    /**
     * Vrátí seznam identifikátorů typů atributů archivního popisu,
     * které se mají pro verzi archivní pomůcky zobrazovat v hierarchickém seznamu uzlů.
     * @param faVersionId id verze
     * @return Zapouzdření {@link RulFaView} a {@link FaViewDescItemTypes}.
     */
    FaViewDescItemTypes getFaViewDescItemTypes(Integer faVersionId);

    /**
     * Pro soubor pravidel a typ výstupu uloží seznam identifikátorů typů atributů archivního popisu,
     * které se mají zobrazovat v hierarchickém seznamu uzlů.
     * @param rulFaView nastavení zobrazení archivního popisu pro uložení
     * @param descItemTypeIds seznam typů atributů do {@link RulFaView#setViewSpecification(String)}.
     * @return descItemTypeIds
     */
    List<Integer> saveFaViewDescItemTypes(RFV rulFaView, Integer[] descItemTypeIds);
}
