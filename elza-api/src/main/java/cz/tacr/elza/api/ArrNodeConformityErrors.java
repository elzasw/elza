package cz.tacr.elza.api;

/**
 * Pro chybové stavy uzlu {@link ArrNodeConformityInfo} se odkazuje na hodnoty atributů, které jsou ve špatném stavu.
 *
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 19.11.2015
 */
public interface ArrNodeConformityErrors<ANCI extends ArrNodeConformityInfo, ADI extends ArrDescItem> {

    /**
     * @return id chyby
     */
    Integer getNodeConformityErrorsId();


    /**
     * @param nodeConformityErrorsId id chyby
     */
    void setNodeConformityErrorsId(Integer nodeConformityErrorsId);


    /**
     * @return stav uzlu
     */
    ANCI getNodeConformityInfo();


    /**
     * @param nodeConformityInfo stav uzlu
     */
    void setNodeConformityInfo(ANCI nodeConformityInfo);


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
}
