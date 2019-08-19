package cz.tacr.elza.dataexchange.output.aps;

import org.apache.commons.lang.Validate;

import cz.tacr.elza.dataexchange.output.loaders.LoadDispatcher;
import cz.tacr.elza.dataexchange.output.loaders.NestedLoadDispatcher;
import cz.tacr.elza.domain.ApDescription;

public class DescriptionDispatcher extends NestedLoadDispatcher<ApDescription> {

    // --- fields ---

    private final ApInfo apInfo;

    private ApDescription description;

    // --- constructor ---

    public DescriptionDispatcher(ApInfo apInfo, LoadDispatcher<ApInfo> apInfoDispatcher) {
        super(apInfoDispatcher);
        this.apInfo = apInfo;
    }

    // --- methods ---

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
