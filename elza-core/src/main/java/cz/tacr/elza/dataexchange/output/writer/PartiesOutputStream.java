package cz.tacr.elza.dataexchange.output.writer;

import cz.tacr.elza.domain.ParParty;
import cz.tacr.elza.service.vo.ApAccessPointData;

/**
 * Output stream for parties export.
 */
public interface PartiesOutputStream {

    void addParty(ParParty party, ApAccessPointData apData);

    /**
     * Writer will be notified about finished parties export.
     */
    void processed();

    /**
     * Release all resources.
     */
    void close();
}
