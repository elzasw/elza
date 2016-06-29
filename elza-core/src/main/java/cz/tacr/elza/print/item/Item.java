package cz.tacr.elza.print.item;

// TODO - JavaDoc - Lebeda

import cz.tacr.elza.domain.ArrItem;
import cz.tacr.elza.print.Node;
import cz.tacr.elza.print.Output;

/**
 * @author <a href="mailto:martin.lebeda@marbes.cz">Martin Lebeda</a>
 *         Date: 22.6.16
 */
public interface Item<T> {
    // TODO - JavaDoc - Lebeda
    ArrItem getArrItem();

    // TODO - JavaDoc - Lebeda
    Node getNode();

    // TODO - JavaDoc - Lebeda
    Output getOutput();

    // TODO - JavaDoc - Lebeda
    cz.tacr.elza.print.ItemType getType();

    // TODO - JavaDoc - Lebeda
    cz.tacr.elza.print.ItemSpec getSpecification();

    // TODO - JavaDoc - Lebeda
    Integer getPosition();

    // TODO - JavaDoc - Lebeda
    // řazených dle rul_desc_item.view_order + arr_item.position
    int compareToItemViewOrderPosition(Item o);

    // TODO - JavaDoc - Lebeda
    String serializeValue();

    // TODO - JavaDoc - Lebeda
    String getSerializedValue();

    // TODO - JavaDoc - Lebeda
    String serialize();

    // TODO - JavaDoc - Lebeda
    String getSerialized();

    // TODO - JavaDoc - Lebeda
    T getValue();
}
