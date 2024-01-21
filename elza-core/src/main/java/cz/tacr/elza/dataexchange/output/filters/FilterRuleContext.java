package cz.tacr.elza.dataexchange.output.filters;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;

import cz.tacr.elza.core.data.ItemType;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.ArrItem;
import cz.tacr.elza.domain.RulItemSpec;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.codes.BaseCode;

/**
 * Context and parameters of FilterRule
 */
public class FilterRuleContext {

    List<ArrItem> addedRestrItem = new ArrayList<>();

    final Collection<? extends ArrItem> restrItems;

    public FilterRuleContext(final Collection<? extends ArrItem> soiItems) {
        this.restrItems = soiItems;
    }

    public boolean hasRestrItem(ItemType itemType, RulItemSpec itemSpec) {
        if (hasRestrItem(itemType, itemSpec, addedRestrItem)) {
            return true;
        }

        if (hasRestrItem(itemType, itemSpec, restrItems)) {
            return true;
        }

        return false;
    }

    private static boolean hasRestrItem(ItemType itemType, RulItemSpec itemSpec,
                                        Collection<? extends ArrItem> collection) {
        if (CollectionUtils.isNotEmpty(collection)) {
            for (ArrItem soiItem : collection) {
                if (itemType.getItemTypeId().equals(soiItem.getItemTypeId())) {
                    if (itemSpec != null) {
                        if (!itemSpec.getItemSpecId().equals(soiItem.getItemSpecId())) {
                            continue;
                        } else {
                            return true;
                        }
                    } else {
                        // item spec is null compare only by itemType
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static ArrData getFirstData(Collection<? extends ArrItem> items, ItemType itemType, RulItemSpec itemSpec) {
        if (items == null) {
            return null;
        }
        for (ArrItem item : items) {
            // compare over id            
            if (item.getItemTypeId() != null) {
                if (item.getItemTypeId().equals(itemType.getItemTypeId())) {
                    // compare spec
                    if (itemSpec != null) {
                        if (!itemSpec.getItemSpecId().equals(item.getItemSpecId())) {
                            // spec do not match -> skip to next item                            
                            continue;
                        }
                    }
                    return item.getData();
                }
            } else {
                throw new BusinessException("Missing itemTypeId", BaseCode.INVALID_STATE);
            }
        }
        return null;
    }

    public ArrData getFirstData(ItemType itemType, RulItemSpec itemSpec) {
        if (itemType == null) {
            return null;
        }
        ArrData result = getFirstData(addedRestrItem, itemType, itemSpec);
        if (result == null) {
            result = getFirstData(restrItems, itemType, itemSpec);
        }
        return result;
    }

    public void addRestrItem(ArrDescItem descItem) {
        addedRestrItem.add(descItem);
    }
}
