package cz.tacr.elza.dataexchange.output.writer;

import cz.tacr.elza.domain.RegRecord;

/**
 * Output stream for access points export.
 */
public interface AccessPointsOutputStream {

    void addAccessPoint(RegRecord accessPoint);

    void processed();

    void close();
}
