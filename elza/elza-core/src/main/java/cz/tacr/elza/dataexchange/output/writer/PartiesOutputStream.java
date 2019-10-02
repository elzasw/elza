package cz.tacr.elza.dataexchange.output.writer;

import cz.tacr.elza.dataexchange.output.parties.PartyInfo;

/**
 * Output stream for parties export.
 */
public interface PartiesOutputStream {

    void addParty(PartyInfo partyInfo);

    /**
     * Writer will be notified about finished parties export.
     */
    void processed();

    /**
     * Release all resources.
     */
    void close();
}
