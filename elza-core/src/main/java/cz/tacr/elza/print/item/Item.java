package cz.tacr.elza.print.item;

import cz.tacr.elza.domain.ArrItem;
import cz.tacr.elza.print.ItemSpec;
import cz.tacr.elza.print.ItemType;
import cz.tacr.elza.print.Node;
import cz.tacr.elza.print.Output;

/**
 * Rozhranní pro tiskový Item. Implementováno dle jednotlivých datových typů.
 *
 * @author <a href="mailto:martin.lebeda@marbes.cz">Martin Lebeda</a>
 *         Date: 22.6.16
 */
public interface Item<T> {

    /**
     * @return původní uložený item, ze kterého tiskový objekt vychází
     */
    ArrItem getArrItem();

    /**
     * @return node na který je item navázán, pokud je null, jde o navázání přímo na output
     */
    Node getNode();

    /**
     * @return output na který je item navázán
     */
    Output getOutput();

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
}
