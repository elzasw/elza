package cz.tacr.elza.metrics;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.core.task.TaskDecorator;
import org.springframework.stereotype.Component;

/**
 * Tracks scheduled-task execution so a frozen scheduler becomes observable.
 *
 * Installed as the {@link TaskDecorator} of the application {@code TaskScheduler}: every scheduled
 * execution is registered while it runs and removed when it returns, so {@link #longestRunningSeconds()}
 * reports how long the oldest still-running task has been executing. The scheduler also stamps
 * {@link #recordHeartbeat()} on a fixed rate; {@link #heartbeatAgeSeconds()} then grows without bound
 * once the scheduler stops ticking (a thread stuck on an untimed wait, or a dead pool).
 *
 * All read methods are computed on the caller's thread (the metrics scrape / watchdog thread), so they
 * stay answerable even when every scheduler thread is blocked.
 */
@Component
public class InFlightTaskRegistry {

    /** Running executions keyed by a per-execution id. */
    private final Map<Long, RunningTask> inFlight = new ConcurrentHashMap<>();

    private final AtomicLong sequence = new AtomicLong();

    /** Epoch millis of the last scheduler heartbeat; seeded at startup so age grows if it never fires. */
    private final AtomicLong lastHeartbeatMillis = new AtomicLong(System.currentTimeMillis());

    /**
     * Whether steady-state monitoring is meaningful. Turned on by {@code StartupService} only once the
     * application is fully started (scheduler enabled, sync worker running) and off again on shutdown.
     * While {@code false} the scheduler tasks and the queue worker are not expected to be doing work,
     * so the gauges report "not applicable" and the watchdog stays silent instead of raising false alarms
     * during the startup / recovery / shutdown windows.
     */
    private volatile boolean active = false;

    /** Epoch millis when monitoring last became active; the reference point for age gauges before a first event. */
    private volatile long activatedAtMillis = System.currentTimeMillis();

    /**
     * Wraps a scheduled task so its execution is tracked. The same wrapper may run repeatedly (fixed-rate
     * / fixed-delay tasks reuse one Runnable), so a fresh id is allocated per execution.
     */
    public Runnable wrap(final Runnable delegate) {
        return () -> {
            long id = sequence.incrementAndGet();
            inFlight.put(id, new RunningTask(id, describe(delegate), Thread.currentThread(), System.currentTimeMillis()));
            try {
                delegate.run();
            } finally {
                inFlight.remove(id);
            }
        };
    }

    /** Records that the scheduler is alive. Called on a fixed rate from the monitored scheduler pool. */
    public void recordHeartbeat() {
        lastHeartbeatMillis.set(System.currentTimeMillis());
    }

    /** Enables steady-state monitoring once the application is fully started; seeds a fresh heartbeat. */
    public void activate() {
        long now = System.currentTimeMillis();
        activatedAtMillis = now;
        lastHeartbeatMillis.set(now);
        active = true;
    }

    /** Disables monitoring (application stopping), so the shutdown window raises no alarms. */
    public void deactivate() {
        active = false;
    }

    public boolean isActive() {
        return active;
    }

    /** Epoch millis when monitoring last became active; used as the baseline for age gauges before a first event. */
    public long activatedAtMillis() {
        return activatedAtMillis;
    }

    /** Seconds since the last scheduler heartbeat. */
    public double heartbeatAgeSeconds() {
        return Math.max(0d, (System.currentTimeMillis() - lastHeartbeatMillis.get()) / 1000d);
    }

    /** Seconds the longest currently-running scheduled task has been executing; {@code -1} when idle. */
    public double longestRunningSeconds() {
        long oldestStart = Long.MAX_VALUE;
        for (RunningTask task : inFlight.values()) {
            oldestStart = Math.min(oldestStart, task.startMillis());
        }
        if (oldestStart == Long.MAX_VALUE) {
            return -1d;
        }
        return Math.max(0d, (System.currentTimeMillis() - oldestStart) / 1000d);
    }

    /** Currently-running tasks that have been executing for at least {@code thresholdMillis}. */
    public List<RunningTask> stuck(final long thresholdMillis) {
        long now = System.currentTimeMillis();
        List<RunningTask> result = new ArrayList<>();
        for (RunningTask task : inFlight.values()) {
            if (now - task.startMillis() >= thresholdMillis) {
                result.add(task);
            }
        }
        return result;
    }

    /** Ids of all currently-running executions (used to expire watchdog dedup state). */
    public Set<Long> currentIds() {
        return new HashSet<>(inFlight.keySet());
    }

    private static String describe(final Runnable delegate) {
        String label = delegate.toString();
        return label != null ? label : delegate.getClass().getName();
    }

    /** A single in-flight scheduled execution. The live thread is kept so a watchdog can snapshot its stack. */
    public static final class RunningTask {

        private final long id;
        private final String label;
        private final Thread thread;
        private final long startMillis;

        RunningTask(final long id, final String label, final Thread thread, final long startMillis) {
            this.id = id;
            this.label = label;
            this.thread = thread;
            this.startMillis = startMillis;
        }

        public long id() {
            return id;
        }

        public String label() {
            return label;
        }

        public String threadName() {
            return thread.getName();
        }

        public long startMillis() {
            return startMillis;
        }

        public long runningSeconds() {
            return Math.max(0L, (System.currentTimeMillis() - startMillis) / 1000L);
        }

        /** Snapshot of the executing thread's stack at call time, i.e. where the task is currently blocked. */
        public String stackTraceAsString() {
            StringBuilder sb = new StringBuilder();
            for (StackTraceElement element : thread.getStackTrace()) {
                sb.append("\tat ").append(element).append('\n');
            }
            return sb.toString();
        }
    }
}
