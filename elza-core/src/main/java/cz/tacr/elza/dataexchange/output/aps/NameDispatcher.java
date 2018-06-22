package cz.tacr.elza.dataexchange.output.aps;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.Validate;

import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.dataexchange.output.loaders.LoadDispatcher;
import cz.tacr.elza.dataexchange.output.loaders.NestedLoadDispatcher;
import cz.tacr.elza.domain.ApName;
import cz.tacr.elza.domain.ApNameType;

public class NameDispatcher extends NestedLoadDispatcher<ApName> {

    private final List<ApName> names = new ArrayList<>();

    private final ApInfoImpl apInfo;

    private final StaticDataProvider staticData;

    public NameDispatcher(ApInfoImpl apInfo, LoadDispatcher<ApInfoImpl> apInfoDispatcher,
            StaticDataProvider staticData) {
        super(apInfoDispatcher);
        this.apInfo = apInfo;
        this.staticData = staticData;
    }

    @Override
    public void onLoad(ApName result) {
        // init name type
        ApNameType type = staticData.getApNameTypeById(result.getNameTypeId());
        Validate.notNull(type);
        result.setNameType(type);
        // set result
        names.add(result);
    }

    @Override
    protected void onCompleted() {
        apInfo.setNames(names);
    }
}