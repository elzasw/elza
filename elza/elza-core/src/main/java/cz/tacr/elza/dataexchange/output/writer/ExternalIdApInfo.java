package cz.tacr.elza.dataexchange.output.writer;

import java.util.Collection;

import cz.tacr.elza.domain.ApExternalId;

public interface ExternalIdApInfo {

    Collection<ApExternalId> getExternalIds();

    void setExternalIds(Collection<ApExternalId> externalIds);
}
