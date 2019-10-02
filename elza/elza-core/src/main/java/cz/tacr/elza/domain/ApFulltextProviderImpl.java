package cz.tacr.elza.domain;

import cz.tacr.elza.repository.ApNameRepository;

public class ApFulltextProviderImpl implements ApFulltextProvider {

    private final ApNameRepository apNameRepository;
    
    public ApFulltextProviderImpl(ApNameRepository apNameRepository) {
        this.apNameRepository = apNameRepository;
    }
    
    @Override
    public String getFulltext(ApAccessPoint accessPoint) {
        ApName prefName = apNameRepository.findPreferredNameByAccessPoint(accessPoint);
        return createFulltext(prefName);
    }
    
    public static String createFulltext(ApName apName) {
        return apName.getFullName();
    }
}
