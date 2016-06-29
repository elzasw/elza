package cz.tacr.elza.api;

/**
 * Vazba: Hromadná akce, která počítá hodnotu atributu výstupu.
 *
 * @author Martin Šlapa
 * @since 27.06.2016
 */
public interface RulItemTypeAction<A extends RulAction, IT extends RulItemType> {

    /**
     * @return identifikátor entity
     */
    Integer getItemTypeActionId();

    /**
     * @param itemTypeActionId identifikátor entity
     */
    void setItemTypeActionId(Integer itemTypeActionId);

    /**
     * @return hromadná akce
     */
    A getAction();

    /**
     * @param action hromadná akce
     */
    void setAction(A action);

    /**
     * @return typ atributu
     */
    IT getItemType();

    /**
     * @param itemType typ atributu
     */
    void setItemType(IT itemType);
}
