package cz.tacr.elza.domain;

import org.apache.commons.lang3.StringUtils;

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
        String name = apName.getName();
        if (StringUtils.isEmpty(name)) {
            return null;
        }
        StringBuilder sb = new StringBuilder(name);
        String cmpl = apName.getComplement();
        if (StringUtils.isNotEmpty(cmpl)) {
            sb.append(" (").append(cmpl).append(')');
        }
        return sb.toString();
    }
}
