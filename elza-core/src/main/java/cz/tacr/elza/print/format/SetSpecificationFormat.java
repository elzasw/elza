package cz.tacr.elza.print.format;

import java.util.List;

import cz.tacr.elza.print.item.Item;

public class SetSpecificationFormat implements FormatAction {

	private final String prefix;
	private final String postfix;
	private final boolean afterValue;

	public SetSpecificationFormat(String beforeString, String afterString, boolean afterValue) {
	    this.prefix = beforeString;
	    this.postfix = afterString;
	    this.afterValue = afterValue;
	}
	
	@Override
	public void format(FormatContext ctx, List<Item> items) {
		ctx.setSpecificationPrefix(this.prefix);
		ctx.setSpecificationSeparator(this.postfix);
		ctx.setSpecificationAfterValue(this.afterValue);
	}

}
