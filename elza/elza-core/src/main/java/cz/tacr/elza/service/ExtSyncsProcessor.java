package cz.tacr.elza.service;

import cz.tacr.elza.api.ApExternalSystemType;
import cz.tacr.elza.domain.ApExternalSystem;
import cz.tacr.elza.domain.ExtSyncsQueueItem;
import cz.tacr.elza.exception.ObjectNotFoundException;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.repository.ApExternalSystemRepository;
import cz.tacr.elza.repository.ExtSyncsQueueItemRepository;
import cz.tacr.elza.service.cam.CamService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ExtSyncsProcessor implements Runnable {

    @Autowired
    private ExtSyncsQueueItemRepository extSyncsQueueItemRepository;

    @Autowired
    private ApExternalSystemRepository apExternalSystemRepository;

    @Autowired
    private CamService camService;

    private static final Logger logger = LoggerFactory.getLogger(ExtSyncsProcessor.class);

    private volatile Thread asyncThread = null;

    private final Object lock = new Object();

    private static int QUEUE_CHECK_TIME_INTERVAL = 10000;

    public void startExtSyncs() {
        synchronized (lock) {
            if (this.asyncThread == null) {
                this.asyncThread = new Thread(this,"ExtSyncsProcessor");
                this.asyncThread.start();
            }
        }
    }


    @Override
    public void run() {
        try {
            while (true) {
                if(!isQueuePopulated()) {
                    Thread.sleep(QUEUE_CHECK_TIME_INTERVAL);
                } else {
                    processQueue();
                }
            }
        } catch(Exception e) {
            logger.error("ExtSyncsProcessor - processor thread error " + e.toString());
        }
    }

    private void processQueue() {
        Pageable pageable = PageRequest.of(0, 100);
        // find first items
        Page<ExtSyncsQueueItem> page = extSyncsQueueItemRepository.findByState(ExtSyncsQueueItem.ExtAsyncQueueState.NEW, pageable);

        if (page != null && page.getTotalElements() > 0) {
            List<ExtSyncsQueueItem> extSyncsQueueItems = page.getContent();

            for (ExtSyncsQueueItem extSyncsQueueItem : extSyncsQueueItems) {
                Integer apExternalSystemId = extSyncsQueueItem.getApExternalSystem().getExternalSystemId();
                ApExternalSystem apExternalSystem = apExternalSystemRepository.findById(apExternalSystemId)
                        .orElseThrow(() -> new ObjectNotFoundException("Externí systém neexistuje", BaseCode.ID_NOT_EXIST).setId(apExternalSystemId));
                if (apExternalSystem.getType() == ApExternalSystemType.CAM ||
                        apExternalSystem.getType() == ApExternalSystemType.CAM_UUID) {
                    extSyncsQueueItem.setState(ExtSyncsQueueItem.ExtAsyncQueueState.RUNNING);
                    extSyncsQueueItemRepository.save(extSyncsQueueItem);
                    camService.synchronizeExtItem(extSyncsQueueItem, apExternalSystem);
                }
            }
        }
    }

    private boolean isQueuePopulated() {
        return extSyncsQueueItemRepository.countByState(ExtSyncsQueueItem.ExtAsyncQueueState.NEW) > 0;
    }
}
