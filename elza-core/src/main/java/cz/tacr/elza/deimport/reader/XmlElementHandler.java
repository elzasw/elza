package cz.tacr.elza.deimport.reader;

import javax.xml.stream.XMLEventReader;

/**
 * Created by todtj on 15.05.2017.
 */
public interface XmlElementHandler {

    void handleStartElement(XMLEventReader eventReader);

    void handleEndElement();
}
