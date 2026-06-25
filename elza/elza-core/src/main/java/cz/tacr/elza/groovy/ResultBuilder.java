package cz.tacr.elza.groovy;

import java.util.Objects;

import org.apache.commons.lang3.StringUtils;

public class ResultBuilder {

	private GroovyGenCtx ctx;

	private final StringBuilder result = new StringBuilder();

	private String separator = ", ";

	public ResultBuilder(GroovyGenCtx ctx) {
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

	public ResultBuilder append(final String itemValue) {
		if(StringUtils.isEmpty(itemValue)) {
			return this;
		}
		if(result.length() > 0) {
			result.append(separator);
		}
		result.append(itemValue);
		return this;
	}

	public ResultBuilder append(final GroovyItem item) {
		return append(item.getValue());
	}

	public ResultBuilder append(final String prefix, GroovyItem item) {
		if(StringUtils.isEmpty(prefix)) {
			append(item);
		} else {
			append(prefix);
			String value = item.getValue();
			if(StringUtils.isNotEmpty(value)) {
				appendText(value);
			}
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
		if (item == null) {
			return this;
		}
		return appendWithLimit(item.getValue(), lengthLimit);
	}

	public ResultBuilder appendWithLimit(String itemString, int lengthLimit) {
		if (itemString == null) {
			return this;
		}
	    if (itemString.length() > (lengthLimit+3)) {
	    	itemString = itemString.substring(0, lengthLimit) + "...";
	    }
		return append(itemString);
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
