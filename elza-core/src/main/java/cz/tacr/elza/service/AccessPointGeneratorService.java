package cz.tacr.elza.service;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.Nullable;
import javax.persistence.EntityManager;
import javax.transaction.Transactional;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.google.common.eventbus.Subscribe;
import cz.tacr.elza.common.TaskExecutor;
import cz.tacr.elza.core.ResourcePathResolver;
import cz.tacr.elza.domain.*;
import cz.tacr.elza.drools.service.ModelFactory;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.repository.*;
import cz.tacr.elza.service.event.CacheInvalidateEvent;
import cz.tacr.elza.service.eventnotification.EventFactory;
import cz.tacr.elza.service.eventnotification.events.EventType;
import cz.tacr.elza.service.vo.AccessPoint;
import cz.tacr.elza.service.vo.Name;

/**
 * Serviska pro generování.
 */
@Service
public class AccessPointGeneratorService {

    private static final Logger logger = LoggerFactory.getLogger(AccessPointGeneratorService.class);

    public static final String ITEMS = "ITEMS";
    public static final String AP = "AP";

    private final AccessPointDataService apDataService;
    private final ApAccessPointRepository apRepository;
    private final ApplicationContext appCtx;
    private final AccessPointService accessPointService;

    private final TaskExecutor taskExecutor = new TaskExecutor(1);
    private final BlockingQueue<ApQueueItem> queue = new LinkedBlockingQueue<>();

    @Autowired
    public AccessPointGeneratorService(final AccessPointDataService apDataService,
                                       final ApAccessPointRepository apRepository,
                                       final ApplicationContext appCtx,
                                       final AccessPointService accessPointService) {
        this.apDataService = apDataService;
        this.apRepository = apRepository;
        this.appCtx = appCtx;
        this.accessPointService = accessPointService;
        this.taskExecutor.addTask(new AccessPointGeneratorThread());
        this.taskExecutor.start();
    }

    /**
     * Provede revalidaci AP, které nebyly dokončeny před restartem serveru.
     */
    public void restartQueuedAccessPoints() {
        Set<Integer> accessPointIds = apRepository.findInitAccessPointIds();
        for (Integer accessPointId : accessPointIds) {
            generateAsyncAfterCommit(accessPointId, null);
        }
    }

    /**
     * Třída pro zpracování požadavků pro asynchronní zpracování.
     */
    private class AccessPointGeneratorThread implements Runnable {
        @Override
        public void run() {
            while (true) {
                ApQueueItem item = null;
                try {
                    item = queue.take();
                    appCtx.getBean(AccessPointGeneratorService.class).processAsyncGenerate(item.getAccessPointId(), item.getChangeId());
                } catch (InterruptedException e) {
                    logger.info("Closing generator", e);
                    break;
                } catch (Exception e) {
                    logger.error("Process generate fail on accessPointId: {}", item.getAccessPointId(), e);
                }
            }
        }
    }

    /**
     * Zpracování požadavku AP.
     *
     * @param accessPointId identifikátor přístupového bodu
     * @param changeId      identifikátor změny
     */
    @Transactional
    public void processAsyncGenerate(final Integer accessPointId, final Integer changeId) {
        ApAccessPoint accessPoint = accessPointService.getAccessPointInternal(accessPointId);
        ApState apState = accessPointService.getState(accessPoint);
        ApChange apChange = /*changeId != null
                            ? apChangeRepository.findOne(changeId)
                            :*/ apDataService.createChange(ApChange.Type.AP_REVALIDATE);
        logger.info("Asynchronní zpracování AP={} ApChache={}", accessPointId, apChange.getChangeId());
//        generateAndSetResult(apState, apChange);
        logger.info("Asynchronní zpracování AP={} ApChache={} - END - State={}", accessPointId, apChange.getChangeId(), accessPoint.getState());
    }

    /**
     * Provede přidání AP do fronty pro přegenerování/validaci po dokončení aktuální transakce.
     * V případě, že ve frontě již AP je, nepřidá se.
     *
     * @param accessPointId identifikátor přístupového bodu
     * @param changeId      identifikátor změny
     */
    public void generateAsyncAfterCommit(final Integer accessPointId,
                                         @Nullable final Integer changeId) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
            @Override
            public void afterCommit() {
                ApQueueItem item = new ApQueueItem(accessPointId, changeId);
                synchronized (queue) {
                    if (!queue.contains(item)) {
                        try {
                            queue.put(item);
                        } catch (InterruptedException e) {
                            logger.error("Fail insert AP to queue", e);
                        }
                    }
                }
            }
        });
    }

    /**
     * Položka ve frontě na zpracování.
     */
    private class ApQueueItem {

        private Integer accessPointId;
        private Integer changeId;

        public ApQueueItem(final Integer accessPointId, final Integer changeId) {
            this.accessPointId = accessPointId;
            this.changeId = changeId;
        }

        public Integer getAccessPointId() {
            return accessPointId;
        }

        public Integer getChangeId() {
            return changeId;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ApQueueItem that = (ApQueueItem) o;
            return Objects.equals(accessPointId, that.accessPointId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(accessPointId);
        }
    }

}
