package cz.tacr.elza.deimport.reader.handlers;

import javax.xml.bind.JAXBElement;

import cz.tacr.elza.deimport.context.ImportContext;
import cz.tacr.elza.deimport.processor.ItemProcessor;
import cz.tacr.elza.deimport.processor.ItemProcessorFactory;
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