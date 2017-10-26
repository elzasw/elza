package cz.tacr.elza.dataexchange.output.writer;

import cz.tacr.elza.dataexchange.output.sections.ExportLevelInfo;
import cz.tacr.elza.domain.ArrPacket;

public interface SectionOutputStream {

    void addLevel(ExportLevelInfo levelInfo);

    void addPacket(ArrPacket packet);

    void processed();

    void close();
}
