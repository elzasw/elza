package cz.tacr.elza.dataexchange.input.reader.handlers;

import javax.xml.bind.JAXBElement;

import cz.tacr.elza.dataexchange.input.context.ImportContext;
import cz.tacr.elza.dataexchange.input.parties.PartyGroupProcessor;
import cz.tacr.elza.dataexchange.input.reader.ItemProcessor;
import cz.tacr.elza.schema.v2.PartyGroup;

public class PartyGroupElementHandler extends AbstractPartyElementHandler<PartyGroup> {

	public PartyGroupElementHandler(ImportContext context) {
		super(context);
	}

	@Override
	protected void handlePartyElement(JAXBElement<PartyGroup> element) {
		ItemProcessor processor = new PartyGroupProcessor(context);
		processor.process(element);
	}

	@Override
	public Class<PartyGroup> getType() {
		return PartyGroup.class;
	}
}