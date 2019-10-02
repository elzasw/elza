package cz.tacr.elza.dataexchange.input.reader.handlers;

import javax.xml.bind.JAXBElement;

import cz.tacr.elza.dataexchange.input.context.ImportContext;
import cz.tacr.elza.dataexchange.input.context.ImportPhase;
import cz.tacr.elza.dataexchange.input.reader.ItemProcessor;
import cz.tacr.elza.dataexchange.input.sections.StructObjProcessor;
import cz.tacr.elza.schema.v2.StructuredObject;

public class StructObjectElementHandler extends JaxbElementHandler<StructuredObject> {

    protected StructObjectElementHandler(ImportContext context) {
        super(context, ImportPhase.SECTIONS);
    }

    @Override
    public Class<StructuredObject> getType() {
        return StructuredObject.class;
    }

    @Override
    protected void handleJaxbElement(JAXBElement<StructuredObject> element) {
         ItemProcessor processor = new StructObjProcessor(context);
         processor.process(element.getValue());
    }
}
