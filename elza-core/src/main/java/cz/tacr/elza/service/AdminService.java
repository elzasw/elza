package cz.tacr.elza.service;

import java.util.concurrent.Future;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import com.google.common.eventbus.EventBus;
import cz.tacr.elza.annotation.AuthMethod;
import cz.tacr.elza.api.UsrPermission;
import cz.tacr.elza.service.event.CacheInvalidateEvent;
import org.hibernate.search.MassIndexer;
import org.hibernate.search.jpa.FullTextEntityManager;
import org.hibernate.search.jpa.Search;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.ResponseBody;

import cz.tacr.elza.search.IndexerProgressMonitor;

/**
 *
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 19. 1. 2016
 */
@Component
public class AdminService {

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private IndexerProgressMonitor indexerProgressMonitor;

    @Autowired
    private EventBus eventBus;

    private Future<?> indexerStatus;

    /** Přeindexuje všechna data. */
    @AuthMethod(permission = {UsrPermission.Permission.ADMIN})
    public void reindex() {
        FullTextEntityManager fullTextEntityManager = Search.getFullTextEntityManager(entityManager);

        if (isIndexingRunning()) {
            return;
        }

        MassIndexer createIndexer = fullTextEntityManager.createIndexer();
        createIndexer.progressMonitor(indexerProgressMonitor);
        indexerStatus = createIndexer.start();
    }

    /**
     * Zjistí zda běží indexování.
     *
     * @return true pokud běží indexování, jinak false
     */
    @ResponseBody
    @AuthMethod(permission = {UsrPermission.Permission.ADMIN})
    public boolean isIndexingRunning() {
        if (indexerStatus != null) {
            return !indexerStatus.isDone();
        }

        return false;
    }

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
