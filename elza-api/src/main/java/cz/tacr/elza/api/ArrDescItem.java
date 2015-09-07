package cz.tacr.elza.api;

import java.io.Serializable;



/**
 * Atribut archivního popisu evidovaný k jednotce archivního popisu. Odkaz na uzel stromu AP je
 * řešen pomocí node_id.
 * 
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 20.8.2015
 */
public interface ArrDescItem<FC extends ArrFaChange, RT extends RulDescItemType, RS extends RulDescItemSpec>
        extends
            Versionable,
            Serializable {


    /**
     * @return identifikátor hodnoty atributu, který se mění při každé verzované změně hodnoty.
     */
    public Integer getDescItemId();


    /**
     * Nastaví identifikátor hodnoty atributu, který se mění při každé verzované změně hodnoty.
     * 
     * @param descItemId
     */
    public void setDescItemId(final Integer descItemId);

    /**
     * @return Číslo změny založení hodnoty.
     */
    FC getCreateChange();


    /**
     * Nastaví číslo změny založení hodnoty.
     * 
     * @param createChange
     */
    void setCreateChange(final FC createChange);

    /**
     * @return Číslo změny smazání hodnoty.
     */
    FC getDeleteChange();


    void setDeleteChange(final FC deleteChange);

    /**
     * @return identifikátor hodnoty atrributu, který se nemění při verzované změně hodnoty.
     */
    Integer getDescItemObjectId();

    /**
     * Nastaví identifikátor hodnoty atrributu, který se nemění při verzované změně hodnoty.
     * 
     * @param descItemObjectId
     */
    void setDescItemObjectId(final Integer descItemObjectId);


    /**
     * 
     * @return Odkaz na typ atributu.
     */
    RT getDescItemType();

    /**
     * Nastaví odkaz na typ atributu.
     * 
     * @param descItemType
     */
    void setDescItemType(final RT descItemType);

    /**
     * @return Odkaz na podtyp atributu.
     */
    RS getDescItemSpec();

    /**
     * Nastaví odkaz na podtyp atributu.
     * 
     * @param descItemSpec
     */
    void setDescItemSpec(final RS descItemSpec);

    /**
     * 
     * @return ID nodu.
     */
    Integer getNodeId();

    /**
     * Nastaví ID nodu.
     * 
     * @param nodeId
     */
    void setNodeId(final Integer nodeId);

    /**
     * 
     * @return pořadí atributu v rámci shodného typu a specifikace atributu. U neopakovatelných
     *         atributů bude hodnota vždy 1, u opakovatelných dle skutečnosti.).
     */
    Integer getPosition();

    /**
     * Nastaví pořadí atributu v rámci shodného typu a specifikace atributu. U neopakovatelných
     * atributů bude hodnota vždy 1, u opakovatelných dle skutečnosti.).
     * 
     * @param position
     */
    void setPosition(final Integer position);

}
