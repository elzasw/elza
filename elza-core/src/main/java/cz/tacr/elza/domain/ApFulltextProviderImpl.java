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
    	ApState apState = apService.getState(accessPoint);
        if (apState.getDeleteChangeId() != null) {
            return null;
        }
        
        ApName prefName = apService.getPreferredAccessPointName(accessPoint);
        if (prefName == null) {
            return null;
        }
        return createFulltext(prefName);
    }
    
    public static String createFulltext(ApName apName) {
        return apName.getFullName();
    }
}
