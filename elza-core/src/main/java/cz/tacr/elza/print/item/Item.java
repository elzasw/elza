package cz.tacr.elza.print.item;

import cz.tacr.elza.print.NodeId;

/**
 * Rozhranní pro tiskový Item. Implementováno dle jednotlivých datových typů.
 *
 * @author <a href="mailto:martin.lebeda@marbes.cz">Martin Lebeda</a>
 *         Date: 22.6.16
 */
public interface Item<T> {

    /**
     * @return node na který je item navázán, pokud je null, jde o navázání přímo na output
     */
    NodeId getNodeId();

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
    T getValue();

    /**
     * Metoda pro získání hodnoty do fieldu v Jasper.
     * Umožní na položce v detailu volat metody sám nad sebou (nejen implicitně zpřístupněné gettery).
     *
     * @return odkaz sám na sebe
     */
    Item<T> getItem();
}
