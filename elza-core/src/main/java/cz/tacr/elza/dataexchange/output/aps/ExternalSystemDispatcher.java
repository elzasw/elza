package cz.tacr.elza.dataexchange.output.aps;

import org.apache.commons.lang3.Validate;

import cz.tacr.elza.dataexchange.output.loaders.LoadDispatcher;
import cz.tacr.elza.domain.RegExternalSystem;
import cz.tacr.elza.domain.RegRecord;

/**
 *  External system dispatcher for specified access point.
 */
public class ExternalSystemDispatcher implements LoadDispatcher<RegExternalSystem> {

    private final RegRecord ap;

    public ExternalSystemDispatcher(RegRecord ap) {
        this.ap = ap;
    }

    @Override
    public void onLoadBegin() {
    }

    @Override
    public void onLoad(RegExternalSystem result) {
        ap.setExternalSystem(result);
    }

    @Override
    public void onLoadEnd() {
        Validate.notNull(ap.getExternalSystem());
    }
}
