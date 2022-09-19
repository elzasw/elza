package cz.tacr.elza.dataexchange.output.writer;

import java.io.OutputStream;

import javax.xml.stream.XMLStreamException;

import cz.tacr.elza.dataexchange.output.context.ExportContext;
import cz.tacr.elza.dataexchange.output.sections.SectionContext;

/**
 * Export builder. Implementation specifies output format.
 */
public interface ExportBuilder {

    SectionOutputStream openSectionOutputStream(SectionContext sectionContext);

    ApOutputStream openAccessPointsOutputStream(ExportContext exportContext);

    /**
     * Builds export from collected data through output streams.
     */
    void build(OutputStream os) throws XMLStreamException;

    /**
     * Release all resources.
     */
    void clear();

    /**
     * Return if deleted APs can be exported to the given
     * output format.
     * 
     * @return
     */
    boolean canExportDeletedAPs();
}
