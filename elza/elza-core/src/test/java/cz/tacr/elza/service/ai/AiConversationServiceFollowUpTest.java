package cz.tacr.elza.service.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.fasterxml.jackson.databind.ObjectMapper;

import cz.tacr.elza.aiprovider.client.ElzaAiApi;
import cz.tacr.elza.aiprovider.client.vo.SubmitTask;
import cz.tacr.elza.aiprovider.client.vo.TaskAccepted;
import cz.tacr.elza.controller.vo.AiRequestCreateVO;
import cz.tacr.elza.controller.vo.AiRequestVO;
import cz.tacr.elza.domain.AiConversation;
import cz.tacr.elza.domain.AiExternalSystem;
import cz.tacr.elza.domain.AiRequest;
import cz.tacr.elza.repository.AiConversationRepository;
import cz.tacr.elza.repository.AiExternalSystemRepository;
import cz.tacr.elza.repository.AiRequestEventRepository;
import cz.tacr.elza.repository.AiRequestRepository;
import cz.tacr.elza.security.UserDetail;
import cz.tacr.elza.service.AiProviderService;
import cz.tacr.elza.service.UserService;

/**
 * The cross-type follow-up (the "fix this finding" handoff): a follow-up may
 * name its own {@code taskType} — an {@code elza.enhanceDescription} exchange
 * submitted into a {@code elza.revision} thread — while still continuing the
 * conversation ({@code parentTaskId} = the previous exchange's task). Without
 * an explicit type the previous exchange's type is reused, as always. Before
 * this, the type was hard-wired to the previous exchange's, so a check
 * conversation could only ever produce more read-only checks — its findings
 * were unfixable from the prompt (observed 2026-08-10, provider task 2305).
 */
class AiConversationServiceFollowUpTest {

    private final AiConversationRepository conversationRepository = mock(AiConversationRepository.class);
    private final AiRequestRepository requestRepository = mock(AiRequestRepository.class);
    private final AiRequestEventRepository eventRepository = mock(AiRequestEventRepository.class);
    private final AiExternalSystemRepository externalSystemRepository = mock(AiExternalSystemRepository.class);
    private final AiProviderService providerService = mock(AiProviderService.class);
    private final AiRequestViewMapper viewMapper = mock(AiRequestViewMapper.class);
    private final AiRequestPoller requestPoller = mock(AiRequestPoller.class);
    private final AiEventPoller eventPoller = mock(AiEventPoller.class);
    private final UserService userService = mock(UserService.class);
    private final AiContextResolver contextResolver = mock(AiContextResolver.class);
    private final AiToolRegistry toolRegistry = mock(AiToolRegistry.class);
    private final ElzaAiApi api = mock(ElzaAiApi.class);

    private final AiConversationService service = new AiConversationService();

    private final AiExternalSystem externalSystem = new AiExternalSystem();

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "aiConversationRepository", conversationRepository);
        ReflectionTestUtils.setField(service, "aiRequestRepository", requestRepository);
        ReflectionTestUtils.setField(service, "aiRequestEventRepository", eventRepository);
        ReflectionTestUtils.setField(service, "aiExternalSystemRepository", externalSystemRepository);
        ReflectionTestUtils.setField(service, "aiProviderService", providerService);
        ReflectionTestUtils.setField(service, "requestViewMapper", viewMapper);
        ReflectionTestUtils.setField(service, "aiRequestPoller", requestPoller);
        ReflectionTestUtils.setField(service, "aiEventPoller", eventPoller);
        ReflectionTestUtils.setField(service, "userService", userService);
        ReflectionTestUtils.setField(service, "objectMapper", new ObjectMapper());
        ReflectionTestUtils.setField(service, "contextResolver", contextResolver);
        ReflectionTestUtils.setField(service, "toolRegistry", toolRegistry);

        UserDetail user = mock(UserDetail.class);
        when(user.getId()).thenReturn(77);
        when(user.getUsername()).thenReturn("petr");
        when(userService.getLoggedUserDetail()).thenReturn(user);

        AiConversation conversation = new AiConversation();
        conversation.setAiConversationId(5);
        conversation.setUserId(77);
        conversation.setExternalSystemId(9);
        when(conversationRepository.findById(5)).thenReturn(Optional.of(conversation));
        when(externalSystemRepository.findById(9)).thenReturn(Optional.of(externalSystem));

        AiRequest previous = new AiRequest();
        previous.setAiRequestId(1);
        previous.setAiConversationId(5);
        previous.setTaskType("elza.revision");
        previous.setProfile("standard");
        previous.setState("done");
        previous.setTaskUid("t-parent-42");
        previous.setCreateDate(new Date());
        when(requestRepository.findByAiConversationIdOrderByCreateDateAsc(5))
                .thenReturn(List.of(previous));

        when(contextResolver.resolveAll(any())).thenReturn(List.of());
        when(providerService.createApi(eq(externalSystem), eq(77))).thenReturn(api);
        when(api.submitTask(any(OffsetDateTime.class), any(SubmitTask.class)))
                .thenReturn(new TaskAccepted().taskId("t-new"));
        when(viewMapper.loadEventsByRequest(any())).thenReturn(Map.of());
        when(viewMapper.toVO(any(), any())).thenReturn(new AiRequestVO());

        // submitExchange registers an after-commit hook; give it a live
        // synchronization scope like the @Transactional runtime would.
        TransactionSynchronizationManager.initSynchronization();
    }

    @AfterEach
    void tearDown() {
        TransactionSynchronizationManager.clearSynchronization();
    }

    @Test
    void aFollowUpMaySwitchTheTaskTypeAndStillChainsTheThread() {
        service.createRequest(5, new AiRequestCreateVO()
                .taskType("elza.enhanceDescription")
                .userInstructions("Připrav opravu zjištění 1."));

        SubmitTask submitted = submittedTask();
        assertThat(submitted.getTaskType()).isEqualTo("elza.enhanceDescription");
        // The thread continues: the new exchange is chained to the previous
        // exchange's task regardless of the type switch.
        assertThat(submitted.getParentTaskId()).isEqualTo("t-parent-42");
        assertThat(storedRequest().getTaskType()).isEqualTo("elza.enhanceDescription");
    }

    @Test
    void aFollowUpWithoutATypeReusesThePreviousExchanges() {
        service.createRequest(5, new AiRequestCreateVO()
                .userInstructions("Pokračuj v kontrole."));

        SubmitTask submitted = submittedTask();
        assertThat(submitted.getTaskType()).isEqualTo("elza.revision");
        assertThat(submitted.getParentTaskId()).isEqualTo("t-parent-42");
        assertThat(storedRequest().getTaskType()).isEqualTo("elza.revision");
    }

    private SubmitTask submittedTask() {
        ArgumentCaptor<SubmitTask> captor = ArgumentCaptor.forClass(SubmitTask.class);
        verify(api).submitTask(any(OffsetDateTime.class), captor.capture());
        return captor.getValue();
    }

    private AiRequest storedRequest() {
        ArgumentCaptor<AiRequest> captor = ArgumentCaptor.forClass(AiRequest.class);
        verify(requestRepository, org.mockito.Mockito.atLeastOnce()).save(captor.capture());
        return captor.getValue();
    }
}
