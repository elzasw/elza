package cz.tacr.elza.dataexchange.output.aps;

import cz.tacr.elza.domain.ApAccessPoint;
import cz.tacr.elza.domain.ApExternalSystem;
import cz.tacr.elza.service.vo.ApAccessPointData;
import org.apache.commons.lang3.Validate;

import cz.tacr.elza.dataexchange.output.loaders.LoadDispatcher;

/**
 *  External system dispatcher for specified access point.
 */
public class ExternalSystemDispatcher implements LoadDispatcher<ApExternalSystem> {

    private final ApAccessPoint ap;
    private final ApAccessPointData pointData;

    public ExternalSystemDispatcher(ApAccessPoint ap, ApAccessPointData pointData) {
        this.ap = ap;
        this.pointData = pointData;
    }

    @Override
    public void onLoadBegin() {
    }

    @Override
    public void onLoad(ApExternalSystem result) {
        pointData.setExternalSystem(result);
    }

    @Override
    public void onLoadEnd() {
        Validate.notNull(pointData.getExternalSystem());
    }
}
