package cz.tacr.elza.service;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.SmartLifecycle;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.repository.DataRepository;
import cz.tacr.elza.repository.vo.DataIdTypeId;
import jakarta.transaction.Transactional;

@Service
public class CleanupWorker implements SmartLifecycle {

    static final Logger log = LoggerFactory.getLogger(CleanupWorker.class);

    private enum ThreadStatus {
        RUNNING, STOP_REQUEST, STOPPED
    }

    private int batchSize = 1_000;

    @Autowired
    private DataRepository dataRepository;

    @Value("${elza.cleanup.enabled:true}")
    private boolean cleanupEnable;

    @Value("${elza.cleanup.wait:60}")
    private int waitMin;

    private ThreadStatus status = ThreadStatus.STOPPED;

    private final Object lock = new Object();

    /**
     * Delete all arr_data that are not related to tables:
     * - ap_item
     * - ap_rev_item
     * - arr_item
     */
    @Transactional
    private void deleteData() {
    	log.debug("Deleting {} from add_data...", batchSize);
    	List<DataIdTypeId> data = dataRepository.findUnlinkedDataIds(PageRequest.of(0, batchSize));
    	// data -> Map<typeId, List<dataId>>
    	Map<Integer, List<Integer>> dataTypeIdMap = data.stream().collect(
    			Collectors.groupingBy(DataIdTypeId::getTypeId, Collectors.mapping(DataIdTypeId::getDataId, Collectors.toList())));

    	for (Entry<Integer, List<Integer>> entry : dataTypeIdMap.entrySet()) {
			DataType dataType = DataType.fromId(entry.getKey());
    		dataType.getRepository().deleteAllById(entry.getValue());
        	log.debug("Deleted {} from {}.", entry.getValue().size(), dataType.getEntity().getStorageTable());
    	}
    }

    public void run() {
        synchronized (lock) {
            while (status == ThreadStatus.RUNNING) {
            	log.debug("Looking for data to delete...");
                try {
                	while (!dataRepository.findUnlinkedDataIds(PageRequest.of(0, 1)).isEmpty()) {
                		deleteData();
                	}
                } catch (Exception ex) {
                    log.error("Error in cleanup process. ", ex);
                }
                try {
                    // wake up 1 hour by default to check for changes
                    lock.wait(1000 * 60 * waitMin);
                } catch (InterruptedException e) {
                    log.error(e.getMessage());
                    break;
                }
            }
            status = ThreadStatus.STOPPED;
            lock.notifyAll();
        }
    }

    @Override
    public void start() {
        log.info("Starting service...");
        if (!cleanupEnable) {
            log.info("Service is disabled.");
            status = ThreadStatus.STOPPED;
            return;
        }
        status = ThreadStatus.RUNNING;
        new Thread(() -> {
            run();
        }).start();
        log.info("Service started.");
    }

    @Override
    public void stop() {
        log.info("Stopping service...");
        Validate.isTrue(status == ThreadStatus.RUNNING);
        status = ThreadStatus.STOP_REQUEST;

        synchronized (lock) {
            lock.notifyAll();
            while (status != ThreadStatus.STOPPED) {
                try {
                    lock.wait(100);
                } catch (InterruptedException e) {
                    log.error(e.getMessage());
                    break;
                }
            }
        }
        log.info("Service is stopped.");
    }

    @Override
    public boolean isRunning() {
        return status == ThreadStatus.RUNNING;
    }

}