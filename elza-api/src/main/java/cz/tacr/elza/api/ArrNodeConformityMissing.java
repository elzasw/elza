package cz.tacr.elza.api;

/**
 * Textový popis chyby {@link ArrNodeConformity}
 *
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 19.11.2015
 */
public interface ArrNodeConformityMissing<ANCI extends ArrNodeConformity,
        RDIT extends RulItemType, RDIS extends RulItemSpec, PT extends RulPolicyType> {

    /**
     * @return id textového popisu
     */
    Integer getNodeConformityMissingId();


    /**
     * @param nodeConformityMissingId id textového popisu
     */
    void setNodeConformityMissingId(Integer nodeConformityMissingId);


    /**
     * @return stav uzlu
     */
    ANCI getNodeConformity();


    /**
     * @param nodeConformityInfo stav uzlu
     */
    void setNodeConformity(ANCI nodeConformityInfo);


    /**
     * @return typ atributu
     */
    RDIT getItemType();


    /**
     * @param itemType typ atributu
     */
    void setItemType(RDIT itemType);


    /**
     * @return specifikace typu atributu
     */
    RDIS getItemSpec();


    /**
     * @param itemSpec specifikace typu atributu
     */
    void setItemSpec(RDIS itemSpec);


    /**
     * @return Textový popis chyby
     */
    String getDescription();


    /**
     * @param description Textový popis chyby
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
