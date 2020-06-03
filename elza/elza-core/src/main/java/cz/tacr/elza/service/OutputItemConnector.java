package cz.tacr.elza.service;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import cz.tacr.elza.core.data.ItemType;
import cz.tacr.elza.core.data.StructType;
import cz.tacr.elza.domain.ArrChange;
import cz.tacr.elza.domain.ArrItem;
import cz.tacr.elza.domain.ArrStructuredItem;
import cz.tacr.elza.domain.table.ElzaTable;

/**
 * Connector updates output items for single output definition.
 */
public interface OutputItemConnector {

    /**
     * Change supplier should be set before further processing.
     */
    void setChangeSupplier(Supplier<ArrChange> changeSupplier);

    /**
     * When filter is set only item with specified type is allowed to process (others are ignored).
     */
    void setItemTypeFilter(int allowedItemTypeId);

    void addIntItem(int value, ItemType rsit, Integer itemSpecId);

    void addStringItem(String value, ItemType rsit, Integer itemSpecId);

    void addTableItem(ElzaTable value, ItemType rsit, Integer itemSpecId);

    void addItems(Collection<? extends ArrItem> items, ItemType rsit);

    /**
     * Returns item type ids which were modified by any call of addXXXItem() method.
     */
    Set<Integer> getModifiedItemTypeIds();

    ItemType getItemTypeByCode(String itemType);

    StructType getStructuredTypeByCode(String outputType);

    void addStructuredItem(ItemType outputTypeApRef, StructType structuredType, List<ArrStructuredItem> dataItems);
}
