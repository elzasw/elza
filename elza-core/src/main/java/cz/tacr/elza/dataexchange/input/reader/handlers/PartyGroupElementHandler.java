package cz.tacr.elza.dataexchange.input.reader.handlers;

import javax.xml.bind.JAXBElement;

import cz.tacr.elza.dataexchange.input.context.ImportContext;
import cz.tacr.elza.dataexchange.input.processor.ItemProcessor;
import cz.tacr.elza.dataexchange.input.processor.ItemProcessorFactory;
import cz.tacr.elza.schema.v2.PartyGroup;

public class PartyGroupElementHandler extends AbstractPartyElementHandler<PartyGroup> {

	public PartyGroupElementHandler(ImportContext context) {
		super(context);
	}

	@Override
	protected void handlePartyElement(JAXBElement<PartyGroup> element) {
		ItemProcessor processor = ItemProcessorFactory.createPartyGroupProcessor(context);
		processor.process(element);
	}

	@Override
	public Class<PartyGroup> getType() {
		return PartyGroup.class;
	}
}