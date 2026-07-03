package cz.tacr.elza.metrics;

import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import cz.tacr.elza.cam.SyncConfig;
import cz.tacr.elza.cam.SyncConfig.SynchronizationInfo;
import cz.tacr.elza.domain.ExtSyncsQueueItem.ExtAsyncQueueState;
import cz.tacr.elza.repository.ExtSyncsQueueItemRepository;
import cz.tacr.elza.websocket.WebSocketThreadPoolTaskExecutor;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;

/**
 * Instance-level monitoring gauges exposed through Actuator/Prometheus and reported to the
 * Customer Service Center (CSC).
 *
 * The CAM values are aggregated across all configured external systems into a single series
 * (worst age, or total count), so one threshold applies uniformly on every Elza instance
 * regardless of how many external systems it has. Ages are reported in seconds; an age gauge
 * returns {@code -1} when there is nothing to measure (no configured system, or no matching
 * item), a value no "above" threshold will alert on.
 */
@Component
public class ElzaMonitoringMetrics implements MeterBinder {

    /** Reported by an age gauge when there is nothing to measure. */
    private static final double NOT_APPLICABLE = -1d;

    private final ExtSyncsQueueItemRepository extSyncsQueueItemRepository;

    private final SyncConfig syncConfig;

    /** Outbound WebSocket executor holding the live client sessions; absent outside the web application. */
    @Autowired(required = false)
    @Qualifier("clientOutboundChannelExecutor")
    private WebSocketThreadPoolTaskExecutor outboundChannelExecutor;

    /** Epoch millis of the last successful CAM poll, keyed by external system code. */
    private final Map<String, AtomicLong> lastCamPollSuccessMillis = new ConcurrentHashMap<>();

    /**
     * Baseline used as "last success" until a real poll succeeds, so the age grows from startup
     * when a configured CAM system never answers.
     */
    private final long startupMillis = System.currentTimeMillis();

    @Autowired
    public ElzaMonitoringMetrics(final ExtSyncsQueueItemRepository extSyncsQueueItemRepository,
                                 final SyncConfig syncConfig) {
        this.extSyncsQueueItemRepository = extSyncsQueueItemRepository;
        this.syncConfig = syncConfig;
    }

    @Override
    public void bindTo(final MeterRegistry registry) {
        // Meter names carry their unit in the suffix and must not declare a Micrometer base unit:
        // the Prometheus registry would append it again (e.g. ..._seconds_seconds), breaking the
        // exact series names the CSC scrape map / csc-metrics.json rely on.
        Gauge.builder("elza.cam.last_success_age_seconds", this, ElzaMonitoringMetrics::camLastSuccessAgeSeconds)
                .description("Seconds since the last successful poll of a CAM external system, taken as the worst"
                        + " (oldest) across all configured systems; -1 when no CAM system is configured")
                .register(registry);

        Gauge.builder("elza.cam.error_count", this, m -> m.countByState(ExtAsyncQueueState.ERROR))
                .description("CAM sync items in ERROR state (failed dispatch/upload, e.g. invalid XML representation),"
                        + " summed across all external systems")
                .register(registry);

        Gauge.builder("elza.cam.oldest_error_age_seconds", this, m -> m.oldestAgeSeconds(ExtAsyncQueueState.ERROR))
                .description("Seconds since the oldest CAM sync item in ERROR state last changed state; -1 when none")
                .register(registry);

        Gauge.builder("elza.cam.deferred_oldest_age_seconds", this,
                m -> m.oldestAgeSeconds(ExtAsyncQueueState.UPDATE_DEFERRED))
                .description("Seconds since the oldest deferred CAM sync item (e.g. a REPLACEMENT whose replacing"
                        + " entity is not yet available) last changed state; -1 when none")
                .register(registry);

        Gauge.builder("elza.users.connected", this, ElzaMonitoringMetrics::connectedUsers)
                .description("Currently connected users (active WebSocket client sessions)")
                .register(registry);
    }

    /**
     * Records a successful CAM poll for the given external system. Called by the scheduler when a
     * synchronization run completes without error (including a run that finds nothing changed).
     */
    public void recordCamPollSuccess(final String extSysCode) {
        if (extSysCode == null) {
            return;
        }
        lastCamPollSuccessMillis
                .computeIfAbsent(extSysCode, code -> new AtomicLong(startupMillis))
                .set(System.currentTimeMillis());
    }

    private double camLastSuccessAgeSeconds() {
        List<SynchronizationInfo> configs = syncConfig.getConfig();
        if (configs == null || configs.isEmpty()) {
            return NOT_APPLICABLE;
        }
        long oldestSuccessMillis = Long.MAX_VALUE;
        for (SynchronizationInfo config : configs) {
            AtomicLong lastSuccess = lastCamPollSuccessMillis.get(config.getCode());
            oldestSuccessMillis = Math.min(oldestSuccessMillis, lastSuccess != null ? lastSuccess.get() : startupMillis);
        }
        return Math.max(0d, (System.currentTimeMillis() - oldestSuccessMillis) / 1000d);
    }

    private double countByState(final ExtAsyncQueueState state) {
        return extSyncsQueueItemRepository.countByState(state);
    }

    private double oldestAgeSeconds(final ExtAsyncQueueState state) {
        OffsetDateTime oldest = extSyncsQueueItemRepository.findOldestDateByState(state);
        if (oldest == null) {
            return NOT_APPLICABLE;
        }
        return Math.max(0d, Duration.between(oldest.toInstant(), Instant.now()).getSeconds());
    }

    private double connectedUsers() {
        return outboundChannelExecutor != null ? outboundChannelExecutor.getSessions().size() : 0d;
    }
}
