package cz.tacr.elza.cam;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

import jakarta.transaction.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;

import cz.tacr.elza.domain.ApExternalSystem;
import cz.tacr.elza.metrics.ElzaMonitoringMetrics;
import cz.tacr.elza.repository.ApExternalSystemRepository;
import cz.tacr.elza.service.AccessPointConnectorService;
import cz.tacr.elza.service.event.ApExternalSystemEvent;

/**
 * Časovač pro synchronizace přístupových bodů s CAM.
 * Interval synchronizace se čte z ap_external_system.sync_delay; změna přes REST API
 * je promítnuta bez restartu skrz {@link ApExternalSystemEvent}.
 */
@Service
public class CamScheduler {

    private static final Logger log = LoggerFactory.getLogger(CamScheduler.class);

    @Autowired
    private AccessPointConnectorService apConnectService;

    @Autowired
    private ApExternalSystemRepository apExternalSystemRepository;

    @Autowired
    private ElzaMonitoringMetrics monitoringMetrics;

    @Autowired
    private TaskScheduler taskScheduler;

    private final Map<Integer, ScheduledFuture<?>> activeFutures = new ConcurrentHashMap<>();

    private volatile boolean enabled = false;

    @Transactional
    public synchronized void start() {
        enabled = true;
        for (ApExternalSystem sys : apExternalSystemRepository.findAll()) {
            scheduleFresh(sys);
        }
    }

    public synchronized void stop() {
        enabled = false;
        activeFutures.values().forEach(f -> f.cancel(false));
        activeFutures.clear();
    }

    /**
     * On create/update/delete of an AP external system, drop the running trigger for it (if any) and
     * re-read from DB. Handles: hot delay change, re-enable after 0/null, new system added, deletion.
     */
    @EventListener
    public synchronized void onExternalSystemChanged(ApExternalSystemEvent event) {
        if (!enabled) {
            return;
        }
        Integer sysId = event.getExternalSystem().getExternalSystemId();
        ScheduledFuture<?> existing = activeFutures.remove(sysId);
        if (existing != null) {
            existing.cancel(false);
        }
        apExternalSystemRepository.findById(sysId).ifPresent(this::scheduleFresh);
    }

    private void scheduleFresh(ApExternalSystem sys) {
        Integer delay = sys.getSyncDelay();
        if (delay == null || delay <= 0) {
            return;
        }
        Integer sysId = sys.getExternalSystemId();
        String code = sys.getCode();
        ScheduledFuture<?> future = taskScheduler.schedule(
                () -> runSync(code),
                triggerContext -> {
                    if (!enabled) {
                        return null;
                    }
                    Integer curDelay = apExternalSystemRepository.findById(sysId)
                            .map(ApExternalSystem::getSyncDelay)
                            .orElse(null);
                    if (curDelay == null || curDelay <= 0) {
                        // Synchronizace je vypnutá nebo záznam byl smazán. Úkol je ukončen;
                        // při příštím ApExternalSystemEvent onExternalSystemChanged se spustí nový úkol
                        return null;
                    }
                    Instant last = triggerContext.lastCompletion();
                    return last != null
                            ? last.plus(Duration.ofSeconds(curDelay))
                            : Instant.now();
                }
        );
        activeFutures.put(sysId, future);
    }

    private void runSync(String extSysCode) {
        if (!enabled) {
            return;
        }
        log.debug("Accesspoint synchronization started.");
        apConnectService.getConnector(extSysCode).synchronizeAccessPointsForExternalSystem(extSysCode);
        monitoringMetrics.recordCamPollSuccess(extSysCode);
        log.debug("Accesspoint synchronization finished.");
    }
}