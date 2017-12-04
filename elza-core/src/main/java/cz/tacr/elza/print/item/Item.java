package cz.tacr.elza.print.item;

/**
 * Rozhranní pro tiskový Item. Implementováno dle jednotlivých datových typů.
 *
 */
public interface Item extends Comparable<Item> {

    /**
     * @return typ item, odpovídá rul_item_type (+rul_data_type)
     */
    ItemType getType();

    /**
     * @return specifikace item, odpovídá rul_item_spec
     */
    ItemSpec getSpecification();

    /**
     * @return pozice item v seznamu
     */
    int getPosition();

    /**
     * @return je nedefinovaná hodnota?
     */
    boolean isUndefined();

    /**
     * @return vrací hodnotu formátovanou jako text k tisku
     */
    String getSerializedValue();

    /**
     * @return vrací popis položky + hodnotu formátovanou jako text k tisku
     */
    String getSerialized();

    /**
     * @return vrací původní hodnotu položky
     */
    <T> T getValue(Class<T> type);

    /**
     * Return if item is empty.
     *
     *  Empty items are not printed
     * @return Return true if item is empty (no value), return false if
     * item is not empty and should be printed.
     */
	boolean isEmpty();
}
