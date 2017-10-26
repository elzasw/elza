package cz.tacr.elza.dataexchange.output.writer.xml.nodes;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

public interface XmlNode {

    void write(XMLStreamWriter streamWriter) throws XMLStreamException;

    void clear();
}
