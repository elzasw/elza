package cz.tacr.elza.dataexchange.output.writer;

import java.util.Collection;

import cz.tacr.elza.domain.ApAccessPoint;
import cz.tacr.elza.domain.ApExternalId;
import cz.tacr.elza.domain.ApState;

public interface BaseApInfo {

    ApAccessPoint getAp();

    // todo[dataexchange]: ApState se nikde neplni
    ApState getApState();

    Collection<ApExternalId> getExternalIds();
}