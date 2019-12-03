package cz.tacr.elza.dataexchange.input.reader.handlers;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.events.StartElement;

import cz.tacr.elza.dataexchange.input.context.ImportContext;
import cz.tacr.elza.dataexchange.input.context.ImportPhase;
import cz.tacr.elza.dataexchange.input.reader.XmlElementHandler;

/**
 * Base implementation of XmlElementHandler
 * 
 * Each handler is applied at some phase.
 */
public abstract class ContextAwareElementHandler implements XmlElementHandler {

	// Import context
    protected final ImportContext context;

    protected final ImportPhase phase;

    protected ContextAwareElementHandler(ImportContext context, ImportPhase phase) {
        this.context = context;
        this.phase = phase;
    }

    @Override
    public final void handleStartElement(XMLEventReader eventReader, StartElement startElement) {
        context.setCurrentPhase(phase);
        handleStart(eventReader, startElement);
    }

    @Override
    public final void handleEndElement() {
        // XXX: place holder for phase end notification
        handleEnd();
    }

    protected abstract void handleStart(XMLEventReader eventReader, StartElement startElement);

    protected abstract void handleEnd();
}
