package cz.tacr.elza.service.ai;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.beans.factory.annotation.Value;

import cz.tacr.elza.aiprovider.client.vo.SubmitTask;
import cz.tacr.elza.aiprovider.client.vo.TaskAccepted;
import cz.tacr.elza.aiprovider.client.vo.TaskMetadata;
import cz.tacr.elza.controller.vo.AiConversationCreateVO;
import cz.tacr.elza.controller.vo.AiConversationDetailVO;
import cz.tacr.elza.controller.vo.AiConversationVO;
import cz.tacr.elza.controller.vo.AiRequestCreateVO;
import cz.tacr.elza.controller.vo.AiRequestEventVO;
import cz.tacr.elza.controller.vo.AiRequestVO;
import cz.tacr.elza.controller.vo.AiUsageVO;
import cz.tacr.elza.domain.AiConversation;
import cz.tacr.elza.domain.AiExternalSystem;
import cz.tacr.elza.domain.AiRequest;
import cz.tacr.elza.domain.AiRequestEvent;
import cz.tacr.elza.exception.AccessDeniedException;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.ObjectNotFoundException;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.repository.AiConversationRepository;
import cz.tacr.elza.repository.AiExternalSystemRepository;
import cz.tacr.elza.repository.AiRequestEventRepository;
import cz.tacr.elza.repository.AiRequestRepository;
import cz.tacr.elza.security.UserDetail;
import cz.tacr.elza.service.AiProviderService;
import cz.tacr.elza.service.UserService;

/**
 * AI conversations of the current user: create (= submit the first exchange to
 * the provider), refresh, follow-up, cancel, event log. Backed by the
 * ai_conversation / ai_request / ai_request_event tables; provider
 * communication runs over the signed client ({@link AiProviderService}) and
 * state changes are observed by {@link AiRequestPoller}.
 *
 * <p>Task input building (v1): the {@code parameters} JSON of the exchange is
 * sent as the provider task input when present; otherwise the input is
 * {@code {"message": userInstructions}}. The output schema is permissive
 * ({@code {"type":"object"}}) — panel task types with a fixed contract (e.g.
 * the revision action) will supply their own input builders and schemas.
 */
@Service
public class AiConversationService {

    /** Permissive output schema of panel exchanges (v1). */
    private static final Map<String, Object> OPEN_OBJECT_SCHEMA = Map.of("type", "object");

    @Autowired
    private AiConversationRepository aiConversationRepository;

    @Autowired
    private AiRequestRepository aiRequestRepository;

    @Autowired
    private AiRequestEventRepository aiRequestEventRepository;

    @Autowired
    private AiExternalSystemRepository aiExternalSystemRepository;

    @Autowired
    private AiProviderService aiProviderService;

    @Autowired
    private AiBlockMapperRegistry blockMapperRegistry;

    @Autowired
    private AiRequestPoller aiRequestPoller;

    @Autowired
    private UserService userService;

    @Autowired
    private ObjectMapper objectMapper;

    /** Application version, reported in task metadata (audit at the provider/CSC). */
    @Value("${version:0.0.0}")
    private String appVersion;

    // -----------------------------------------------------------------------
    // Operations
    // -----------------------------------------------------------------------

    @Transactional
    public AiConversationDetailVO createConversation(final AiConversationCreateVO vo) {
        Integer userId = loggedUserId();
        AiExternalSystem externalSystem = aiProviderService.findAiSystemByCodeOrId(vo.getExternalSystemCode());

        AiConversation conversation = new AiConversation();
        conversation.setExternalSystemId(externalSystem.getExternalSystemId());
        conversation.setUserId(userId);
        conversation.setTitle(deriveTitle(vo));
        conversation.setContextType(vo.getContextType());
        conversation.setContext(vo.getContext());
        Date now = new Date();
        conversation.setCreateDate(now);
        conversation.setLastChangeDate(now);
        aiConversationRepository.save(conversation);

        submitExchange(conversation, externalSystem, vo.getTaskType(),
                       vo.getUserInstructions(), vo.getParameters(), null);
        return getDetail(conversation, externalSystem);
    }

    @Transactional
    public AiConversationDetailVO createRequest(final Integer conversationId, final AiRequestCreateVO vo) {
        AiConversation conversation = loadOwnConversation(conversationId);
        AiExternalSystem externalSystem = loadExternalSystem(conversation);

        List<AiRequest> requests = aiRequestRepository
                .findByAiConversationIdOrderByCreateDateAsc(conversationId);
        if (requests.isEmpty()) {
            throw new BusinessException("Conversation has no exchanges", BaseCode.INVALID_STATE);
        }
        AiRequest last = requests.get(requests.size() - 1);
        if (!"done".equals(last.getState())) {
            throw new BusinessException("Previous exchange is not finished", BaseCode.INVALID_STATE);
        }

        conversation.setLastChangeDate(new Date());
        aiConversationRepository.save(conversation);

        submitExchange(conversation, externalSystem, last.getTaskType(),
                       vo.getUserInstructions(), vo.getParameters(), last.getTaskUid());
        return getDetail(conversation, externalSystem);
    }

