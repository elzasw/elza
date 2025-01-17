package cz.tacr.elza.groovy;

import java.util.Objects;

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
	
	/**
	 * Append text to the current item
	 * 
	 * Text is without separator
	 * @param textValue
	 * @return
	 */
	public ResultBuilder appendText(final String textValue) {
		if (textValue != null) {
			result.append(textValue);
		}
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

	public ResultBuilder appendItem(final String itemType, final String prefix) {
		ctx.getItemsByItemType(itemType).forEach(i -> append(prefix, i));
		return this;
	}

	public ResultBuilder appendItem(final String itemType) {
		ctx.getItemsByItemType(itemType).forEach(i -> append(i));
		return this;
	}

	public ResultBuilder appendItemWithLimit(final String itemType, int lengthLimit) {
		GroovyItem item = ctx.getFirstItemByItemType(itemType);
		if (item != null) {
			String itemString = item.getValue();
	    	if (itemString.length() > lengthLimit) {
	    		itemString = itemString.substring(0, lengthLimit);
	    	}
			result.append(separator).append(itemString);
		}
		return this;
	}

	public ResultBuilder appendItemWithSpecLabel(final String itemType, final String itemSpec) {
    	for (GroovyItem groovyItem : ctx.getItems()) {
    		if (groovyItem.getTypeCode().equals(itemType) && Objects.equals(groovyItem.getSpecCode(), itemSpec)) {
    			append(groovyItem.getSpecType().getShortcut() + " ", groovyItem);
    		}
    	}
		return this;
	}

    @Override
	public String toString() {
		return result.toString();
	}
}
