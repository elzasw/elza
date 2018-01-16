package cz.tacr.elza.service;

import java.util.concurrent.Future;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.hibernate.search.MassIndexer;
import org.hibernate.search.jpa.FullTextEntityManager;
import org.hibernate.search.jpa.Search;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.ResponseBody;

import cz.tacr.elza.core.security.AuthMethod;
import cz.tacr.elza.domain.UsrPermission;
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
}
