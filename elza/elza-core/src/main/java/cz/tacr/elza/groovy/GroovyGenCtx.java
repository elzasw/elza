package cz.tacr.elza.groovy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import jakarta.annotation.Nullable;

public class GroovyGenCtx {

	private final GroovyFund fund;

	private final List<GroovyItem> items;

	/**
	 * Items of the parent levels ordered from the nearest parent up to the root.
	 */
	private final List<List<GroovyItem>> parentItems;

	public GroovyGenCtx(GroovyFund fund, GroovyAe groovyAe, List<GroovyItem> items) {
		this(fund, groovyAe, items, Collections.emptyList());
	}

	public GroovyGenCtx(GroovyFund fund, GroovyAe groovyAe, List<GroovyItem> items, List<List<GroovyItem>> parentItems) {
		this.fund = fund;
		this.items = items;
		this.parentItems = parentItems;
	}

	public GroovyFund getFund() {
		return fund;
	}

	public GroovyAe getGroovyAe() {
		return fund.getInstitution().getGroovyAe();
	}

	public String getInstitutionCode() {
		return fund.getInstitution().getInternalCode();
	}

	public List<GroovyItem> getItems() {
		return items;
	}

	public List<GroovyItem> getItemsByItemType(String itemType) {
    	List<GroovyItem> result = new ArrayList<>();
    	for (GroovyItem item : getItems()) {
    		if (item.getTypeCode().equals(itemType)) {
    			result.add(item);
    		}
    	}
    	return result;
	}

	@Nullable
	public GroovyItem getFirstItemByItemType(String itemType) {
    	for (GroovyItem item : getItems()) {
    		if (item.getTypeCode().equals(itemType)) {
    			return item;
    		}
    	}
    	return null;
	}

	/**
	 * Find the first item of the given type with a non-empty value on the nearest
	 * parent level that has it. Parent levels are searched from the nearest parent
	 * up to the root.
	 */
	@Nullable
	public GroovyItem getFirstParentItemByItemType(String itemType) {
		for (List<GroovyItem> levelItems : parentItems) {
			for (GroovyItem item : levelItems) {
				if (item.getTypeCode().equals(itemType) && StringUtils.isNotEmpty(item.getValue())) {
					return item;
				}
			}
		}
		return null;
	}
}