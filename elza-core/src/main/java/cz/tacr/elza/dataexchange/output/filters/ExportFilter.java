package cz.tacr.elza.dataexchange.output.filters;

import cz.tacr.elza.dataexchange.output.sections.LevelInfoImpl;
import cz.tacr.elza.dataexchange.output.writer.LevelInfo;
import cz.tacr.elza.dataexchange.output.writer.StructObjectInfo;

/**
 * Base export filter
 * 
 *
 */
public interface ExportFilter {

    LevelInfo processLevel(LevelInfoImpl levelInfo);

    StructObjectInfo processStructObj(StructObjectInfo structObjectInfo);

}
