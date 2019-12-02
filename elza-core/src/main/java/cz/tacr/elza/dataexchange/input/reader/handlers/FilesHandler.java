package cz.tacr.elza.dataexchange.input.reader.handlers;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.events.StartElement;

import cz.tacr.elza.dataexchange.input.context.ImportContext;
import cz.tacr.elza.dataexchange.input.context.ImportPhase;
import cz.tacr.elza.dataexchange.input.sections.context.SectionContext;
import cz.tacr.elza.dataexchange.input.sections.context.SectionsContext;

public class FilesHandler extends ContextAwareElementHandler
{

    public FilesHandler(ImportContext context) {
        super(context, ImportPhase.SECTIONS);
    }

    @Override
    protected void handleStart(XMLEventReader eventReader, StartElement startElement) {
    }

    @Override
    protected void handleEnd() {
        // all files are read
        // store them in DB
        SectionsContext ssCtx = context.getSections();
        SectionContext secCtx = ssCtx.getCurrentSection();

        secCtx.filesFinished();
    }

}
