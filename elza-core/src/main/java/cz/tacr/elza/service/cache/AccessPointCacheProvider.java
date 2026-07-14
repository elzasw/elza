package cz.tacr.elza.service.cache;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Provides access points from the cache with a bounded in-memory LRU cache.
 *
 * A single instance is intended to be used within one processing scope (e.g. one output
 * generation) and by a single thread - the underlying cache is not synchronized.
 *
 * Each call to {@link AccessPointCacheService#findCachedAccessPoint(Integer)} triggers a DB read
 * and a full JSON deserialization of the whole access point, so the same entity referenced many
 * times (typical for outputs applying access restrictions) would otherwise be loaded repeatedly.
 * The cache keeps recently used access points to avoid that. It is bounded to
 * {@link #MAX_CACHE_SIZE} entries and evicts the least recently used entry when the limit is
 * exceeded, so memory stays bounded even for funds referencing a large number of entities.
 */
public class AccessPointCacheProvider {

    /**
     * Maximum number of access points kept in memory at once.
     */
    public static final int MAX_CACHE_SIZE = 1000;

    private final AccessPointCacheService apcService;

    private final Map<Integer, CachedAccessPoint> cache;

    public AccessPointCacheProvider(AccessPointCacheService apcService) {
        this(apcService, MAX_CACHE_SIZE);
    }

    public AccessPointCacheProvider(AccessPointCacheService apcService, final int maxCacheSize) {
        this.apcService = apcService;
        // access-ordered map -> iteration/eviction order follows last access, giving LRU eviction
        this.cache = new LinkedHashMap<>(16, 0.75f, true) {
            private static final long serialVersionUID = 1L;

            @Override
            protected boolean removeEldestEntry(Map.Entry<Integer, CachedAccessPoint> eldest) {
                return size() > maxCacheSize;
            }
        };
    }

    public CachedAccessPoint get(Integer accessPointId) {
        // the map is access-ordered, so get() promotes an existing entry to the
        // most-recently-used position and protects a repeatedly used entity from eviction
        CachedAccessPoint cached = cache.get(accessPointId);
        if (cached != null) {
            return cached;
        }
        CachedAccessPoint loaded = apcService.findCachedAccessPoint(accessPointId);
        // misses are not cached so they can be resolved once the entity becomes available
        if (loaded != null) {
            cache.put(accessPointId, loaded);
        }
        return loaded;
    }

}