    @Transactional
    public AiConversationDetailVO getConversation(final Integer conversationId) {
        AiConversation conversation = loadOwnConversation(conversationId);
        return getDetail(conversation, loadExternalSystem(conversation));
    }

    @Transactional
    public List<AiConversationVO> listConversations(final String contextType) {
        Integer userId = loggedUserId();
        List<AiConversationVO> result = new ArrayList<>();
        for (AiConversation conversation : aiConversationRepository
                .findByUserIdOrderByLastChangeDateDesc(userId)) {
            if (contextType != null && !contextType.equals(conversation.getContextType())) {
                continue;
            }
            result.add(toVO(conversation, loadExternalSystem(conversation)));
        }
        return result;
    }

    @Transactional
    public AiRequestVO cancelRequest(final Integer requestId) {
        AiRequest request = aiRequestRepository.findById(requestId)
                .orElseThrow(() -> notFound("AI request not found: " + requestId, requestId));
        AiConversation conversation = loadOwnConversation(request.getAiConversationId());
        AiExternalSystem externalSystem = loadExternalSystem(conversation);

        if (request.getTaskUid() != null && !isTerminal(request.getState())) {
            try {
                aiProviderService.createApi(externalSystem, conversation.getUserId())
                        .cancelTask(OffsetDateTime.now(), request.getTaskUid());
            } catch (Exception e) {
                // best effort: the poller picks the final state up either way
            }
            addEvent(request, AiRequestEvent.TYPE_CANCEL, null);
        }
        return toVO(request);
    }

    @Transactional
    public List<AiRequestEventVO> listRequestEvents(final Integer requestId) {
        AiRequest request = aiRequestRepository.findById(requestId)
                .orElseThrow(() -> notFound("AI request not found: " + requestId, requestId));
        loadOwnConversation(request.getAiConversationId()); // owner check

        List<AiRequestEventVO> result = new ArrayList<>();
        for (AiRequestEvent event : aiRequestEventRepository
                .findByAiRequestIdOrderByCreateDateAsc(requestId)) {
            result.add(new AiRequestEventVO()
                    .id(event.getAiRequestEventId())
                    .eventType(event.getEventType())
                    .data(event.getData())
                    .createDate(toOffset(event.getCreateDate())));
        }
        return result;
    }

    // -----------------------------------------------------------------------
    // Exchange submission
    // -----------------------------------------------------------------------

