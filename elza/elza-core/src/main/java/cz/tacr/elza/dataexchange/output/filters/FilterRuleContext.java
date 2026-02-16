package cz.tacr.elza.dataexchange.output.filters;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.collections4.CollectionUtils;

import cz.tacr.elza.core.data.ItemType;
import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.ArrItem;
import cz.tacr.elza.domain.RulItemSpec;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.service.cache.AccessPointCacheProvider;
import jakarta.annotation.Nonnull;

/**
 * Context and parameters of FilterRule
 * 
 * Context is created as new instance for each processed level.
 */
public class FilterRuleContext {

    List<ArrItem> addedRestrItem = new ArrayList<>();

    final Collection<? extends ArrItem> restrItems;
    
    /**
     * List of matching items
     */
    List<? extends ArrItem> matchingItems = Collections.emptyList();

	private final AccessPointCacheProvider apcProvider;

	private StaticDataProvider staticDataProvider;
	
    public FilterRuleContext(final Collection<? extends ArrItem> soiItems, 
    		final AccessPointCacheProvider apcProvider, final StaticDataProvider staticDataProvider) {
        this.restrItems = soiItems;
        this.apcProvider = apcProvider;
        this.staticDataProvider = staticDataProvider;
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

    private List<ArrItem> getItems(Collection<? extends ArrItem> items, ItemType itemType, RulItemSpec itemSpec) {
        if (items == null) {
            return null;
        }
        
        List<ArrItem> result = null;
        
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
                    if(result==null) {
                    	result = new ArrayList<>();
                    }
                    result.add(item);
                }
            } else {
                throw new BusinessException("Missing itemTypeId", BaseCode.INVALID_STATE);
            }
        }
        return result;
    }
    
    /**
     * Return all matching items
     * 
     * 
     * @param itemType
     * @param itemSpec
     * @return Return empty list if no matching items
     */
    public @Nonnull List<ArrItem> getItems(ItemType itemType, RulItemSpec itemSpec) {
        if (itemType == null) {
            return Collections.emptyList();
        }
        List<ArrItem> results1 = getItems(addedRestrItem, itemType, itemSpec);
        List<ArrItem> results2 = getItems(restrItems, itemType, itemSpec);
        if(results1==null) {
        	return (results2==null)?Collections.emptyList():results2;
        } else {
        	if(results2!=null) {
        		// combine results
        		results1.addAll(results2);
        	}        	
        	return results1;
        }
    }

    public void addRestrItem(ArrDescItem descItem) {
        addedRestrItem.add(descItem);
    }

	public AccessPointCacheProvider getApCacheProvider() {
		return apcProvider;
	}
	
	public StaticDataProvider getStaticDataProvider() {
		return staticDataProvider;
	}

	public void resetMatchedItems() {
		matchingItems = Collections.emptyList();		
	}

	/**
	 * Store matching items
	 * 
	 * @param matchedItems
	 */
	public void setMatchedItems(List<ArrItem> matchedItems) {
		matchingItems = matchedItems;		
	}
	
	List<? extends ArrItem> getMatchingItems() {
		return matchingItems;
	}
}
