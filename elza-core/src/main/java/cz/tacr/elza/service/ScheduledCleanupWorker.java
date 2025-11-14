package cz.tacr.elza.service;

import java.util.Collection;
import java.util.List;

import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import cz.tacr.elza.common.ObjectListIterator;
import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.repository.DataRepository;
import cz.tacr.elza.repository.vo.DataIdTypeId;
import jakarta.transaction.Transactional;

/**
 * Delete data that are not related to any table
 */
@Service
@Lazy(false) // forced bean creation
public class ScheduledCleanupWorker {

    static final Logger log = LoggerFactory.getLogger(ScheduledCleanupWorker.class);

    @Value("${elza.cleanup.maxBatch:150000}")
    private int maxBatchSize;

    @Autowired
    private DataRepository dataRepository;

    @Autowired
    protected PlatformTransactionManager tm;

    /**
     * Delete all arr_data that are not related to tables:
     * - ap_item
     * - ap_rev_item
     * - arr_item
     */
    private void deleteData(Collection<DataIdTypeId> data) {
    	log.debug("Deleting {} from add_data...", data.size());
    	// data -> Map<typeId, List<dataId>>
    	Map<Integer, List<Integer>> dataTypeIdMap = data.stream().collect(
    			Collectors.groupingBy(DataIdTypeId::getTypeId, Collectors.mapping(DataIdTypeId::getDataId, Collectors.toList())));

    	for (Entry<Integer, List<Integer>> entry : dataTypeIdMap.entrySet()) {
			DataType dataType = DataType.fromId(entry.getKey());
    		dataType.getRepository().deleteAllById(entry.getValue());
        	log.debug("Deleted {} from {}.", entry.getValue().size(), dataType.getEntity().getStorageTable());
    	}
    }

    // by default every day in 3:00
    @Scheduled(cron = "${elza.cleanup.cron:0 0 3 * * *}")
	public void scheduledCleanup() {
    	log.debug("Deleting from add_data started...");
		List<DataIdTypeId> dataIds;
    	try {
    		do {
    			dataIds = dataRepository.findUnlinkedDataIds(PageRequest.of(0, maxBatchSize));
		    	ObjectListIterator.forEachPage(dataIds, p -> {
		    		new TransactionTemplate(tm).executeWithoutResult(ts -> {
	                    deleteData(p);
	                });
	            });
    		} while (!dataIds.isEmpty());
    	} catch (Exception e) {
    		log.error("Error in cleanup process. ", e);
    	}
	}
}