package cz.tacr.elza.dataexchange.input.reader.handlers;

import jakarta.xml.bind.JAXBElement;

import cz.tacr.elza.dataexchange.input.context.ImportContext;
import cz.tacr.elza.dataexchange.input.context.ImportPhase;
import cz.tacr.elza.dataexchange.input.reader.ItemProcessor;
import cz.tacr.elza.dataexchange.input.sections.SectionLevelProcessor;
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
        ItemProcessor processor = new SectionLevelProcessor(context);
        processor.process(element.getValue());
    }
}
