package cz.tacr.elza.groovy;

import java.util.ArrayList;
import java.util.List;

import jakarta.annotation.Nullable;

public class GroovyGenCtx {

	private final GroovyFund fund;

	private final List<GroovyItem> items;

	public GroovyGenCtx(GroovyFund fund, GroovyAe groovyAe, List<GroovyItem> items) {
		this.fund = fund;
		this.items = items;
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
}