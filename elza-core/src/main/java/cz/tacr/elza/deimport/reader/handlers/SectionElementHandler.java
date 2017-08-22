package cz.tacr.elza.deimport.reader.handlers;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.StartElement;

import cz.tacr.elza.deimport.DEImportException;
import cz.tacr.elza.deimport.context.ImportContext;
import cz.tacr.elza.deimport.context.ImportContext.ImportPhase;

public class SectionElementHandler extends ContextAwareElementHandler {

    public SectionElementHandler(ImportContext context) {
        super(context, ImportPhase.SECTIONS);
    }

    @Override
    public void handleStart(XMLEventReader eventReader) {
        StartElement startElement;
        try {
            startElement = eventReader.peek().asStartElement();
        } catch (XMLStreamException e) {
            throw new DEImportException("Cannot read section start element");
        }
        Attribute ruleSetCode = startElement.getAttributeByName(new QName("rule"));
        context.getSections().beginSection(ruleSetCode.getValue());
    }

    @Override
    public void handleEnd() {
        context.getSections().endSection();
    }
}
