package cz.tacr.elza.dataexchange.output.writer;

import java.util.Collection;

import cz.tacr.elza.domain.ApBindingState;

public interface ExternalIdApInfo {

    Collection<ApBindingState> getExternalIds();

    void setExternalIds(Collection<ApBindingState> externalIds);
}
