package cz.tacr.elza.web;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import cz.tacr.elza.metrics.InFlightTaskRegistry;
import cz.tacr.elza.metrics.InFlightTaskRegistry.RunningTask;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

/**
 * Keeps the scheduler-liveness signals fresh and captures diagnostics when a scheduled task hangs.
 *
 * <ul>
 *   <li>{@link #heartbeat()} runs on the monitored application scheduler pool, so
 *       {@code elza.scheduler.heartbeat_age_seconds} grows once that pool stops ticking.</li>
 *   <li>The watchdog runs on its own single daemon thread — independent of the monitored pool, so it
 *       still fires when every scheduler thread is blocked — and logs the stuck task together with the
 *       stack trace of the thread executing it (the evidence that is otherwise lost on restart).</li>
 * </ul>
 */
@Component
public class SchedulerWatchdog {

    private static final Logger log = LoggerFactory.getLogger(SchedulerWatchdog.class);

    private final InFlightTaskRegistry registry;

    private final long stuckThresholdMillis;

    private final ScheduledExecutorService watchdogExecutor = Executors.newSingleThreadScheduledExecutor(runnable -> {
        Thread thread = new Thread(runnable, "sched-watchdog");
        thread.setDaemon(true);
        return thread;
    });

    /** Ids of stuck executions already logged, so each stuck episode is reported once. */
    private final Set<Long> reported = ConcurrentHashMap.newKeySet();

    public SchedulerWatchdog(final InFlightTaskRegistry registry,
                             @Value("${elza.monitoring.scheduler.stuckThresholdSeconds:1800}") final long stuckThresholdSeconds) {
        this.registry = registry;
        this.stuckThresholdMillis = stuckThresholdSeconds * 1000L;
    }

    @Scheduled(fixedRateString = "${elza.monitoring.scheduler.heartbeatMs:30000}")
    public void heartbeat() {
        registry.recordHeartbeat();
    }

    @PostConstruct
    void start() {
        watchdogExecutor.scheduleWithFixedDelay(this::check, 60L, 60L, TimeUnit.SECONDS);
    }

    @PreDestroy
    void stop() {
        watchdogExecutor.shutdownNow();
    }

    private void check() {
        // Stay silent until the application is fully started (and again once it is stopping): scheduled
        // tasks and the sync worker are not expected to be doing work outside that window.
        if (!registry.isActive()) {
            return;
        }
        try {
            for (RunningTask task : registry.stuck(stuckThresholdMillis)) {
                if (reported.add(task.id())) {
                    log.warn("Scheduled task appears STUCK for {}s: {} (thread {}). Current stack:\n{}",
                             task.runningSeconds(), task.label(), task.threadName(), task.stackTraceAsString());
                }
            }
            // Drop dedup state for executions that have finished, so a later stall re-reports.
            reported.retainAll(registry.currentIds());
        } catch (Exception e) {
            log.error("Scheduler watchdog check failed.", e);
        }
    }
}
