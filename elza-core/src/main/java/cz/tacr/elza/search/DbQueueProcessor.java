package cz.tacr.elza.search;

import java.util.List;
import java.util.Properties;

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
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;

import cz.tacr.elza.core.AppContext;
import cz.tacr.elza.service.IndexWorkService;

/**
 * Hibernate Search support - implementace Backend Queue Processor, ktera neindexuje okamzite,
 * ale uklada pozadavky na indexovani do fronty v DB tabulce {@code sys_index_work}.
 */
@SuppressWarnings("SpringJavaAutowiredMembersInspection")
public class DbQueueProcessor implements BackendQueueProcessor {

    private static final Logger logger = LoggerFactory.getLogger(DbQueueProcessor.class);

    // --- services ---

    @Autowired
    private IndexWorkService indexWorkService;

    // --- fields ---

    @Autowired
    private IndexWorkProcessor indexWorkProcessor;

    private Properties props;
    private IndexManager indexManager;
    private String indexName;
    private ExtendedSearchIntegrator searchIntegrator;

    // --- constructor ---

    public DbQueueProcessor() {
        AppContext.addApplicationListener((ApplicationListener<ContextRefreshedEvent>) event -> {
            logger.debug("Spring application context init for " + DbQueueProcessor.class);
            AutowireCapableBeanFactory factory = event.getApplicationContext().getAutowireCapableBeanFactory();
            factory.autowireBean(DbQueueProcessor.this);
        });
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

    @Override
    public void close() {
    }

    @Override
    public void applyWork(List<LuceneWork> workList, IndexingMonitor monitor) {
        indexWorkService.createIndexWork(indexName, workList);
        // indexManager.performOperations(workList, monitor);
        indexWorkProcessor.notifyIndexProcessor();
    }

    @Override
    public void applyStreamWork(LuceneWork work, IndexingMonitor monitor) {
        indexWorkService.createIndexWork(indexName, work);
        // indexManager.performStreamOperation(work, monitor, false);
        indexWorkProcessor.notifyIndexProcessor();
    }

    protected LuceneWorkSerializer getWorkSerializer() {
        return searchIntegrator.getWorkSerializer();
    }
}
