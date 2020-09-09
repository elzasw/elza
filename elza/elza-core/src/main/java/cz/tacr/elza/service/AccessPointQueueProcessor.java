package cz.tacr.elza.service;

import cz.tacr.elza.domain.ApAccessPointQueueItem;
import cz.tacr.elza.repository.ApAccessPointQueueItemRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AccessPointQueueProcessor implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(AccessPointQueueProcessor.class);
    private volatile Thread manager = null;

    @Autowired
    private AccessPointGeneratorService accessPointGeneratorService;

    @Autowired
    private ApAccessPointQueueItemRepository accessPointQueueItemRepository;

    public void startValidating() {
        if(this.manager == null) {
            this.manager = new Thread(this,"AccessPointQueueProcessor");
            this.manager.start();
        }
    }

    @Override
    public void run() {
        try {
            while (true) {
                if(!accessPointQueueItemRepository.isQueuePopulated()) {
                    Thread.sleep(10000);
                } else {
                    revalidateRecords();
                }
            }
        } catch(Exception e) {
            logger.error("AccessPointQueueProcessor - processor thread error " + e.toString());
        }
    }

    public void revalidateRecords() {
        List<ApAccessPointQueueItem> queueItemList = accessPointQueueItemRepository.findAll();
        for (ApAccessPointQueueItem item : queueItemList) {
            accessPointGeneratorService.processAsyncGenerate(item);
        }
    }
}
