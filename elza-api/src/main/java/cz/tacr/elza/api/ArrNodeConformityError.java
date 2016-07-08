package cz.tacr.elza.api;

/**
 * Pro chybové stavy uzlu {@link ArrNodeConformity} se odkazuje na hodnoty atributů, které jsou ve špatném stavu.
 *
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 19.11.2015
 */
public interface ArrNodeConformityError<ANCI extends ArrNodeConformity, ADI extends ArrDescItem, PT extends RulPolicyType> {

    /**
     * @return id chyby
     */
    Integer getNodeConformityErrorId();


    /**
     * @param nodeConformityErrorId id chyby
     */
    void setNodeConformityErrorId(Integer nodeConformityErrorId);


    /**
     * @return stav uzlu
     */
    ANCI getNodeConformity();


    /**
     * @param nodeConformity stav uzlu
     */
    void setNodeConformity(ANCI nodeConformity);


    /**
     * @return chybná hodnota atributu
     */
    ADI getDescItem();


    /**
     * @param descItem chybná hodnota atributu
     */
    void setDescItem(ADI descItem);


    /**
     * @return textový popis chyby
     */
    String getDescription();


    /**
     * @param description textový popis chyby
     */
    void setDescription(String description);

    /**
     * @return typy kontrol, validací, archivního popisu
     */
    PT getPolicyType();

    /**
     * @param policyType typy kontrol, validací, archivního popisu
     */
    void setPolicyType(PT policyType);
}
