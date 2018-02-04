package cz.tacr.elza.service;

import java.util.Collection;
import java.util.Set;
import java.util.function.Supplier;

import cz.tacr.elza.core.data.RuleSystem;
import cz.tacr.elza.core.data.RuleSystemItemType;
import cz.tacr.elza.domain.ArrChange;
import cz.tacr.elza.domain.ArrItem;
import cz.tacr.elza.domain.table.ElzaTable;

/**
 * Connector updates output items for single output definition.
 */
public interface OutputItemConnector {

    RuleSystem getRuleSystem();

    /**
     * Change supplier should be set before further processing.
     */
    void setChangeSupplier(Supplier<ArrChange> changeSupplier);

    /**
     * When filter is set only item with specified type is allowed to process (others are ignored).
     */
    void setItemTypeFilter(int allowedItemTypeId);

    void addIntItem(int value, RuleSystemItemType rsit, Integer itemSpecId);

    void addStringItem(String value, RuleSystemItemType rsit, Integer itemSpecId);

    void addTableItem(ElzaTable value, RuleSystemItemType rsit, Integer itemSpecId);

    void addItems(Collection<ArrItem> items, RuleSystemItemType rsit);

    /**
     * Returns item type ids which were modified by any call of addXXXItem() method.
     */
    Set<Integer> getModifiedItemTypeIds();
}
