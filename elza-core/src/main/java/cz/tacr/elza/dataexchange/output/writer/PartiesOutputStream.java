package cz.tacr.elza.dataexchange.output.writer;

import cz.tacr.elza.domain.ParParty;

public interface PartiesOutputStream {

    void addParty(ParParty party);

    void processed();

    void close();
}
