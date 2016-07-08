package cz.tacr.elza.api.controller;

import java.io.File;
import java.util.List;

import cz.tacr.elza.api.*;
import cz.tacr.elza.api.RulItemSpec;
import cz.tacr.elza.api.vo.NodeTypeOperation;
import cz.tacr.elza.api.vo.RelatedNodeDirection;


/**
 * Rozhraní operací pro pravidla.
 *
 * @param <DT> {@link RulDataType} datový typ atribut arch. popisu
 * @param <DIT> {@link RulItemType} datový typ atributů arch. popisu
 * @param <DIS> {@link RulItemSpec} datový typ specifických atributů arch. popisu
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 12. 8. 2015
 */
public interface RuleManager<DT extends RulDataType, DIT extends RulItemType, DIS extends RulItemSpec,
        NTO extends NodeTypeOperation, RND extends RelatedNodeDirection, DI extends ArrDescItem,
        FAV extends ArrFundVersion, P extends RulPackage> {

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
     * Vrátí všechny typy atributů archivního popisu k zadaným pravidlům tvorby.
     * @param ruleSetId     Identifikátor sady pravidel
     * @return  seznam typů hodnot atrubutů
     */
    List<? extends RulItemType> getDescriptionItemTypes(Integer ruleSetId);

    /**
     * Vrátí všechny typy hodnot atributů archivního popisu k uzlu.
     * @param fundVersionId   Identifikátor verze
     * @param nodeId        Identifikátor uzlu
     * @return  Seznam typů hodnot atributů
     */
    List<? extends RulItemType> getDescriptionItemTypesForNode(Integer fundVersionId,
                                                               Integer nodeId);

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
