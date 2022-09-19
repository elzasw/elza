package cz.tacr.elza.service.cache;

public class AccessPointCacheProvider {

    private AccessPointCacheService apcService;

    public AccessPointCacheProvider(AccessPointCacheService apcService) {
        this.apcService = apcService;
    }

    public CachedAccessPoint get(Integer accessPointId) {
        return apcService.findCachedAccessPoint(accessPointId);
    }

}
