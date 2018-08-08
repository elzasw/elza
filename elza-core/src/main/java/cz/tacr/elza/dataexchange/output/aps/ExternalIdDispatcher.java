package cz.tacr.elza.dataexchange.output.aps;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.Validate;

import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.dataexchange.output.loaders.LoadDispatcher;
import cz.tacr.elza.dataexchange.output.loaders.NestedLoadDispatcher;
import cz.tacr.elza.domain.ApExternalId;
import cz.tacr.elza.domain.ApExternalIdType;

public class ExternalIdDispatcher extends NestedLoadDispatcher<ApExternalId> {

    private final List<ApExternalId> externalIds = new ArrayList<>();

    private final BaseApInfoImpl baseApInfo;

    private final StaticDataProvider staticData;

    public ExternalIdDispatcher(BaseApInfoImpl baseApInfo, LoadDispatcher<?> apInfoDispatcher,
            StaticDataProvider staticData) {
        super(apInfoDispatcher);
        this.baseApInfo = baseApInfo;
        this.staticData = staticData;
    }

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
        baseApInfo.setExternalIds(externalIds);
    }
}
