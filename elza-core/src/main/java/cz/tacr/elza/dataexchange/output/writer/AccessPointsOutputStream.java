package cz.tacr.elza.dataexchange.output.writer;

import cz.tacr.elza.domain.RegRecord;

public interface AccessPointsOutputStream {

    void addAccessPoint(RegRecord accessPoint);

    void processed();

    void close();
}
