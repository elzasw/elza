package cz.tacr.elza.api;

import java.io.Serializable;



/**
 * Atribut archivního popisu evidovaný k jednotce archivního popisu. Odkaz na uzel stromu AP je
 * řešen pomocí node_id.
 *
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 20.8.2015
 */
public interface ArrDescItem<FC extends ArrChange, RT extends RulItemType,
    RS extends RulItemSpec, N extends ArrNode> extends Serializable {

    /**
     * @return identifikátor hodnoty atributu, který se mění při každé verzované změně hodnoty.
     */
    Integer getDescItemId();


    /**
     * Nastaví identifikátor hodnoty atributu, který se mění při každé verzované změně hodnoty.
     *
     * @param descItemId identifikátor hodnoty atributu
     */
    void setDescItemId(Integer descItemId);

    /**
     * @return Číslo změny založení hodnoty.
     */
    FC getCreateChange();


    /**
     * Nastaví číslo změny založení hodnoty.
     *
     * @param createChange číslo změny založení hodnoty.
     */
    void setCreateChange(FC createChange);

    /**
     * @return Číslo změny smazání hodnoty.
     */
    FC getDeleteChange();


    void setDeleteChange(FC deleteChange);

    /**
     * @return identifikátor hodnoty atrributu, který se nemění při verzované změně hodnoty.
     */
    Integer getDescItemObjectId();

    /**
     * Nastaví identifikátor hodnoty atrributu, který se nemění při verzované změně hodnoty.
     *
     * @param descItemObjectId identifikátor hodnoty atrributu, který se nemění při verzované změně hodnoty.
     */
    void setDescItemObjectId(Integer descItemObjectId);


    /**
     *
     * @return Odkaz na typ atributu.
     */
    RT getItemType();

    /**
     * Nastaví odkaz na typ atributu.
     *
     * @param itemType odkaz na typ atributu.
     */
    void setItemType(RT itemType);

    /**
     * @return Odkaz na podtyp atributu.
     */
    RS getItemSpec();

    /**
     * Nastaví odkaz na podtyp atributu.
     *
     * @param itemSpec odkaz na podtyp atributu.
     */
    void setItemSpec(RS itemSpec);

    /**
     * @return nod.
     */
    N getNode();

    /**
     * Nastaví nod.
     *
     * @param node nod.
     */
    void setNode(N node);

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
     * @param position pořadí atributu v rámci shodného typu a specifikace atributu.
     */
    void setPosition(Integer position);

}
