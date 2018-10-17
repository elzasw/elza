package cz.tacr.elza.search;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import cz.tacr.elza.domain.SysIndexWork;
import cz.tacr.elza.service.IndexWorkService;

import static java.util.stream.Collectors.*;

/**
 * Listener nad tabulkou {@code sys_index_work} - zpracovava frontu pozadavku na preindexovani entit v Hibernate Search.
 */
@Component
@ConditionalOnProperty(prefix = "elza.hibernate.index", name = "enabled", havingValue = "true", matchIfMissing = true)
public class IndexWorkProcessor {

    private static final Logger logger = LoggerFactory.getLogger(IndexWorkProcessor.class);

    // --- services ---

    private final IndexWorkService indexWorkService;

    private final SearchIndexService searchWorkIndexService;

    private final ThreadPoolTaskExecutor taskExecutor;

    // --- fields ---

    @Value("${elza.hibernate.index.batch_size:100}")
    private int batchSize;

    /**
     * disable processing in runtime
     */
    private volatile boolean disabled = false;

    // --- getters/setters ---

    public boolean isDisabled() {
        return disabled;
    }

    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
    }

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

    @Scheduled(fixedRateString = "${elza.hibernate.index.refresh_rate:1000}")
    public void indexAll() {

        if (!disabled) {

            // pockame, az bude volny thread, aby se worky zbytecne nehromadili ve fronte a nezamykaly v DB
            if (taskExecutor.getCorePoolSize() > taskExecutor.getActiveCount()) {

                Page<SysIndexWork> workPage;

                do {

                    workPage = indexWorkService.findAllToIndex(new PageRequest(0, taskExecutor.getCorePoolSize() * batchSize));

                    if (workPage.hasContent()) {

                        List<SysIndexWork> workList = workPage.getContent();

                        indexAll(workList);
                    }

                } while (!disabled && workPage.hasNext());

            } else {
                logger.debug("Hibernate search index - no thread available, indexing delayed");
            }
        }
    }

    protected void indexAll(List<SysIndexWork> workList) {

        int size = workList.size();

        if (size > 0) {

            logger.debug("Hibernate search index - indexing of " + size + " entries in batches of " + batchSize);

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
            logger.debug("Hibernate search index - nothing to update");
            return;
        }

        List<Long> workIdList = workList.stream().map(work -> work.getIndexWorkId()).collect(Collectors.toList());

        indexWorkService.updateStartTime(workIdList);

        Set<Integer> entityIdList = workList.stream().map(work -> work.getEntityId()).collect(Collectors.toSet());

        taskExecutor.submit(() -> {

            processBatch(workList);

            indexWorkService.delete(workIdList);
        });
    }

    protected void processBatch(List<SysIndexWork> workList) {
        try {

            searchWorkIndexService.processBatch(workList);

        } catch (Exception e) {
            StringBuilder sb = new StringBuilder(256);
            workList.stream().collect(groupingBy(work -> work.getEntityClass(), mapping(work -> work.getEntityId().toString(), joining(", ", "[", "]"))))
                    .forEach((entityClass, list) -> {
                        if (sb.length() > 0) sb.append(", ");
                        sb.append(entityClass.getName() + " " + list);
                    });
            logger.error("Hibernate search index - error processing a batch: " + sb, e);
            throw e;
        }
    }
}
