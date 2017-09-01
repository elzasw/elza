package cz.tacr.elza.deimport.reader;

import java.util.LinkedList;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import javax.xml.stream.util.EventReaderDelegate;

import cz.tacr.elza.deimport.DEImportException;

class XmlElementReaderDelegate extends EventReaderDelegate {

    private final XmlElementReaderPath path = new XmlElementReaderPath(32);

    private final LinkedList<XmlElementReaderDelegate.HandlerScope> handlerScopeStack = new LinkedList<>();

    public XmlElementReaderDelegate(XMLEventReader eventReader) {
        super(eventReader);
    }

    public boolean isProcessingHandler() {
        return handlerScopeStack.size() > 0;
    }

    public XmlElementReaderPath getNextElementPath() throws XMLStreamException {
        XmlElementReaderPath path = this.path.copy();
        XMLEvent nextEvent = peek();
        if (nextEvent.isStartElement()) {
            path.enterElement(nextEvent.asStartElement().getName());
        } else if (nextEvent.isEndElement()) {
            path.enterElement(nextEvent.asEndElement().getName());
        }
        return path;
    }

    /**
     * @return True when stream position was changed by handler.
     */
    public boolean processHandler(XmlElementHandler handler, XmlElementReaderPath path) throws XMLStreamException {
        XMLEvent peekEvent = peek();
        if (!peekEvent.isStartElement()) {
            throw new XMLStreamException("Handler must proceed from start element");
        }
        handlerScopeStack.add(new HandlerScope(handler, path));
        try {
            handler.handleStartElement(this);
        } catch (Throwable t) {
            int lineNumber = peekEvent.getLocation().getLineNumber();
            throw new DEImportException("Reading of XML element failed, path:" + path + ", line:" + lineNumber, t);
        }
        return peekEvent != peek();
    }

    @Override
    public XMLEvent nextEvent() throws XMLStreamException {
        XMLEvent event = super.nextEvent();
        if (event.isStartElement()) {
            handleNextStartElement(event.asStartElement());
        } else if (event.isEndElement()) {
            handleNextEndElement(event.asEndElement());
        }
        return event;
    }

    private void handleNextStartElement(StartElement startElement) throws XMLStreamException {
        path.enterElement(startElement.getName());
        if (isProcessingHandler()) {
            HandlerScope handlerScope = handlerScopeStack.getLast();
            handlerScope.checkScope(path);
        }
    }

    private void handleNextEndElement(EndElement endElement) {
        if (isProcessingHandler()) {
            HandlerScope handlerScope = handlerScopeStack.getLast();
            if (path.equalPath(handlerScope.path)) {
                handlerScopeStack.removeLast();
                handlerScope.handler.handleEndElement();
            }
        }
        path.leaveElement(endElement.getName());
    }

    private static class HandlerScope {

        private final XmlElementHandler handler;

        private final XmlElementReaderPath path;

        public HandlerScope(XmlElementHandler handler, XmlElementReaderPath path) {
            this.handler = handler;
            this.path = path;
        }

        public void checkScope(XmlElementReaderPath readerPath) throws XMLStreamException {
            if (!path.matchPath(readerPath)) {
                throw new XMLStreamException("Invalid handler scope, readerPath:" + readerPath + ", handlerPath:" + path);
            }
        }
    }
}
