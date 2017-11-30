package cz.tacr.elza.dataexchange.input.reader.handlers;

import javax.xml.bind.JAXBElement;

import cz.tacr.elza.dataexchange.input.context.ImportContext;
import cz.tacr.elza.dataexchange.input.processor.ItemProcessor;
import cz.tacr.elza.dataexchange.input.processor.ItemProcessorFactory;
import cz.tacr.elza.schema.v2.Person;

public class PersonElementHandler extends AbstractPartyElementHandler<Person> {

	public PersonElementHandler(ImportContext context) {
		super(context);
	}

	@Override
	protected void handlePartyElement(JAXBElement<Person> element) {
		ItemProcessor processor = ItemProcessorFactory.createPersonProcessor(context);
		processor.process(element);
	}

	@Override
	public Class<Person> getType() {
		return Person.class;
	}
}
