package cz.tacr.elza.api;

import java.io.Serializable;

/**
 * Nadřízená položka.
 *
 * @author Martin Šlapa
 * @since 17.06.2016
 */
public interface ArrItem<C extends ArrChange, RT extends RulItemType,
        RS extends RulItemSpec> extends Serializable {


    Integer getItemId();

    void setItemId(Integer itemId);

    C getCreateChange();

    void setCreateChange(C createChange);

    C getDeleteChange();

    void setDeleteChange(C deleteChange);

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
}
