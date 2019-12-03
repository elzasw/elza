package cz.tacr.elza.dataexchange.input.reader.handlers;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.StartElement;

import cz.tacr.elza.dataexchange.input.DEImportException;
import cz.tacr.elza.dataexchange.input.context.ImportContext;
import cz.tacr.elza.dataexchange.input.context.ImportPhase;

public class StructTypeElementHandler extends ContextAwareElementHandler {

    protected StructTypeElementHandler(ImportContext context) {
        super(context, ImportPhase.SECTIONS);
    }

    @Override
    protected void handleStart(XMLEventReader eventReader, StartElement startElement) {
        StartElement nextStartElement;
        try {
            nextStartElement = eventReader.nextEvent().asStartElement();
        } catch (Exception e) {
            throw new DEImportException("Cannot read section start element");
        }

        Attribute stCode = nextStartElement.getAttributeByName(new QName("c"));
        context.getSections().getCurrentSection().setProcessingStructType(stCode.getValue());
    }

    @Override
    protected void handleEnd() {
        // NOP
    }
}
