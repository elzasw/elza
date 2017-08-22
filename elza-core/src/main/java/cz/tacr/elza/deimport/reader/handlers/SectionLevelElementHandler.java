package cz.tacr.elza.deimport.reader.handlers;

import javax.xml.bind.JAXBElement;

import cz.tacr.elza.deimport.context.ImportContext;
import cz.tacr.elza.deimport.context.ImportContext.ImportPhase;
import cz.tacr.elza.deimport.processor.ItemProcessor;
import cz.tacr.elza.deimport.processor.ItemProcessorFactory;
import cz.tacr.elza.schema.v2.Level;

public class SectionLevelElementHandler extends JaxbElementHandler<Level> {

    public SectionLevelElementHandler(ImportContext context) {
        super(context, ImportPhase.SECTIONS);
    }

    @Override
    public Class<Level> getType() {
        return Level.class;
    }

    @Override
    protected void handleJaxbElement(JAXBElement<Level> element) {
        ItemProcessor processor = ItemProcessorFactory.createSectionLevelProcessor(context);
        processor.process(element.getValue());
    }
}
