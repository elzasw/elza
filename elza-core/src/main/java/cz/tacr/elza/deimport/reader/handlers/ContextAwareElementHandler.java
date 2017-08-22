package cz.tacr.elza.deimport.reader.handlers;

import javax.xml.stream.XMLEventReader;

import cz.tacr.elza.deimport.context.ImportContext;
import cz.tacr.elza.deimport.context.ImportContext.ImportPhase;
import cz.tacr.elza.deimport.reader.XmlElementHandler;

public abstract class ContextAwareElementHandler implements XmlElementHandler {

    protected final ImportContext context;

    protected final ImportPhase phase;

    protected ContextAwareElementHandler(ImportContext context, ImportPhase phase) {
        this.context = context;
        this.phase = phase;
    }

    @Override
    public final void handleStartElement(XMLEventReader eventReader) {
        context.setCurrentPhase(phase);
        handleStart(eventReader);
    }

    @Override
    public final void handleEndElement() {
        // XXX: place holder for phase end notification
        handleEnd();
    }

    protected abstract void handleStart(XMLEventReader eventReader);

    protected abstract void handleEnd();
}
