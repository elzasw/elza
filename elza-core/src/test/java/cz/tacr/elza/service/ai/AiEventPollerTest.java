package cz.tacr.elza.service.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Date;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;

import cz.tacr.elza.aiprovider.client.vo.TaskEvent;
import cz.tacr.elza.aiprovider.client.vo.TaskEvents;
import cz.tacr.elza.aiprovider.client.vo.TaskState;
import cz.tacr.elza.domain.AiRequest;
import cz.tacr.elza.repository.AiRequestEventRepository;
import cz.tacr.elza.repository.AiRequestRepository;

/**
 * {@link AiEventPoller#applyBatch}: the event stream is advisory, yet storing
 * a batch saves the whole {@code ai_request} row (Hibernate writes every
 * column), so the row is loaded under its lock and a request the task poll has
 * meanwhile finished comes back finished. Loaded plainly, the copy could
 * predate that commit and the save put {@code running} and no output back over
 * the stored result (2026-09-17).
 */
class AiEventPollerTest {

    private final AiRequestRepository requestRepository = mock(AiRequestRepository.class);
    private final AiRequestEventRepository eventRepository = mock(AiRequestEventRepository.class);
    private final AiAnswerBuffer answerBuffer = mock(AiAnswerBuffer.class);
    private final PlatformTransactionManager txManager = mock(PlatformTransactionManager.class);

    private final AiEventPoller poller = new AiEventPoller();

    @BeforeEach
    void setUp() {
        when(txManager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        ReflectionTestUtils.setField(poller, "aiRequestRepository", requestRepository);
        ReflectionTestUtils.setField(poller, "aiRequestEventRepository", eventRepository);
        ReflectionTestUtils.setField(poller, "answerBuffer", answerBuffer);
        ReflectionTestUtils.setField(poller, "transactionTemplate", new TransactionTemplate(txManager));
        ReflectionTestUtils.setField(poller, "objectMapper", new ObjectMapper());
    }

    @Test
    void theTerminalBatchIsAppliedToTheLockedRowAndLeavesAFinishedRequestFinished() {
        // What the locked read returns: the task poll has already committed the
        // outcome (state, output, finish date; progress cleared).
        AiRequest request = new AiRequest();
        request.setAiRequestId(31);
        request.setTaskUid("t31");
        request.setState("done");
        request.setOutput("[{\"objectType\":\"elza.markdown\"}]");
        request.setFinishDate(new Date());
        request.setEventSeq(10);
        when(requestRepository.findForUpdateByAiRequestId(31)).thenReturn(Optional.of(request));

        // The final batch: a trailing phase and the terminal lifecycle event.
        TaskEvents batch = new TaskEvents().state(TaskState.DONE).nextSince(12L)
                .addEventsItem(new TaskEvent().seq(11L).type("phase")
                        .message("Processing the search results…").percent(65f))
                .addEventsItem(new TaskEvent().seq(12L).type("done"));

        Boolean stored = ReflectionTestUtils.invokeMethod(poller, "applyBatch", 31, batch);

        assertThat(stored).isTrue();
        // The cursor advances and both events are in the transparency log…
        assertThat(request.getEventSeq()).isEqualTo(12L);
        verify(eventRepository, times(2)).save(any());
        // …while the committed outcome is left exactly as the task poll wrote it:
        // no phase text resurrected on a finished exchange.
        assertThat(request.getState()).isEqualTo("done");
        assertThat(request.getOutput()).isEqualTo("[{\"objectType\":\"elza.markdown\"}]");
        assertThat(request.getFinishDate()).isNotNull();
        assertThat(request.getProgressMessage()).isNull();
        assertThat(request.getProgressPercent()).isNull();
        // The row was loaded through the locked read, never the plain one.
        verify(requestRepository).findForUpdateByAiRequestId(31);
        verify(requestRepository, never()).findById(any());
    }
}
