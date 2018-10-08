package cz.tacr.elza.search;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import cz.tacr.elza.domain.ArrSearchWork;
import cz.tacr.elza.service.SearchWorkService;

/**
 * @author <a href="mailto:stepan.marek@coreit.cz">Stepan Marek</a>
 */
@Component
@ConditionalOnProperty(prefix = "elza.hibernate.index", name = "enabled", havingValue = "true", matchIfMissing = true)
public class SearchWorkProcessor {

    private static final Logger logger = LoggerFactory.getLogger(SearchWorkProcessor.class);

    @Autowired
    private SearchWorkService searchWorkService;

    @Autowired
    private SearchWorkIndexService searchWorkIndexService;

    @Autowired
    @Qualifier("threadPoolTaskExecutorHS")
    private ThreadPoolTaskExecutor taskExecutor;

    @Value("${elza.hibernate.index.batch_size:200}")
    private int batchSize;

    @Scheduled(fixedRateString = "${elza.hibernate.index.refresh_rate:1000}")
    public void indexAll() {

        for (Map.Entry<Class, List<ArrSearchWork>> entry : searchWorkService.groupByEntityClass().entrySet()) {

            Class entityClass = entry.getKey();
            List<ArrSearchWork> workList = entry.getValue();

            int size = workList.size();

            logger.debug("Index " + entityClass + ": indexing of " + size + " entries in batches of " + batchSize);

            int start = 0;
            while (start < size) {
                int end = start + batchSize;
                if (end > size) {
                    end = size;
                }
                indexBatch(entityClass, workList.subList(start, end));
                start = end;
            }
        }
    }

    public void indexBatch(Class entityClass, List<ArrSearchWork> workList) {

        if (CollectionUtils.isEmpty(workList)) {
            logger.debug("Index " + entityClass + " - nothing to update");
            return;
        }

        List<Integer> workIdList = workList.stream().map(w -> w.getSearchWorkId()).collect(Collectors.toList());

        searchWorkService.updateStartTime(workIdList);

        Set<Integer> entityIdList = workList.stream().map(w -> w.getEntityId()).collect(Collectors.toSet());

        taskExecutor.submit(() -> {

            searchWorkIndexService.processBatch(entityClass, entityIdList);

            searchWorkService.delete(workIdList);
        });
    }
}
