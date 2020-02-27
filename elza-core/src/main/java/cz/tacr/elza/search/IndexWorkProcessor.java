package cz.tacr.elza.search;

import cz.tacr.elza.domain.SysIndexWork;
import cz.tacr.elza.service.IndexWorkService;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.*;

/**
 * Listener nad tabulkou {@code sys_index_work} - zpracovava frontu pozadavku na preindexovani entit v Hibernate Search.
 */
@Component
public class IndexWorkProcessor implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(IndexWorkProcessor.class);

    // --- services ---

    private final IndexWorkService indexWorkService;

    private final SearchIndexService searchWorkIndexService;

    // --- fields ---

    private final ThreadPoolTaskExecutor taskExecutor;

    @Value("${elza.hibernate.index.batch_size:100}")
    private int batchSize = 100;

    private volatile boolean enabled = true;

    /**
     * temporary pause processing
     */
    private volatile boolean stop = false;

    private volatile boolean suspend = false;

    private volatile Thread manager = null;

    private final Object lock = new Object();

    // --- constructor ---

    @Autowired
    public IndexWorkProcessor(
            IndexWorkService indexWorkService,
            SearchIndexService searchWorkIndexService,
            @Qualifier("threadPoolTaskExecutorHS") ThreadPoolTaskExecutor taskExecutor) {

        this.indexWorkService = indexWorkService;
        this.searchWorkIndexService = searchWorkIndexService;
        this.taskExecutor = taskExecutor;
    }

    // --- methods ---

    /**
     * Nastartuje hlavni thread - zavolat jednou po startu aplikace
     */
    public synchronized void startIndexing() {

        if (this.manager != null) {
            throw new IllegalStateException("Already running");
        }

        indexWorkService.clearStartTime();

        this.enabled = true;


        this.manager = new Thread(this, "IndexWorkProcessor");
        this.manager.start();
    }

    /**
     * Zastavi hlavni thread - zavolat jednou pri ukoncovani aplikace
     */
    public synchronized void stopIndexing() {

        this.enabled = false;

        synchronized (lock) {
            lock.notifyAll();
        }
        waitForCompleteTasks();
    }

    /**
     * Pozastavi zpracovani - povolit zpracovani metodou {@link IndexWorkProcessor#resumeIndexing()}.
     *
     * @see IndexWorkProcessor#resumeIndexing()
     */
    public void suspendIndexing() {
        this.suspend = true;
        this.stop = true;
        waitForCompleteTasks();
        logger.info("Hibernate search index processor - suspended");
    }

    /**
     * Vyčkání na dokončení všech vláken s běžícím přeindexováním.
     */
    private void waitForCompleteTasks() {
        ThreadPoolExecutor executor = taskExecutor.getThreadPoolExecutor();
        while (executor.getTaskCount() != executor.getCompletedTaskCount()) {
            logger.warn("Waiting for complete tasks {}/{}", executor.getCompletedTaskCount(), executor.getTaskCount());
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                logger.error("Interrupt thread", e);
            }
        }
    }

    /**
     * Pokracuje ve zpracovani po {@link IndexWorkProcessor#suspendIndexing()}.
     *
     * @see IndexWorkProcessor#suspendIndexing()
     */
    public void resumeIndexing() {
        this.stop = false;
        this.suspend = false;
        synchronized (lock) {
            lock.notifyAll();
        }
        logger.info("Hibernate search index processor - resumed");
    }

    /**
     * Notifikuje procesor o tom, ze ve fronte jsou nove zpravy.
     */
    @Scheduled(fixedRateString = "${elza.hibernate.index.refresh_rate:60000}")
    public void notifyIndexing() {
        this.suspend = false;
        if (!this.stop) {
            synchronized (lock) {
                lock.notifyAll();
            }
        }
    }

    public void run() {

        logger.info("Hibernate search index processor - started");

        try {

            while (true) {

                synchronized (lock) {
                    try {
                        while (this.suspend || this.stop) {

                            if (!this.enabled) {
                                return;
                            }

                            lock.wait();
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        logger.error("Hibernate search index processor - processor thread interrupted");
                    }
                }

                try {

                    Page<SysIndexWork> workPage;

                    do {

                        if (!this.enabled) {
                            return;
                        }

                        if (this.stop) {
                            break;
                        }

                        // pockame, az bude volny thread, aby se worky zbytecne nehromadili ve fronte a nezamykaly v DB
                        /*
                        if (taskExecutor.getActiveCount() > 2 * taskExecutor.getCorePoolSize()) {
                            logger.debug("Hibernate search index processor - no thread available, indexing delayed");
                            break;
                        }
                        */

                        workPage = indexWorkService.findAllToIndex(new PageRequest(0, taskExecutor.getCorePoolSize() * batchSize));

                        if (workPage.hasContent()) {

                            indexAll(workPage.getContent());
                        }

                    } while (workPage.hasNext());

                } catch (Exception e) {

                    logger.error("Hibernate search index processor error", e);

                } finally {

                    this.suspend = true;
                }
            }

        } finally {

            // konec zpracovani
            this.manager = null;

            logger.info("Hibernate search index processor - finished");
        }
    }

    protected void indexAll(List<SysIndexWork> workList) {

        int size = workList.size();

        if (size > 0) {

            logger.debug("Hibernate search index processor - indexing of " + size + " entries in batches of " + batchSize);

            int start = 0;
            while (start < size) {
                int end = start + batchSize;
                if (end > size) {
                    end = size;
                }
                indexBatch(workList.subList(start, end));
                start = end;
            }
        }
    }

    protected void indexBatch(List<SysIndexWork> workList) {

        if (CollectionUtils.isEmpty(workList)) {
            logger.debug("Hibernate search index processor - nothing to update");
            return;
        }

        List<Long> workIdList = workList.stream().map(work -> work.getIndexWorkId()).collect(Collectors.toList());

        indexWorkService.updateStartTime(workIdList);

        Set<Integer> entityIdList = workList.stream().map(work -> work.getEntityId()).collect(Collectors.toSet());

        taskExecutor.submit(() -> {

            processBatch(workList, workIdList);

        });
    }

    protected void processBatch(List<SysIndexWork> workList, final List<Long> workIdList) {
        try {

            searchWorkIndexService.processBatch(workList);
            indexWorkService.delete(workIdList);

        } catch (Exception e) {
            StringBuilder sb = new StringBuilder(256);
            workList.stream().collect(groupingBy(work -> work.getEntityClass(), mapping(work -> work.getEntityId().toString(), joining(", ", "[", "]"))))
                    .forEach((entityClass, list) -> {
                        if (sb.length() > 0) sb.append(", ");
                        sb.append(entityClass.getName() + " " + list);
                    });
            logger.error("Hibernate search index processor - error processing a batch: " + sb, e);
            throw e;
        }
    }
}
