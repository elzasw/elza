package cz.tacr.elza.service.da;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ScheduledFuture;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.TriggerContext;

import cz.tacr.elza.api.DigitalRepositoryType;
import cz.tacr.elza.domain.ArrDigitalRepository;
import cz.tacr.elza.repository.DigitalRepositoryRepository;
import cz.tacr.elza.service.event.ArrDigitalRepositoryEvent;

/**
 * Unit tests of the synchronization scheduling; the interval is read from
 * arr_digital_repository.sync_delay on every fire, so a change takes effect without a restart.
 */
public class DaSchedulerTest {

    private static final int REPO_ID = 42;

    private DaScheduler scheduler;

    private DaService daService;
    private DigitalRepositoryRepository digitalRepositoryRepository;
    private TaskScheduler taskScheduler;
    private ScheduledFuture<?> future;

    private ArrDigitalRepository repo;

    @BeforeEach
    void setUp() {
        daService = mock(DaService.class);
        digitalRepositoryRepository = mock(DigitalRepositoryRepository.class);
        taskScheduler = mock(TaskScheduler.class);
        future = mock(ScheduledFuture.class);

        repo = repository(DigitalRepositoryType.DA, 60);

        when(digitalRepositoryRepository.findAll()).thenReturn(List.of(repo));
        when(digitalRepositoryRepository.findById(REPO_ID)).thenReturn(Optional.of(repo));
        when(taskScheduler.schedule(any(Runnable.class), any(Trigger.class))).thenAnswer(inv -> future);

        scheduler = new DaScheduler();
        setField(scheduler, "daService", daService);
        setField(scheduler, "digitalRepositoryRepository", digitalRepositoryRepository);
        setField(scheduler, "taskScheduler", taskScheduler);
    }

    private static ArrDigitalRepository repository(DigitalRepositoryType type, Integer syncDelay) {
        ArrDigitalRepository repo = new ArrDigitalRepository();
        repo.setExternalSystemId(REPO_ID);
        repo.setCode("DA-REPO");
        repo.setDigitalRepositoryType(type);
        repo.setSyncDelay(syncDelay);
        return repo;
    }

    private Trigger capturedTrigger() {
        ArgumentCaptor<Trigger> trigger = ArgumentCaptor.forClass(Trigger.class);
        verify(taskScheduler).schedule(any(Runnable.class), trigger.capture());
        return trigger.getValue();
    }

    private static TriggerContext contextWithLastCompletion(Instant instant) {
        TriggerContext context = mock(TriggerContext.class);
        when(context.lastCompletion()).thenReturn(instant);
        return context;
    }

    @Test
    void start_schedulesDaRepositoryWithPositiveDelay() {
        scheduler.start();

        verify(taskScheduler).schedule(any(Runnable.class), any(Trigger.class));
    }

    @Test
    void start_skipsRepositoryOfAnotherType() {
        when(digitalRepositoryRepository.findAll())
                .thenReturn(List.of(repository(DigitalRepositoryType.FILESYSTEM, 60)));

        scheduler.start();

        verify(taskScheduler, never()).schedule(any(Runnable.class), any(Trigger.class));
    }

    @Test
    void start_skipsRepositoryWithSynchronizationOff() {
        when(digitalRepositoryRepository.findAll()).thenReturn(List.of(repository(DigitalRepositoryType.DA, 0)));

        scheduler.start();

        verify(taskScheduler, never()).schedule(any(Runnable.class), any(Trigger.class));
    }

    @Test
    void trigger_usesCurrentDelayFromDatabase() {
        scheduler.start();
        Trigger trigger = capturedTrigger();
        Instant lastCompletion = Instant.parse("2026-08-28T10:00:00Z");

        // the interval was raised to 5 minutes after the task had been scheduled
        repo.setSyncDelay(300);

        assertEquals(lastCompletion.plus(Duration.ofMinutes(5)),
                trigger.nextExecution(contextWithLastCompletion(lastCompletion)));
    }

    @Test
    void trigger_endsTaskWhenSynchronizationIsSwitchedOff() {
        scheduler.start();
        Trigger trigger = capturedTrigger();

        repo.setSyncDelay(0);

        assertNull(trigger.nextExecution(contextWithLastCompletion(Instant.parse("2026-08-28T10:00:00Z"))));
    }

    @Test
    void trigger_endsTaskWhenRepositoryIsDeleted() {
        scheduler.start();
        Trigger trigger = capturedTrigger();

        when(digitalRepositoryRepository.findById(REPO_ID)).thenReturn(Optional.empty());

        assertNull(trigger.nextExecution(contextWithLastCompletion(null)));
    }

    @Test
    void trigger_endsTaskAfterStop() {
        scheduler.start();
        Trigger trigger = capturedTrigger();

        scheduler.stop();

        assertNull(trigger.nextExecution(contextWithLastCompletion(null)));
    }

    @Test
    void stop_cancelsRunningTasks() {
        scheduler.start();

        scheduler.stop();

        verify(future).cancel(false);
    }

    @Test
    void onDigitalRepositoryChanged_reschedulesWithTheNewDelay() {
        scheduler.start();

        repo.setSyncDelay(120);
        scheduler.onDigitalRepositoryChanged(new ArrDigitalRepositoryEvent(this, repo));

        verify(future).cancel(false);
        verify(taskScheduler, times(2)).schedule(any(Runnable.class), any(Trigger.class));
    }

    @Test
    void onDigitalRepositoryChanged_schedulesRepositoryCreatedAfterStartup() {
        when(digitalRepositoryRepository.findAll()).thenReturn(List.of());
        scheduler.start();

        scheduler.onDigitalRepositoryChanged(new ArrDigitalRepositoryEvent(this, repo));

        verify(taskScheduler).schedule(any(Runnable.class), any(Trigger.class));
    }

    @Test
    void onDigitalRepositoryChanged_isIgnoredWhenSchedulerIsStopped() {
        scheduler.onDigitalRepositoryChanged(new ArrDigitalRepositoryEvent(this, repo));

        verify(taskScheduler, never()).schedule(any(Runnable.class), any(Trigger.class));
    }

    private static void setField(Object target, String fieldName, Object value) {
        try {
            java.lang.reflect.Field f = target.getClass().getDeclaredField(fieldName);
            f.setAccessible(true);
            f.set(target, value);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}
