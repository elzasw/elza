package cz.tacr.elza.deimport.reader.handlers;

import javax.xml.bind.JAXBElement;

import cz.tacr.elza.deimport.context.ImportContext;
import cz.tacr.elza.deimport.processor.ItemProcessor;
import cz.tacr.elza.deimport.processor.ItemProcessorFactory;
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