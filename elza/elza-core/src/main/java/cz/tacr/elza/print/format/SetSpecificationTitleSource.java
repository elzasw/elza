package cz.tacr.elza.print.format;

import java.util.List;

import cz.tacr.elza.print.item.Item;

public class SetSpecificationTitleSource implements FormatAction {
	private final SpecTitleSource specTitleSource;
	
	public SetSpecificationTitleSource(SpecTitleSource specTitleSource) {
		this.specTitleSource = specTitleSource;
	}

	@Override
	public void format(FormatContext ctx, List<Item> items) {
		ctx.setSpecTitleSource(specTitleSource);
	}

}