    /**
     * Creates the ai_request row, submits the task to the provider and starts
     * polling (after commit). A provider failure at submit does not fail the
     * whole operation — the exchange is stored in state {@code error} so the
     * user sees the failure in the thread.
     */
    private void submitExchange(final AiConversation conversation, final AiExternalSystem externalSystem,
                                final String taskType, final String userInstructions,
                                final String parameters, final String parentTaskUid) {
        AiRequest request = new AiRequest();
        request.setAiConversationId(conversation.getAiConversationId());
        request.setRequestId(UUID.randomUUID().toString());
        request.setTaskType(taskType);
        request.setState("queued");
        request.setUserInstructions(userInstructions);
        request.setParameters(parameters);
        request.setCreateDate(new Date());
        aiRequestRepository.save(request);

        // Attribution metadata (accounts-and-credits proposal, decision Q7):
        // requestedBy = the current Elza username, ALWAYS sent — with a shared
        // signing key it is the only per-user attribution the provider/CSC
        // usage breakdown has. Audit-only by design, never enforcement.
        TaskMetadata metadata = new TaskMetadata()
                .requestedBy(loggedUsername())
                .app("elza")
                .appVersion(appVersion);

        SubmitTask submitTask = new SubmitTask()
                .requestId(request.getRequestId())
                .taskType(taskType)
                .userInstructions(userInstructions)
                .input(buildInput(userInstructions, parameters))
                .outputSchema(new HashMap<>(OPEN_OBJECT_SCHEMA))
                .parentTaskId(parentTaskUid)
                .metadata(metadata);
        addEvent(request, AiRequestEvent.TYPE_SUBMIT, toJson(submitTask));

        try {
            // Bill to the conversation owner's account: their personal key
            // when stored, else the instance-wide key (shared account).
            TaskAccepted accepted = aiProviderService.createApi(externalSystem, conversation.getUserId())
                    .submitTask(OffsetDateTime.now(), submitTask);
            request.setTaskUid(accepted.getTaskId());
            aiRequestRepository.save(request);
        } catch (Exception e) {
            request.setState("error");
            request.setErrorCode("SUBMIT_FAILED");
            request.setErrorMessage(e.getMessage());
            request.setFinishDate(new Date());
            aiRequestRepository.save(request);
            addEvent(request, AiRequestEvent.TYPE_ERROR, toJson(Map.of("message",
                    StringUtils.defaultString(e.getMessage()))));
            return;
        }

        final Integer requestId = request.getAiRequestId();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                aiRequestPoller.ensurePolling(requestId);
            }
        });
    }

    private Object buildInput(final String userInstructions, final String parameters) {
        if (StringUtils.isNotBlank(parameters)) {
            try {
                return objectMapper.readValue(parameters, Map.class);
            } catch (Exception e) {
                throw new BusinessException("Parameters are not valid JSON", BaseCode.PROPERTY_IS_INVALID);
            }
        }
        return Map.of("message", StringUtils.defaultString(userInstructions));
    }

    // -----------------------------------------------------------------------
    // Mapping & helpers
    // -----------------------------------------------------------------------

    private AiConversationDetailVO getDetail(final AiConversation conversation,
                                             final AiExternalSystem externalSystem) {
        List<AiRequestVO> requests = new ArrayList<>();
        for (AiRequest request : aiRequestRepository
                .findByAiConversationIdOrderByCreateDateAsc(conversation.getAiConversationId())) {
            requests.add(toVO(request));
        }
        return new AiConversationDetailVO()
                .conversation(toVO(conversation, externalSystem))
                .requests(requests);
    }

    private AiConversationVO toVO(final AiConversation conversation, final AiExternalSystem externalSystem) {
        return new AiConversationVO()
                .id(conversation.getAiConversationId())
                .externalSystemCode(externalSystem.getCode())
                .title(conversation.getTitle())
                .contextType(conversation.getContextType())
                .context(conversation.getContext())
                .createDate(toOffset(conversation.getCreateDate()))
                .lastChangeDate(toOffset(conversation.getLastChangeDate()));
    }

    private AiRequestVO toVO(final AiRequest request) {
        AiRequestVO vo = new AiRequestVO()
                .id(request.getAiRequestId())
                .taskType(request.getTaskType())
                .state(request.getState())
                .userInstructions(request.getUserInstructions())
                .errorCode(request.getErrorCode())
                .errorMessage(request.getErrorMessage())
                .promptVersion(request.getPromptVersion())
                .createDate(toOffset(request.getCreateDate()))
                .finishDate(toOffset(request.getFinishDate()));
        if ("done".equals(request.getState()) && request.getOutput() != null) {
            vo.setBlocks(blockMapperRegistry.map(request.getTaskType(), request.getOutput()));
        }
        if (request.getFinishDate() != null || !"queued".equals(request.getState())) {
            vo.setUsage(new AiUsageVO()
                    .inputTokens(request.getInputTokens())
                    .outputTokens(request.getOutputTokens())
                    .costUnits(request.getCostUnits())
                    .chargedCredits(request.getChargedCredits()));
        }
        return vo;
    }

    private AiConversation loadOwnConversation(final Integer conversationId) {
        Integer userId = loggedUserId();
        AiConversation conversation = aiConversationRepository.findById(conversationId)
                .filter(c -> userId.equals(c.getUserId()))
                .orElseThrow(() -> notFound("AI conversation not found: " + conversationId, conversationId));
        return conversation;
    }

    private AiExternalSystem loadExternalSystem(final AiConversation conversation) {
        return aiExternalSystemRepository.findById(conversation.getExternalSystemId())
                .orElseThrow(() -> notFound("AI external system not found", conversation.getExternalSystemId()));
    }

    private Integer loggedUserId() {
        UserDetail userDetail = userService.getLoggedUserDetail();
        if (userDetail == null || userDetail.getId() == null) {
            throw new AccessDeniedException("User not authorized.", Collections.emptyList());
        }
        return userDetail.getId();
    }

    private String loggedUsername() {
        UserDetail userDetail = userService.getLoggedUserDetail();
        return userDetail == null ? null : userDetail.getUsername();
    }

    private String deriveTitle(final AiConversationCreateVO vo) {
        if (StringUtils.isNotBlank(vo.getTitle())) {
            return StringUtils.abbreviate(vo.getTitle(), 250);
        }
        if (StringUtils.isNotBlank(vo.getUserInstructions())) {
            return StringUtils.abbreviate(vo.getUserInstructions(), 250);
        }
        return vo.getTaskType();
    }

    private void addEvent(final AiRequest request, final String eventType, final String data) {
        AiRequestEvent event = new AiRequestEvent();
        event.setAiRequestId(request.getAiRequestId());
        event.setEventType(eventType);
        event.setData(data);
        event.setCreateDate(new Date());
        aiRequestEventRepository.save(event);
    }

    private String toJson(final Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            return String.valueOf(value);
        }
    }

    private static OffsetDateTime toOffset(final Date date) {
        return date == null ? null
                : OffsetDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
    }

    private static boolean isTerminal(final String state) {
        return "done".equals(state) || "error".equals(state) || "cancelled".equals(state);
    }

    private static ObjectNotFoundException notFound(final String message, final Object id) {
        return new ObjectNotFoundException(message, BaseCode.ID_NOT_EXIST).setId(id);
    }
}
