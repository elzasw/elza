package cz.tacr.elza.dataexchange.output.aps;

import org.apache.commons.lang.Validate;

import cz.tacr.elza.dataexchange.output.loaders.LoadDispatcher;
import cz.tacr.elza.dataexchange.output.loaders.NestedLoadDispatcher;
import cz.tacr.elza.domain.ApDescription;

public class DescriptionDispatcher extends NestedLoadDispatcher<ApDescription> {

    private final ApInfoImpl apInfo;

    private ApDescription description;

    public DescriptionDispatcher(ApInfoImpl apInfo, LoadDispatcher<ApInfoImpl> apInfoDispatcher) {
        super(apInfoDispatcher);
        this.apInfo = apInfo;
    }

    @Override
    public void onLoad(ApDescription result) {
        Validate.isTrue(description == null);
        description = result;
    }

    @Override
    protected void onCompleted() {
        apInfo.setDesc(description);
    }
}
