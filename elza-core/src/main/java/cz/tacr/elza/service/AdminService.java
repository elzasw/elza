package cz.tacr.elza.service;

import java.util.concurrent.Future;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.hibernate.search.MassIndexer;
import org.hibernate.search.jpa.FullTextEntityManager;
import org.hibernate.search.jpa.Search;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 *
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 19. 1. 2016
 */
@Component
public class AdminService implements ApplicationListener<ContextRefreshedEvent> {

    @PersistenceContext
    private EntityManager entityManager;

    private Future<?> indexerStatus;

    /** Přeindexuje všechna data. */
    public void reindex() {
        FullTextEntityManager fullTextEntityManager = Search.getFullTextEntityManager(entityManager);

        if (isIndexingRunning()) {
            return;
//            indexerStatus.cancel(true);
        }

        MassIndexer createIndexer = fullTextEntityManager.createIndexer();
//        MassIndexerProgressMonitor monitor = new SimpleIndexingProgressMonitor();
//        createIndexer.progressMonitor(monitor);
        indexerStatus = createIndexer.start();
    }

    /**
     * Zjistí zda běží indexování.
     *
     * @return true pokud běží indexování, jinak false
     */
    @ResponseBody
    public boolean isIndexingRunning() {
        if (indexerStatus != null) {
            return !indexerStatus.isDone();
        }

        return false;
    }

    // TODO vanek odstranit až bude klient na indexaci
    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        reindex();
    }
}
