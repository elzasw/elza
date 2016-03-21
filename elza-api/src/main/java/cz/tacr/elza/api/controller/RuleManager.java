package cz.tacr.elza.api.controller;

import java.io.File;
import java.util.List;
import java.util.Set;

import cz.tacr.elza.api.ArrDescItem;
import cz.tacr.elza.api.ArrFundVersion;
import cz.tacr.elza.api.ArrVersionConformity;
import cz.tacr.elza.api.RulArrangementType;
import cz.tacr.elza.api.RulDataType;
import cz.tacr.elza.api.RulDescItemSpec;
import cz.tacr.elza.api.RulDescItemType;
import cz.tacr.elza.api.RulPackage;
import cz.tacr.elza.api.RulRuleSet;
import cz.tacr.elza.api.vo.NodeTypeOperation;
import cz.tacr.elza.api.vo.RelatedNodeDirection;


/**
 * Rozhraní operací pro pravidla.
 *
 * @param <DT> {@link RulDataType} datový typ atribut arch. popisu
 * @param <DIT> {@link RulDescItemType} datový typ atributů arch. popisu
 * @param <DIS> {@link RulDescItemSpec} datový typ specifických atributů arch. popisu
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 12. 8. 2015
 */
public interface RuleManager<DT extends RulDataType, DIT extends RulDescItemType, DIS extends RulDescItemSpec,
        NTO extends NodeTypeOperation, RND extends RelatedNodeDirection, DI extends ArrDescItem,
        FAV extends ArrFundVersion, FAVCI extends ArrVersionConformity, P extends RulPackage> {

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
     * @param fundVersionId   Identifikátor verze
     * @param nodeId        Identifikátor uzlu
     * @return  Seznam typů hodnot atributů
     */
    List<? extends RulDescItemType> getDescriptionItemTypesForNode(Integer fundVersionId,
                                                                   Integer nodeId,
                                                                   Set<String> strategies);

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
     * Vrací seznam naimportovaných balíčlů.
     * @return seznam balíčlů
     */
    List<P> getPackages();


    /**
     * Provede import balíčku s konfigurací.
     * @param file soubor balíčku
     */
    void importPackage(File file);


    /**
     * Smaže balíček s konfigurací
     * @param code kód balíčku
     */
    void deletePackage(String code);


    /**
     * Provede export balíčku s konfigurací.
     *
     * @param code kód balíčku
     */
    File exportPackage(String code);
}
