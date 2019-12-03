package cz.tacr.elza.domain;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cz.tacr.elza.repository.ApNameRepository;

public class ApFulltextProviderImpl implements ApFulltextProvider {

    private final ApNameRepository apNameRepository;
    
    Logger log = LoggerFactory.getLogger(ApFulltextProviderImpl.class);

    public ApFulltextProviderImpl(ApNameRepository apNameRepository) {
        this.apNameRepository = apNameRepository;
    }
    
    @Override
    public String getFulltext(ApAccessPoint accessPoint) {
        // Fulltext can be generated only for non deleted accessPoints
        if (accessPoint.getDeleteChangeId() != null) {
            return null;
        }

        ApName prefName = apNameRepository.findPreferredNameByAccessPoint(accessPoint);
        if (prefName == null) {
            log.error("AccessPoint without preferred name, apId={}", accessPoint.getAccessPointId());
            return null;
        }
        return createFulltext(prefName);
    }
    
    public static String createFulltext(ApName apName) {
        return apName.getFullName();
    }
}
