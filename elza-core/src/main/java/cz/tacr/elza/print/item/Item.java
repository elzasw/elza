package cz.tacr.elza.print.item;

import cz.tacr.elza.print.NodeId;

/**
 * Rozhranní pro tiskový Item. Implementováno dle jednotlivých datových typů.
 *
 */
public interface Item {

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
    Integer getPosition();

    /**
     * @return je nedefinovaná hodnota?
     */
    Boolean getUndefined();

    /**
     * porovnání pro řazení dle rul_item_type.view_order + arr_item.position
     */
    int compareToItemViewOrderPosition(Item o);

    /**
     * @return vrací hodnotu formátovanou jako text k tisku
     */
    String serializeValue();

    /**
     * @return vrací hodnotu formátovanou jako text k tisku - pro použití jako field serializedValue v jasperu
     */
    String getSerializedValue();

    /**
     * @return vrací popis položky + hodnotu formátovanou jako text k tisku
     */
    String serialize();

    /**
     * @return vrací popis položky + hodnotu formátovanou jako text k tisku - pro použití jako field serializedValue v jasperu
     */
    String getSerialized();

    /**
     * @return vrací původní hodnotu položky
     */
    <T> T getValue(Class<T> type);

    /**
     * Metoda pro získání hodnoty do fieldu v Jasper.
     * Umožní na položce v detailu volat metody sám nad sebou (nejen implicitně zpřístupněné gettery).
     *
     * @return odkaz sám na sebe
     */
    Item getItem();

    /**
     * Return if item is empty.
     * 
     *  Empty items are not printed
     * @return Return true if item is empty (no value), return false if 
     * item is not empty and should be printed.
     */
	boolean isEmpty();
}
