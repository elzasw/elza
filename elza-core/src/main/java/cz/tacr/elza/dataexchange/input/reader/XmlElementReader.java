package cz.tacr.elza.dataexchange.input.reader;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cz.tacr.elza.dataexchange.input.DEImportException;

/**
 * SAX based XML element reader
 */
public class XmlElementReader {

	private static final Logger logger = LoggerFactory.getLogger(XmlElementReader.class);

	/**
	 * Map of registered element handlers
	 */
    private final Map<String, XmlElementHandler> elementHandlerMap = new HashMap<>();

    private final XmlElementReaderDelegate eventReader;

    private XmlElementReader(XMLEventReader eventReader) {
        this.eventReader = new XmlElementReaderDelegate(eventReader);
    }

	/**
	 * Add element handler
	 * 
	 * @param localPath
	 *            Path to which is handler attached
	 * @param elementHandler
	 */
    public void addElementHandler(String localPath, XmlElementHandler elementHandler) {
    	Objects.requireNonNull(localPath);

		XmlElementHandler currentHandler = elementHandlerMap.put(localPath, elementHandler);
		if (currentHandler != null) {
            throw new IllegalStateException("Path for element handler already registered, path:" + localPath);
        }
    }

	/**
	 * Remove element handler
	 * 
	 * @param localPath
	 *            Path to which is handler attached
	 */
	public void removeElementHandler(String localPath) {
		Objects.requireNonNull(localPath);

		XmlElementHandler currentHandler = elementHandlerMap.remove(localPath);
		if (currentHandler == null) {
			throw new IllegalStateException("No element handler, path:" + localPath);
		}
	}

    public void readDocument() throws XMLStreamException {
        while (eventReader.hasNext()) {
			XMLEvent peekEvent = eventReader.peek();

			if (peekEvent.isStartElement()) {

				processNextElementHandler(peekEvent);

				if (peekEvent == eventReader.peek()) {
					// element was not processed, we have to move to next event
					eventReader.nextEvent();
				}
			} else {
				// simply continue to next start element
				eventReader.nextEvent();
            }
        }
        eventReader.close();
    }

	private void processNextElementHandler(XMLEvent peekEvent) throws XMLStreamException {

		// prepare path for next element
		XmlElementPath path = eventReader.getPath();
		// create new path for next element
		XmlElementPath nextPath = path.copy();

        StartElement startElement = peekEvent.asStartElement();
        nextPath.enterElement(startElement.getName());

		// try to get handler
		XmlElementHandler handler = elementHandlerMap.get(nextPath.toString());
        if (handler != null) {

			// activate and process
			eventReader.activateElementHandler(handler, nextPath);
			try {
                handler.handleStartElement(eventReader, startElement);
			} catch (Throwable t) {
				logger.error("", t);
				int lineNumber = peekEvent.getLocation().getLineNumber();
				throw new DEImportException("Reading of XML element failed, path:" + path + ", line:" + lineNumber, t);
			}

        }
    }

	/**
	 * Create instance of XmlElementReader
	 * 
	 * @param is
	 *            input stream with XML
	 * @return
	 * @throws XMLStreamException
	 */
    public static XmlElementReader create(InputStream is) throws XMLStreamException {
        XMLInputFactory inputFactory = XMLInputFactory.newInstance();
        XMLEventReader eventReader = inputFactory.createXMLEventReader(is);
        return new XmlElementReader(eventReader);
    }
}
