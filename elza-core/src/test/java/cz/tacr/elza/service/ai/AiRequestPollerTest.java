package cz.tacr.elza.service.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
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
import cz.tacr.elza.service.UserService;
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
    private final UserService userService = mock(UserService.class);

    private final AiRequestPoller poller = new AiRequestPoller();

    /** Real push component (it holds the render-as-owner logic under test), mock edges. */
    private final AiRequestPushService requestPushService = new AiRequestPushService();

    @BeforeEach
    void setUp() {
        when(txManager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        when(viewMapper.buildUpdateMessage(any())).thenReturn(new AiRequestUpdateMessage(1, null));
        // The snapshot push impersonates the conversation owner; a plain empty
        // context is enough for the mocked mapper.
        when(userService.createSecurityContext(any()))
                .thenAnswer(invocation -> SecurityContextHolder.createEmptyContext());
        ReflectionTestUtils.setField(requestPushService, "aiRequestRepository", requestRepository);
        ReflectionTestUtils.setField(requestPushService, "requestViewMapper", viewMapper);
        ReflectionTestUtils.setField(requestPushService, "pushService", pushService);
        ReflectionTestUtils.setField(requestPushService, "transactionTemplate",
                new TransactionTemplate(txManager));
        ReflectionTestUtils.setField(requestPushService, "userService", userService);
        ReflectionTestUtils.setField(poller, "aiRequestRepository", requestRepository);
        ReflectionTestUtils.setField(poller, "aiConversationRepository", conversationRepository);
        ReflectionTestUtils.setField(poller, "aiExternalSystemRepository", externalSystemRepository);
        ReflectionTestUtils.setField(poller, "aiRequestEventRepository", eventRepository);
        ReflectionTestUtils.setField(poller, "requestPushService", requestPushService);
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

    @Test
    void failRequestAppliesTheGivenErrorCode() {
        // The give-up path (a task the provider can no longer answer for) settles
        // the exchange with a TIMEOUT code through the same helper.
        AiRequest request = request(7, "t7", "running", 70);
        AiConversation conversation = conversation(700, 7000);
        when(requestRepository.findById(7)).thenReturn(Optional.of(request));
        when(conversationRepository.findById(70)).thenReturn(Optional.of(conversation));

        Boolean marked = ReflectionTestUtils.invokeMethod(poller, "failRequest", 7, "TIMEOUT",
                "The AI provider stopped responding.");

        assertThat(marked).isTrue();
        assertThat(request.getState()).isEqualTo("error");
        assertThat(request.getErrorCode()).isEqualTo("TIMEOUT");
        assertThat(request.getFinishDate()).isNotNull();
        verify(answerBuffer).clear(7);
    }

    @Test
    void givesUpWhenRequestExceedsItsLifetime() {
        // An exchange open past its absolute lifetime is settled before the poll
        // even runs — so the provider is never contacted (aiProviderService is
        // left unset here, proving the check fires first).
        ReflectionTestUtils.setField(poller, "requestLifetimeTimeoutSeconds", 1L);
        AiRequest request = request(8, "t8", "running", 80);
        request.setCreateDate(new Date(System.currentTimeMillis() - 60_000));
        AiConversation conversation = conversation(800, 8000);
        when(requestRepository.findById(8)).thenReturn(Optional.of(request));
        when(conversationRepository.findById(80)).thenReturn(Optional.of(conversation));
        when(externalSystemRepository.findById(8000)).thenReturn(Optional.of(mock(AiExternalSystem.class)));

        ReflectionTestUtils.invokeMethod(poller, "pollLoop", 8);

        assertThat(request.getState()).isEqualTo("error");
        assertThat(request.getErrorCode()).isEqualTo("TIMEOUT");
        assertThat(request.getErrorMessage()).contains("maximum lifetime");
        verify(answerBuffer).clear(8);
    }

    /**
     * An unexpected failure inside the loop must settle the exchange, not
     * abandon it. The loop runs on an executor whose Future nobody inspects, so
     * before this an unchecked exception ended the poller thread in silence: the
     * request stayed open and the user saw a misleading TIMEOUT half an hour
     * later, once the lifetime backstop fired (2026-08-07).
     */
    @Test
    void anUnexpectedFailureSettlesTheRequestInsteadOfAbandoningIt() {
        AiRequest request = request(9, "t9", "running", 90);
        request.setCreateDate(new Date());
        AiConversation conversation = conversation(900, 9000);
        when(requestRepository.findById(9)).thenReturn(Optional.of(request));
        when(conversationRepository.findById(90))
                .thenThrow(new IllegalStateException("conversation lookup exploded"))
                // the failRequest path re-reads it while settling
                .thenReturn(Optional.of(conversation));

        // Must not propagate out of the loop — it is the thread's entry point.
        ReflectionTestUtils.invokeMethod(poller, "pollLoop", 9);

        assertThat(request.getState()).isEqualTo("error");
        assertThat(request.getErrorCode()).isEqualTo("INTERNAL");
        assertThat(request.getErrorMessage()).contains("conversation lookup exploded");
        assertThat(request.getFinishDate()).isNotNull();
    }

    /**
     * The pushed snapshot renders block mappers that run real permission checks
     * (the proposal block re-reads its level through {@code canRead}), and the
     * poller thread has no security context of its own — unauthenticated, the
     * check blew up, every completion push was skipped and the panel hung on
     * its progress state until a manual reload re-rendered on an authenticated
     * request thread (2026-08-10). The render must therefore run under the
     * conversation owner's context, and the pooled thread must be left clean.
     */
    @Test
    void theSnapshotIsRenderedAsTheOwnerAndTheThreadIsLeftClean() {
        AiRequest request = request(11, "t11", "running", 110);
        AiConversation conversation = conversation(1100, 11000);
        when(requestRepository.findById(11)).thenReturn(Optional.of(request));
        when(conversationRepository.findById(110)).thenReturn(Optional.of(conversation));

        SecurityContext ownerContext = SecurityContextHolder.createEmptyContext();
        ownerContext.setAuthentication(new UsernamePasswordAuthenticationToken("owner", null, null));
        when(userService.createSecurityContext(1100)).thenReturn(ownerContext);
        List<SecurityContext> observedAtRender = new ArrayList<>();
        when(viewMapper.buildUpdateMessage(any())).thenAnswer(invocation -> {
            observedAtRender.add(SecurityContextHolder.getContext());
            return new AiRequestUpdateMessage(11, null);
        });

        ReflectionTestUtils.invokeMethod(poller, "failRequest", 11, "TIMEOUT",
                "The AI provider stopped responding.");

        assertThat(observedAtRender).containsExactly(ownerContext);
        verify(pushService).push(eq(1100), any());
        // No trace of the owner left on the pooled thread.
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    /**
     * Rendering is derived, the provider's answer is authoritative: a broken
     * block mapper must not roll back a result that was received and stored.
     * When the view was built inside the persisting transaction, a mapper
     * failure discarded the finished result entirely.
     */
    @Test
    void aBrokenViewMapperDoesNotLoseTheSettledResult() {
        AiRequest request = request(10, "t10", "running", 100);
        AiConversation conversation = conversation(1000, 10000);
        when(requestRepository.findById(10)).thenReturn(Optional.of(request));
        when(conversationRepository.findById(100)).thenReturn(Optional.of(conversation));
        when(viewMapper.buildUpdateMessage(any()))
                .thenThrow(new IllegalStateException("proposal block mapper exploded"));

        Boolean marked = ReflectionTestUtils.invokeMethod(poller, "failRequest", 10, "TIMEOUT",
                "The AI provider stopped responding.");

        // Settled and persisted, even though drawing its card failed…
        assertThat(marked).isTrue();
        assertThat(request.getState()).isEqualTo("error");
        assertThat(request.getErrorCode()).isEqualTo("TIMEOUT");
        // …and nothing half-rendered was pushed to the client.
        verify(pushService, never()).push(any(), any());
    }
}
