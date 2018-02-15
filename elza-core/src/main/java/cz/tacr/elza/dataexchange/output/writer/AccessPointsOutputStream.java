package cz.tacr.elza.dataexchange.output.writer;

import cz.tacr.elza.domain.ApRecord;

/**
 * Output stream for access points export.
 */
public interface AccessPointsOutputStream {

    void addAccessPoint(ApRecord accessPoint);

    void processed();

    void close();
}
