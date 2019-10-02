package cz.tacr.elza.dataexchange.output.aps;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.Validate;

import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.dataexchange.output.loaders.LoadDispatcher;
import cz.tacr.elza.dataexchange.output.loaders.NestedLoadDispatcher;
import cz.tacr.elza.dataexchange.output.writer.ExternalIdApInfo;
import cz.tacr.elza.domain.ApExternalId;
import cz.tacr.elza.domain.ApExternalIdType;

public class ExternalIdDispatcher extends NestedLoadDispatcher<ApExternalId> {

    // --- fields ---

    private final ExternalIdApInfo info;

    private final StaticDataProvider staticData;

    private final List<ApExternalId> externalIds = new ArrayList<>();

    // --- constructor ---

    public ExternalIdDispatcher(ExternalIdApInfo info, LoadDispatcher<?> apInfoDispatcher, StaticDataProvider staticData) {
        super(apInfoDispatcher);
        this.info = info;
        this.staticData = staticData;
    }

    // --- methods ---

    @Override
    public void onLoad(ApExternalId result) {
        // init external id type
        ApExternalIdType type = staticData.getApEidTypeById(result.getExternalIdTypeId());
        Validate.notNull(type);
        result.setExternalIdType(type);
        // set result
        externalIds.add(result);
    }

    @Override
    protected void onCompleted() {
        info.setExternalIds(externalIds);
    }
}
