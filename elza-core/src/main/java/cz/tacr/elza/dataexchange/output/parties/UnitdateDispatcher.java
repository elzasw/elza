package cz.tacr.elza.dataexchange.output.parties;

import org.apache.commons.lang3.Validate;

import cz.tacr.elza.dataexchange.output.loaders.LoadDispatcher;
import cz.tacr.elza.dataexchange.output.loaders.NestedLoadDispatcher;
import cz.tacr.elza.domain.ParUnitdate;

public abstract class UnitdateDispatcher extends NestedLoadDispatcher<ParUnitdate> {

    private ParUnitdate unitdate;

    public UnitdateDispatcher(LoadDispatcher<?> parentDispatcher) {
        super(parentDispatcher);
    }

    public ParUnitdate getUnitdate() {
        return unitdate;
    }

    @Override
    public void onLoad(ParUnitdate result) {
        Validate.isTrue(unitdate == null);
        unitdate = result;
    }
}
