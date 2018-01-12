package cz.tacr.elza.service;

import com.google.common.eventbus.EventBus;

import cz.tacr.elza.core.security.AuthMethod;
import cz.tacr.elza.domain.UsrPermission;
import cz.tacr.elza.service.event.CacheInvalidateEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Serviska pro správu všech cache.
 *
 * @author Martin Šlapa
 * @since 01.12.2016
 */
@Service
public class CacheService {

    @Autowired
    private EventBus eventBus;

    /**
     * Resetuje všechny (navázané) cache v jádru.
     */
    @AuthMethod(permission = {UsrPermission.Permission.ADMIN})
    public void resetAllCache() {
        resetCache(CacheInvalidateEvent.Type.ALL);
    }

    /**
     * Provede reset požadovaných cache.
     *
     * @param types typy cache, které se mají invalidovat
     */
    public void resetCache(final CacheInvalidateEvent.Type ...types) {
        CacheInvalidateEvent cacheInvalidateEvent;
        if (types == null) {
            cacheInvalidateEvent = new CacheInvalidateEvent();
        } else {
            cacheInvalidateEvent = new CacheInvalidateEvent(types);
        }
        eventBus.post(cacheInvalidateEvent);
    }

}
