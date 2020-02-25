package cz.tacr.elza.dataexchange.input.reader;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.events.StartElement;

/**
 * Generic interface for handle SAX events
 */
public interface XmlElementHandler {

    void handleStartElement(XMLEventReader eventReader, StartElement startElement);

    void handleEndElement();
}
