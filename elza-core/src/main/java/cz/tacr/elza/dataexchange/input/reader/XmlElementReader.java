package cz.tacr.elza.dataexchange.input.reader;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;

import org.apache.commons.lang3.Validate;

public class XmlElementReader {

    private final Map<String, XmlElementHandler> elementHandlerMap = new HashMap<>();

    private final XmlElementReaderDelegate delegate;

    private XmlElementReader(XMLEventReader eventReader) {
        this.delegate = new XmlElementReaderDelegate(eventReader);
    }

    public void addElementHandler(String localPath, XmlElementHandler elementHandler) {
        XmlElementHandler currentHandler = elementHandlerMap.put(Validate.notNull(localPath), elementHandler);
        if (currentHandler != null && currentHandler != elementHandler) {
            throw new IllegalStateException("Path for element handler already registered, path:" + localPath);
        }
    }

    public void readDocument() throws XMLStreamException {
        while (delegate.hasNext()) {
            XMLEvent peek = delegate.peek();
            if (peek.isStartElement()) {
                processNextElementHandler();
            }
            delegate.nextEvent();
        }
        delegate.close();
    }

    private void processNextElementHandler() throws XMLStreamException {
        XmlElementReaderPath path = delegate.getNextElementPath();
        XmlElementHandler handler = elementHandlerMap.get(path.toString());
        if (handler != null) {
            if (!delegate.processHandler(handler, path)) {
                delegate.nextEvent();
            }
        }
    }

    public static XmlElementReader create(InputStream is) throws XMLStreamException {
        XMLInputFactory inputFactory = XMLInputFactory.newInstance();
        XMLEventReader eventReader = inputFactory.createXMLEventReader(is);
        return new XmlElementReader(eventReader);
    }
}
