package cz.tacr.elza.dataexchange.output.aps;

import cz.tacr.elza.domain.ApExternalSystem;
import org.apache.commons.lang3.Validate;

import cz.tacr.elza.dataexchange.output.loaders.LoadDispatcher;
import cz.tacr.elza.domain.ApRecord;

/**
 *  External system dispatcher for specified access point.
 */
public class ExternalSystemDispatcher implements LoadDispatcher<ApExternalSystem> {

    private final ApRecord ap;

    public ExternalSystemDispatcher(ApRecord ap) {
        this.ap = ap;
    }

    @Override
    public void onLoadBegin() {
    }

    @Override
    public void onLoad(ApExternalSystem result) {
        ap.setExternalSystem(result);
    }

    @Override
    public void onLoadEnd() {
        Validate.notNull(ap.getExternalSystem());
    }
}
