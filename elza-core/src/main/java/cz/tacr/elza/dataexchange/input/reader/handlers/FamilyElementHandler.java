package cz.tacr.elza.dataexchange.input.reader.handlers;

import javax.xml.bind.JAXBElement;

import cz.tacr.elza.dataexchange.input.context.ImportContext;
import cz.tacr.elza.dataexchange.input.processor.ItemProcessor;
import cz.tacr.elza.dataexchange.input.processor.ItemProcessorFactory;
import cz.tacr.elza.schema.v2.Family;

public class FamilyElementHandler extends AbstractPartyElementHandler<Family> {

	public FamilyElementHandler(ImportContext context) {
		super(context);
	}

	@Override
	protected void handlePartyElement(JAXBElement<Family> element) {
		ItemProcessor processor = ItemProcessorFactory.createFamilyProcessor(context);
		processor.process(element);
	}

	@Override
	public Class<Family> getType() {
		return Family.class;
	}
}