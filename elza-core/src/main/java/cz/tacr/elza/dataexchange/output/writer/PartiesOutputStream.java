package cz.tacr.elza.dataexchange.output.writer;

import cz.tacr.elza.domain.ParParty;

/**
 * Output stream for parties export.
 */
public interface PartiesOutputStream {

    void addParty(ParParty party);

    /**
     * Writer will be notified about finished parties export.
     */
    void processed();

    /**
     * Release all resources.
     */
    void close();
}
