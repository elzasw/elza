package cz.tacr.elza.dataexchange.output.writer;

import java.util.Collection;

import cz.tacr.elza.domain.ApBinding;

public interface ExternalIdApInfo {

    Collection<ApBinding> getExternalIds();

    void setExternalIds(Collection<ApBinding> externalIds);
}
