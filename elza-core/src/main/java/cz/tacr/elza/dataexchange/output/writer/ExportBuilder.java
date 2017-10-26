package cz.tacr.elza.dataexchange.output.writer;

import java.io.OutputStream;

import javax.xml.stream.XMLStreamException;

import cz.tacr.elza.dataexchange.output.sections.SectionContext;

public interface ExportBuilder {

    SectionOutputStream openSectionOutputStream(SectionContext sectionContext);

    AccessPointsOutputStream openAccessPointsOutputStream();

    PartiesOutputStream openPartiesOutputStream();

    void build(OutputStream os) throws XMLStreamException;

    void clear();
}
