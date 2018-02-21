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
     * @return vrací původní hodnotu položky
     */
    <T> T getValue(Class<T> type);

    /**
     * Formats value as string.
     *
     * @return Never null, for non-serializable item should be returned empty string.
     */
    String getSerializedValue();
}
