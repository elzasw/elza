package cz.tacr.elza.dataexchange.output.writer;

import java.util.Collection;

import cz.tacr.elza.domain.ApAccessPoint;
import cz.tacr.elza.domain.ApExternalId;

public interface BaseApInfo {

    ApAccessPoint getAp();

    Collection<ApExternalId> getExternalIds();
}