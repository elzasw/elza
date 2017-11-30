package cz.tacr.elza.dataexchange.input.reader;

import javax.xml.stream.XMLEventReader;

/**
 * Generic interface for handle SAX events
 */
public interface XmlElementHandler {

    void handleStartElement(XMLEventReader eventReader);

    void handleEndElement();
}
