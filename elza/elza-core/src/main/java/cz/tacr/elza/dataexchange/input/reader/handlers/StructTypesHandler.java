package cz.tacr.elza.dataexchange.input.reader.handlers;

import javax.xml.stream.XMLEventReader;

import cz.tacr.elza.dataexchange.input.context.ImportContext;
import cz.tacr.elza.dataexchange.input.context.ImportPhase;
import cz.tacr.elza.dataexchange.input.reader.XmlElementHandler;
import cz.tacr.elza.dataexchange.input.sections.context.SectionContext;
import cz.tacr.elza.dataexchange.input.sections.context.SectionsContext;

/**
 * When all structured objects are read this class will fire validation and
 * generation of values.
 */
public class StructTypesHandler extends ContextAwareElementHandler implements XmlElementHandler {

    protected StructTypesHandler(ImportContext context) {
        super(context, ImportPhase.SECTIONS);
    }

    @Override
    protected void handleStart(XMLEventReader eventReader) {
    }

    @Override
    protected void handleEnd() {
        // all structuredObjects are read
        // store them in DB
        SectionsContext ssCtx = context.getSections();
        SectionContext secCtx = ssCtx.getCurrentSection();

        secCtx.structObjsFinished();
    }
}
