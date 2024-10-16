package cz.tacr.elza.domain;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cz.tacr.elza.service.AccessPointService;

public class ApFulltextProviderImpl implements ApFulltextProvider {

	private final AccessPointService apService;

    Logger log = LoggerFactory.getLogger(ApFulltextProviderImpl.class);

    public ApFulltextProviderImpl(final AccessPointService apService) {
        this.apService = apService;
    }

    @Override
    public String getFulltext(ApAccessPoint accessPoint) {
        // Fulltext can be generated only for non deleted accessPoints
        ApState apState = apService.getStateInternal(accessPoint.getAccessPointId());
        if (apState.getDeleteChangeId() != null) {
            return null;
        }
        ApIndex index = apService.findPreferredPartIndex(accessPoint);
        if (index == null) {
            return null;
        }

        return index.getIndexValue();
    }

}
