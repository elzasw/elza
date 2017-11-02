package cz.tacr.elza.dataexchange.input.reader.handlers;

import javax.xml.bind.JAXBElement;

import cz.tacr.elza.dataexchange.input.context.ImportContext;
import cz.tacr.elza.dataexchange.input.context.ImportPhase;
import cz.tacr.elza.dataexchange.input.processor.ItemProcessor;
import cz.tacr.elza.dataexchange.input.processor.ItemProcessorFactory;
import cz.tacr.elza.schema.v2.Institution;

public class InstitutionElementHandler extends JaxbElementHandler<Institution> {

    public InstitutionElementHandler(ImportContext context) {
        super(context, ImportPhase.INSTITUTIONS);
    }

    @Override
    public Class<Institution> getType() {
        return Institution.class;
    }

    @Override
    protected void handleJaxbElement(JAXBElement<Institution> element) {
        ItemProcessor processor = ItemProcessorFactory.createInstitutionProcessor(context);
        processor.process(element.getValue());
    }
}