package cz.tacr.elza.drools.model;

import java.util.ArrayList;
import java.util.List;

import cz.tacr.elza.core.data.ItemType;
import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.domain.RulItemSpec;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.codes.BaseCode;


/**
 * Popis scénáře založení nové JP.
 * 
 * Objekt je výsledkem volání pravidel.
 *
 * @since 9.12.2015
 */
public class NewLevelApproach {

    /**
     * jméno scénáře
     */
    private final String name;

    /**
     * seznam hodnot atrubutů
     */
    private final List<DescItem> descItems = new ArrayList<>();

	private final StaticDataProvider staticDataProvider;

    public NewLevelApproach(final String name, StaticDataProvider staticDataProvider) {
        this.name = name;
        this.staticDataProvider = staticDataProvider;
    }

    public DescItem addDescItem(final String type, final String spec) {
    	// try to find item type
		ItemType itemType = staticDataProvider.getItemTypeByCode(type);
		RulItemSpec specType = null;
		if(itemType==null) {
			throw new BusinessException("Item type not found", BaseCode.ID_NOT_EXIST).set("type", type);
		}
		if(itemType.hasSpecifications()) {
			// check specification
			if(spec==null) {
				throw new BusinessException("Item type requires specification", BaseCode.INVALID_STATE).set("type", type);
			}
			specType = itemType.getItemSpecByCode(spec);
			if(specType==null) {
				throw new BusinessException("Invalid specification for the type.", BaseCode.ID_NOT_EXIST)
					.set("type", type)
					.set("spec", spec);
			}
		} else {
			if(spec!=null) {
				throw new BusinessException("Item type has no specification", BaseCode.INVALID_STATE).set("type", type);
			}
		}
		
        DescItem descItem = new DescItem(itemType.getEntity(), specType);
        descItems.add(descItem);
        return descItem;
    }

    public void addDescItem(final DescItem descItem) {
        descItems.add(descItem);
    }

    public String getName() {
        return name;
    }

    public List<DescItem> getDescItems() {
        return descItems;
    }
}
