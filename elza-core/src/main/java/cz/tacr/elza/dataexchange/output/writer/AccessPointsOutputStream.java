package cz.tacr.elza.dataexchange.output.writer;

import cz.tacr.elza.domain.ApAccessPoint;
import cz.tacr.elza.service.vo.ApAccessPointData;

/**
 * Output stream for access points export.
 */
public interface AccessPointsOutputStream {

    void addAccessPoint(ApAccessPoint accessPoint, ApAccessPointData pointData);

    void processed();

    void close();
}
