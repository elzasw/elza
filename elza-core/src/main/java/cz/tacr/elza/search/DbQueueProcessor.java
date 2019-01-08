package cz.tacr.elza.search;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import cz.tacr.elza.exception.SystemException;
import org.hibernate.search.backend.IndexingMonitor;
import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.backend.spi.BackendQueueProcessor;
import org.hibernate.search.engine.integration.impl.ExtendedSearchIntegrator;
import org.hibernate.search.indexes.serialization.spi.LuceneWorkSerializer;
import org.hibernate.search.indexes.spi.IndexManager;
import org.hibernate.search.spi.WorkerBuildContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.ContextRefreshedEvent;

import cz.tacr.elza.service.IndexWorkService;

/**
 * Hibernate Search support - implementace Backend Queue Processor, ktera neindexuje okamzite,
 * ale uklada pozadavky na indexovani do fronty v DB tabulce {@code sys_index_work}.
 */
@SuppressWarnings("SpringJavaAutowiredMembersInspection")
public class DbQueueProcessor implements BackendQueueProcessor {

    private static final Logger logger = LoggerFactory.getLogger(DbQueueProcessor.class);

    @Autowired
    ApplicationContext applicationContext;

    // --- services ---

    private static IndexWorkService indexWorkService;
    private static IndexWorkProcessor indexWorkProcessor;

    // --- fields ---

    private Properties props;
    private IndexManager indexManager;
    private String indexName;
    private ExtendedSearchIntegrator searchIntegrator;

    // --- constructor ---

    public DbQueueProcessor() {
        ConfigurableApplicationContext confAppContext = (ConfigurableApplicationContext) applicationContext;
        confAppContext.addApplicationListener((ApplicationListener<ContextRefreshedEvent>) event -> {
            logger.debug("Spring application context init for " + DbQueueProcessor.class);
            AutowireCapableBeanFactory factory = event.getApplicationContext().getAutowireCapableBeanFactory();
            factory.autowireBean(DbQueueProcessor.this);
        });
    }

    /**
     * Doinicializace potøebných bean pøi startu.
     *
     * @param applicationContext
     */
    public static void startInit(final ApplicationContext applicationContext) {
        DbQueueProcessor.indexWorkService = applicationContext.getBean(IndexWorkService.class);
        DbQueueProcessor.indexWorkProcessor = applicationContext.getBean(IndexWorkProcessor.class);
    }

    // --- methods ---

    @Override
    public void initialize(Properties props, WorkerBuildContext context, IndexManager indexManager) {
        logger.debug("Search Integrator setup");
        this.props = props;
        this.indexManager = indexManager;
        this.indexName = indexManager.getIndexName();
        this.searchIntegrator = context.getUninitializedSearchIntegrator();
    }

    /**
     * Kontrola nainicialozování používaných bean.
     */
    private void checkBeans() {
        if (indexWorkService == null || indexWorkProcessor == null) {
            throw new SystemException("Nebyly inicializované závislé beany!");
        }
    }

    @Override
    public void close() {
    }

    @Override
    public void applyWork(List<LuceneWork> luceneWorkList, IndexingMonitor monitor) {
        checkBeans();

        // nektere tasky nemaji ID (napr. PurgeAllLuceneWork) - ty zpracujeme synchronne
        List<LuceneWork> syncList = new ArrayList<>(luceneWorkList.size());
        List<LuceneWork> asyncList = new ArrayList<>(luceneWorkList.size());
        for (LuceneWork luceneWork : luceneWorkList) {
            if (luceneWork.getId() == null) {
                syncList.add(luceneWork);
            } else {
                asyncList.add(luceneWork);
            }
        }

        if (!syncList.isEmpty()) {
            indexManager.performOperations(luceneWorkList, monitor);
        }
        if (!asyncList.isEmpty()) {
            indexWorkService.createIndexWork(indexName, asyncList);
            indexWorkProcessor.notifyIndexing();
        }
    }

    @Override
    public void applyStreamWork(LuceneWork luceneWork, IndexingMonitor monitor) {
        checkBeans();
        // nektere tasky nemaji ID (napr. PurgeAllLuceneWork) - ty zpracujeme synchronne
        if (luceneWork.getId() == null) {
            indexManager.performStreamOperation(luceneWork, monitor, false);
        } else {
            indexWorkService.createIndexWork(indexName, luceneWork);
            indexWorkProcessor.notifyIndexing();
        }
    }

    protected LuceneWorkSerializer getWorkSerializer() {
        return searchIntegrator.getWorkSerializer();
    }
}
