package cz.tacr.elza.service.da;

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

import cz.tacr.elza.api.DigitalRepositoryType;
import cz.tacr.elza.domain.ArrDigitalRepository;
import cz.tacr.elza.repository.DigitalRepositoryRepository;
import cz.tacr.elza.service.event.ArrDigitalRepositoryEvent;

/**
 * Časovač pro synchronizace s digitálním archivem.
 * Interval synchronizace se čte z arr_digital_repository.sync_delay; změna přes REST API
 * je promítnuta bez restartu skrz {@link ArrDigitalRepositoryEvent}.
 */
@Service
public class DaScheduler {

    private static final Logger log = LoggerFactory.getLogger(DaScheduler.class);

    @Autowired
    private DaService daService;

    @Autowired
    private DigitalRepositoryRepository digitalRepositoryRepository;

    @Autowired
    private TaskScheduler taskScheduler;

    private final Map<Integer, ScheduledFuture<?>> activeFutures = new ConcurrentHashMap<>();

    private volatile boolean enabled = false;

    @Transactional
    public synchronized void start() {
        enabled = true;
        for (ArrDigitalRepository digitalRepository : digitalRepositoryRepository.findAll()) {
            scheduleFresh(digitalRepository);
        }
    }

    public synchronized void stop() {
        enabled = false;
        activeFutures.values().forEach(f -> f.cancel(false));
        activeFutures.clear();
    }

    /**
     * On create/update/delete of a digital repository, drop the running trigger for it (if any) and
     * re-read from DB. Handles: hot delay change, re-enable after 0, new repository added, deletion.
     */
    @EventListener
    public synchronized void onDigitalRepositoryChanged(ArrDigitalRepositoryEvent event) {
        if (!enabled) {
            return;
        }
        Integer repoId = event.getDigitalRepository().getExternalSystemId();
        ScheduledFuture<?> existing = activeFutures.remove(repoId);
        if (existing != null) {
            existing.cancel(false);
        }
        digitalRepositoryRepository.findById(repoId).ifPresent(this::scheduleFresh);
    }

    private void scheduleFresh(ArrDigitalRepository digitalRepository) {
        if (digitalRepository.getDigitalRepositoryType() != DigitalRepositoryType.DA) {
            // only a digital archive is synchronized, other repository types have no updates API
            return;
        }
        Integer delay = digitalRepository.getSyncDelay();
        if (delay == null || delay <= 0) {
            return;
        }
        Integer repoId = digitalRepository.getExternalSystemId();
        String code = digitalRepository.getCode();
        ScheduledFuture<?> future = taskScheduler.schedule(
                () -> runSync(code),
                triggerContext -> {
                    if (!enabled) {
                        return null;
                    }
                    Integer curDelay = digitalRepositoryRepository.findById(repoId)
                            .map(ArrDigitalRepository::getSyncDelay)
                            .orElse(null);
                    if (curDelay == null || curDelay <= 0) {
                        // Synchronizace je vypnutá nebo záznam byl smazán. Úkol je ukončen;
                        // při příštím ArrDigitalRepositoryEvent onDigitalRepositoryChanged se spustí nový úkol
                        return null;
                    }
                    Instant last = triggerContext.lastCompletion();
                    return last != null
                            ? last.plus(Duration.ofSeconds(curDelay))
                            : Instant.now();
                }
        );
        activeFutures.put(repoId, future);
    }

    private void runSync(String code) {
        if (!enabled) {
            return;
        }
        log.info("Da repository synchronization started.");
        daService.synchronizeDaRepository(code);
        log.info("Da repository synchronization finished.");
    }
}
