package cz.tacr.elza.api;

import java.io.Serializable;



/**
 * Entita umožňující limitovat hodnoty typu atributu nebo podtypu.
 * 
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 20.8.2015
 */
public interface RulDescItemConstraint<RIT extends RulDescItemType, RIS extends RulDescItemSpec, AV extends ArrFundVersion,
        P extends RulPackage>
        extends
            Serializable {


    Integer getDescItemConstraintId();


    void setDescItemConstraintId(final Integer descItemConstraintId);


    /**
     * @return kód entity
     */
    String getCode();


    /**
     * @param code kód entity
     */
    void setCode(String code);


    RIT getDescItemType();


    void setDescItemType(final RIT descItemType);


    RIS getDescItemSpec();


    void setDescItemSpec(final RIS descItemSpec);


    AV getFundVersion();


    void setFundVersion(final AV fundVersion);


    /**
     * @return příznak, zda je možné atribut použít opakovaně v rámci jedné jednotky archivního
     *         popisu.
     */
    Boolean getRepeatable();


    /**
     * @param repeatable příznak, zda je možné atribut použít opakovaně v rámci jedné jednotky
     *        archivního popisu.
     */
    void setRepeatable(final Boolean repeatable);

    /**
     * @return regulární výraz, na který se při uložení atributu kontroluje jeho hodnota. regexp
     *         bude podporován pouze u některých datových typů jako je například číslo, text ...
     *         bude uvedeno v číselníku datových typů.
     */
    String getRegexp();

    /**
     * regulární výraz, na který se při uložení atributu kontroluje jeho hodnota. regexp bude
     * podporován pouze u některých datových typů jako je například číslo, text ... bude uvedeno v
     * číselníku datových typů.
     * 
     * @param regexp regulární výraz.
     */
    void setRegexp(final String regexp);

    /**
     * 
     * @return maximální možná délka textového řetězce hodnoty atributu, na kterou se při uložení
     *         atributu kontroluje jeho hodnota. limit délky bude podporován pouze u některých
     *         datových typů jako je například text ... bude uvedeno v číselníku datových typů.
     */
    Integer getTextLenghtLimit();

    /**
     * maximální možná délka textového řetězce hodnoty atributu, na kterou se při uložení atributu
     * kontroluje jeho hodnota. limit délky bude podporován pouze u některých datových typů jako je
     * například text ... bude uvedeno v číselníku datových typů.
     * 
     * @param textLenghtLimit maximální možná délka textového řetězce.
     */
    void setTextLenghtLimit(final Integer textLenghtLimit);


    /**
     * @return balíček
     */
    P getPackage();


    /**
     * @param rulPackage balíček
     */
    void setPackage(P rulPackage);

}
