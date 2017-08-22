package cz.tacr.elza.deimport.reader.handlers;

import javax.xml.bind.JAXBElement;

import cz.tacr.elza.deimport.context.ImportContext;
import cz.tacr.elza.deimport.processor.ItemProcessor;
import cz.tacr.elza.deimport.processor.ItemProcessorFactory;
import cz.tacr.elza.schema.v2.Event;

public class EventElementHandler extends AbstractPartyElementHandler<Event> {

	public EventElementHandler(ImportContext context) {
		super(context);
	}

	@Override
	protected void handlePartyElement(JAXBElement<Event> element) {
		ItemProcessor processor = ItemProcessorFactory.createFamilyProcessor(context);
		processor.process(element);
	}

	@Override
	public Class<Event> getType() {
		return Event.class;
	}
}