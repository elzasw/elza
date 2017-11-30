package cz.tacr.elza.dataexchange.output.writer.xml.nodes;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

public abstract class AbstractInternalNode implements XmlNode, Iterable<XmlNode> {

    private final Map<String, String> attributes = new HashMap<>();

    private final QName name;

    public AbstractInternalNode(QName name) {
        this.name = Validate.notNull(name);
    }

    public boolean addAttribute(String localName, String value) {
        Validate.notBlank(localName);
        Validate.notNull(value);

        return attributes.putIfAbsent(localName, value) == null;
    }

    @Override
    public void write(XMLStreamWriter streamWriter) throws XMLStreamException {
        writeBeforeChilds(streamWriter);
        for (XmlNode child : this) {
            child.write(streamWriter);
        }
        writeAfterChilds(streamWriter);
    }

    protected void writeBeforeChilds(XMLStreamWriter streamWriter) throws XMLStreamException {
        streamWriter.writeStartElement(name.getPrefix(), name.getLocalPart(), name.getNamespaceURI());
        // write namespace if not empty
        if (StringUtils.isNotEmpty(name.getNamespaceURI())) {
            streamWriter.writeNamespace(name.getPrefix(), name.getNamespaceURI());
        }
        // write all attributes
        for (Entry<String, String> attribute : attributes.entrySet()) {
            streamWriter.writeAttribute(attribute.getKey(), attribute.getValue());
        }
    }

    protected void writeAfterChilds(XMLStreamWriter streamWriter) throws XMLStreamException {
        streamWriter.writeEndElement();
    }

    @Override
    public void clear() {
        attributes.clear();
        forEach(node -> node.clear());
    }
}
