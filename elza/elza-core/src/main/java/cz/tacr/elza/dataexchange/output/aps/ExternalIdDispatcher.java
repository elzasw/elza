package cz.tacr.elza.dataexchange.output.aps;

import java.util.ArrayList;
import java.util.List;

import cz.tacr.elza.domain.ApExternalSystem;
import org.apache.commons.lang.Validate;

import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.dataexchange.output.loaders.LoadDispatcher;
import cz.tacr.elza.dataexchange.output.loaders.NestedLoadDispatcher;
import cz.tacr.elza.dataexchange.output.writer.ExternalIdApInfo;
import cz.tacr.elza.domain.ApBinding;
import cz.tacr.elza.domain.ApExternalIdType;

public class ExternalIdDispatcher extends NestedLoadDispatcher<ApBinding> {

    // --- fields ---

    private final ExternalIdApInfo info;

    private final StaticDataProvider staticData;

    private final List<ApBinding> externalIds = new ArrayList<>();

    // --- constructor ---

    public ExternalIdDispatcher(ExternalIdApInfo info, LoadDispatcher<?> apInfoDispatcher, StaticDataProvider staticData) {
        super(apInfoDispatcher);
        this.info = info;
        this.staticData = staticData;
    }

    // --- methods ---

    @Override
    public void onLoad(ApBinding result) {
        // init external id type
        ApExternalSystem apExternalSystem = staticData.getApExternalSystemByCode(result.getApExternalSystem().getCode());
        Validate.notNull(apExternalSystem);
        result.setApExternalSystem(apExternalSystem);
        // set result
        externalIds.add(result);
    }

    @Override
    protected void onCompleted() {
        info.setExternalIds(externalIds);
    }
}
