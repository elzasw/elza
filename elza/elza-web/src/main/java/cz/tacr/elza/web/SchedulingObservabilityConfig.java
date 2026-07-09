package cz.tacr.elza.web;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import cz.tacr.elza.metrics.InFlightTaskRegistry;

/**
 * Application task scheduler used for every {@code @Scheduled} method and
 * {@link org.springframework.scheduling.annotation.SchedulingConfigurer} task (CAM sync, the
 * async-slot writer, the nightly cleanup, ...).
 *
 * Two deliberate choices, both prompted by a production incident where a single scheduled CAM poll
 * blocked on an untimed wait and — because Spring's default scheduler is single-threaded — silently
 * froze all scheduled work until the next restart:
 * <ul>
 *   <li>a pool size &gt; 1, so one stuck task no longer starves the others (and the heartbeat can
 *       still run to prove liveness);</li>
 *   <li>an {@link InFlightTaskRegistry} task decorator, so a stuck task is observable
 *       ({@code elza.scheduler.longest_running_task_seconds}) and can be dumped by the watchdog.</li>
 * </ul>
 */
@Configuration
public class SchedulingObservabilityConfig {

    @Bean
    @Primary
    public TaskScheduler taskScheduler(final InFlightTaskRegistry inFlightTaskRegistry,
                                       @Value("${elza.monitoring.scheduler.poolSize:4}") final int poolSize) {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(poolSize);
        scheduler.setThreadNamePrefix("scheduling-");
        scheduler.setTaskDecorator(inFlightTaskRegistry::wrap);
        scheduler.setWaitForTasksToCompleteOnShutdown(false);
        return scheduler;
    }
}
