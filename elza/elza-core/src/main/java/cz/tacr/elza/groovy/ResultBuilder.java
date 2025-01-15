package cz.tacr.elza.groovy;

import java.util.Objects;

import jakarta.annotation.Nullable;

public class ResultBuilder {

	private GroovyGenCtx ctx;

	private StringBuilder result;

	private String separator = ", ";

	public ResultBuilder(GroovyItem init, GroovyGenCtx ctx) {
		this.result = new StringBuilder(init.getValue());
		this.ctx = ctx;
	}

	public ResultBuilder setSeparator(String separator) {
		this.separator = separator;
		return this;
	}

	public ResultBuilder append(final String item) {
		if (item != null) {
			result.append(separator).append(item);
		}
		return this;
	}

	public ResultBuilder append(final GroovyItem item) {
		if (item != null) {
			result.append(separator).append(item.getValue());
		}
		return this;
	}

	public ResultBuilder append(final String prefix, GroovyItem item) {
		if (item != null) {
			result.append(separator).append(prefix).append(item.getValue());
		}
		return this;
	}

	public ResultBuilder append(final String itemType, int lengthLimit) {
		GroovyItem item = findItemByItemTypeCode(itemType);
		if (item != null) {
			String itemString = item.getValue();
	    	if (itemString.length() > lengthLimit) {
	    		itemString = itemString.substring(0, lengthLimit);
	    	}
			result.append(separator).append(itemString);
		}
		return this;
	}

	public ResultBuilder append(final String itemType, final String prefix) {
		append(prefix, findItemByItemTypeCode(itemType));
		return this;
	}

	public ResultBuilder append(final String itemType, final String itemSpec, final String prefix) {
    	for (GroovyItem item : ctx.getItems()) {
    		if (item.getTypeCode().equals(itemType) && Objects.equals(item.getSpecCode(), itemSpec)) {
    			append(prefix, item);
    		}
    	}
		return this;
	}

    @Nullable
    private GroovyItem findItemByItemTypeCode(final String itemType) {
    	for (GroovyItem item : ctx.getItems()) {
    		if (item.getTypeCode().equals(itemType)) {
    			return item;
    		}
    	}
    	return null;
    }

    @Override
	public String toString() {
		return result.toString();
	}
}
