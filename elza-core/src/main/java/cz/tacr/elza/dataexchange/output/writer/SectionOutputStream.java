package cz.tacr.elza.dataexchange.output.writer;

import cz.tacr.elza.dataexchange.output.sections.ExportLevelInfo;
import cz.tacr.elza.domain.ArrPacket;

/**
 * Output stream for section export.
 */
public interface SectionOutputStream {

    void addLevel(ExportLevelInfo levelInfo);

    void addPacket(ArrPacket packet);

    /**
     * Writer will be notified about finished section export.
     */
    void processed();

    /**
     * Release all resources.
     */
    void close();
}