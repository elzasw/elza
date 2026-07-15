package cz.tacr.elza.service.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;

import cz.tacr.elza.domain.AiConversation;
import cz.tacr.elza.domain.AiExternalSystem;
import cz.tacr.elza.domain.AiRequest;
import cz.tacr.elza.repository.AiConversationRepository;
import cz.tacr.elza.repository.AiExternalSystemRepository;
import cz.tacr.elza.repository.AiRequestEventRepository;
import cz.tacr.elza.repository.AiRequestRepository;
import cz.tacr.elza.websocket.UserEventPushService;

/**
 * Startup reconciliation in {@link AiRequestPoller#resumeOpenRequests()}: a
 * non-terminal request the poll can never advance (never submitted, or its
 * conversation / AI provider is gone) is settled as {@code error}/INTERRUPTED
 * so it does not linger as a perpetually running exchange; a resumable request
 * is left untouched for the poll to pick up.
 */
class AiRequestPollerTest {

    private final AiRequestRepository requestRepository = mock(AiRequestRepository.class);
    private final AiConversationRepository conversationRepository = mock(AiConversationRepository.class);
    private final AiExternalSystemRepository externalSystemRepository = mock(AiExternalSystemRepository.class);
    private final AiRequestEventRepository eventRepository = mock(AiRequestEventRepository.class);
    private final AiRequestViewMapper viewMapper = mock(AiRequestViewMapper.class);
    private final UserEventPushService pushService = mock(UserEventPushService.class);
    private final AiAnswerBuffer answerBuffer = mock(AiAnswerBuffer.class);
    private final PlatformTransactionManager txManager = mock(PlatformTransactionManager.class);

    private final AiRequestPoller poller = new AiRequestPoller();

    @BeforeEach
    void setUp() {
        when(txManager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        when(viewMapper.buildUpdateMessage(any())).thenReturn(new AiRequestUpdateMessage(1, null));
        ReflectionTestUtils.setField(poller, "aiRequestRepository", requestRepository);
        ReflectionTestUtils.setField(poller, "aiConversationRepository", conversationRepository);
        ReflectionTestUtils.setField(poller, "aiExternalSystemRepository", externalSystemRepository);
        ReflectionTestUtils.setField(poller, "aiRequestEventRepository", eventRepository);
        ReflectionTestUtils.setField(poller, "requestViewMapper", viewMapper);
        ReflectionTestUtils.setField(poller, "pushService", pushService);
        ReflectionTestUtils.setField(poller, "answerBuffer", answerBuffer);
        ReflectionTestUtils.setField(poller, "transactionTemplate", new TransactionTemplate(txManager));
        ReflectionTestUtils.setField(poller, "objectMapper", new ObjectMapper());
    }

    private static AiRequest request(final int id, final String taskUid, final String state, final int convId) {
        AiRequest request = new AiRequest();
        request.setAiRequestId(id);
        request.setTaskUid(taskUid);
        request.setState(state);
        request.setAiConversationId(convId);
        return request;
    }

    private static AiConversation conversation(final int userId, final int externalSystemId) {
        AiConversation conversation = mock(AiConversation.class);
        when(conversation.getUserId()).thenReturn(userId);
        when(conversation.getExternalSystemId()).thenReturn(externalSystemId);
        return conversation;
    }

    @Test
    void settlesUnresumableRequestsAndLeavesResumableOnes() {
        AiRequest neverSubmitted = request(1, null, "queued", 10);
        AiRequest missingConversation = request(2, "t2", "running", 20);
        AiRequest missingProvider = request(3, "t3", "awaiting_tools", 30);
        AiRequest resumable = request(4, "t4", "running", 40);

        // Build the conversation mocks first — Mockito rejects stubbing a mock
        // in the middle of stubbing another (nested when(...)).
        AiConversation convNeverSubmitted = conversation(100, 1000);
        AiConversation convMissingProvider = conversation(300, 3000);
        AiConversation convResumable = conversation(400, 4000);

        when(requestRepository.findByStateNotIn(any()))
                .thenReturn(List.of(neverSubmitted, missingConversation, missingProvider, resumable));
        when(requestRepository.findById(1)).thenReturn(Optional.of(neverSubmitted));
        when(requestRepository.findById(2)).thenReturn(Optional.of(missingConversation));
        when(requestRepository.findById(3)).thenReturn(Optional.of(missingProvider));
        // A resumed request must be handed to the poll, never marked terminal.
        when(requestRepository.findByStateNotInAndTaskUidIsNotNull(any())).thenReturn(List.of());

        when(conversationRepository.findById(10)).thenReturn(Optional.of(convNeverSubmitted));
        when(conversationRepository.findById(20)).thenReturn(Optional.empty());
        when(conversationRepository.findById(30)).thenReturn(Optional.of(convMissingProvider));
        when(conversationRepository.findById(40)).thenReturn(Optional.of(convResumable));
        // Provider 3000 is gone (missingProvider); 4000 still exists (resumable).
        when(externalSystemRepository.findById(3000)).thenReturn(Optional.empty());
        when(externalSystemRepository.findById(4000)).thenReturn(Optional.of(mock(AiExternalSystem.class)));

        poller.resumeOpenRequests();

        assertThat(neverSubmitted.getState()).isEqualTo("error");
        assertThat(neverSubmitted.getErrorCode()).isEqualTo("INTERRUPTED");
        assertThat(neverSubmitted.getFinishDate()).isNotNull();

        assertThat(missingConversation.getState()).isEqualTo("error");
        assertThat(missingConversation.getErrorCode()).isEqualTo("INTERRUPTED");

        assertThat(missingProvider.getState()).isEqualTo("error");
        assertThat(missingProvider.getErrorCode()).isEqualTo("INTERRUPTED");

        // Resumable: untouched (the poll advances it; a restart loses nothing).
        assertThat(resumable.getState()).isEqualTo("running");
        assertThat(resumable.getErrorCode()).isNull();
        assertThat(resumable.getFinishDate()).isNull();

        // One ERROR transparency-log row per settled request; the owner is notified.
        verify(eventRepository, times(3)).save(any());
        verify(answerBuffer).clear(1);
        verify(answerBuffer).clear(2);
        verify(answerBuffer).clear(3);
        verify(answerBuffer, never()).clear(4);
    }
}
