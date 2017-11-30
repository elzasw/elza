package cz.tacr.elza.dataexchange.input.reader;

import java.util.LinkedList;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import javax.xml.stream.util.EventReaderDelegate;

class XmlElementReaderDelegate extends EventReaderDelegate {

    private final XmlElementPath path = new XmlElementPath(32);

    private final LinkedList<XmlElementReaderDelegate.HandlerScope> handlerScopeStack = new LinkedList<>();

    public XmlElementReaderDelegate(XMLEventReader eventReader) {
        super(eventReader);
    }

	public boolean isActiveElementHandler() {
        return handlerScopeStack.size() > 0;
    }

    /**
	 * Add activel element handler to the stack
	 * 
	 * @param path
	 *            Path where handler will be active
	 */
	public void activateElementHandler(XmlElementHandler handler, XmlElementPath path) {
        handlerScopeStack.add(new HandlerScope(handler, path));
    }

	// Method is called by ElementReader and also by JAXB deserialization
    @Override
    public XMLEvent nextEvent() throws XMLStreamException {
        XMLEvent event = super.nextEvent();

		// update current path
        if (event.isStartElement()) {
            handleNextStartElement(event.asStartElement());
        } else if (event.isEndElement()) {
            handleNextEndElement(event.asEndElement());
        }
        return event;
    }

    private void handleNextStartElement(StartElement startElement) throws XMLStreamException {
        path.enterElement(startElement.getName());
		if (isActiveElementHandler()) {
            HandlerScope handlerScope = handlerScopeStack.getLast();
            handlerScope.checkScope(path);
        }
    }

    private void handleNextEndElement(EndElement endElement) {
		if (isActiveElementHandler()) {
            HandlerScope handlerScope = handlerScopeStack.getLast();
            if (path.equalPath(handlerScope.path)) {
                handlerScopeStack.removeLast();
                handlerScope.handler.handleEndElement();
            }
        }
        path.leaveElement(endElement.getName());
    }

	/**
	 * One active element handler
	 * 
	 *
	 */
    private static class HandlerScope {

		/**
		 * Handler
		 */
        private final XmlElementHandler handler;

		/**
		 * Path where was handler activated
		 */
        private final XmlElementPath path;

        public HandlerScope(XmlElementHandler handler, XmlElementPath path) {
            this.handler = handler;
            this.path = path;
        }

		/**
		 * Check if handler is inside its scope
		 * 
		 * @param readerPath
		 * @throws XMLStreamException
		 */
        public void checkScope(XmlElementPath readerPath) throws XMLStreamException {
            if (!path.matchPath(readerPath)) {
                throw new XMLStreamException("Invalid handler scope, readerPath:" + readerPath + ", handlerPath:" + path);
            }
        }
    }

	/**
	 * Return current path
	 * 
	 * @return
	 */
	public XmlElementPath getPath() {
		return path;
	}
}
