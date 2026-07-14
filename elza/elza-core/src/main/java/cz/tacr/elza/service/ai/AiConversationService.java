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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.beans.factory.annotation.Value;

import cz.tacr.elza.aiprovider.client.vo.AiObject;
import cz.tacr.elza.aiprovider.client.vo.AiServiceInfo;
import cz.tacr.elza.aiprovider.client.vo.SubmitTask;
import cz.tacr.elza.aiprovider.client.vo.TaskAccepted;
import cz.tacr.elza.aiprovider.client.vo.TaskMetadata;
import cz.tacr.elza.aiprovider.client.vo.TaskParameterInfo;
import cz.tacr.elza.aiprovider.client.vo.TaskTypeInfo;
import cz.tacr.elza.controller.vo.AiContextObjectVO;
import cz.tacr.elza.controller.vo.AiConversationCreateVO;
import cz.tacr.elza.controller.vo.AiConversationDetailVO;
import cz.tacr.elza.controller.vo.AiConversationVO;
import cz.tacr.elza.controller.vo.AiRequestActivityVO;
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
 * <p>Task parameters: the exchange is submitted with the provider's typed
 * parameters (see {@link #buildParameters}), built from the context objects the
 * UI supplies; the user's free-form text travels in {@code userInstructions}.
 * Task types whose parameter object types Elza cannot yet marshal are simply
 * not offered.
 */
@Service
public class AiConversationService {

    private static final Logger logger = LoggerFactory.getLogger(AiConversationService.class);

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
    private AiActivityMapper activityMapper;

    @Autowired
    private AiRequestPoller aiRequestPoller;

    @Autowired
    private UserService userService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AiContextResolver contextResolver;

    @Autowired
    private AiToolRegistry toolRegistry;

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
        conversation.setContextType(primaryContextType(vo.getContext()));
        conversation.setContext(serializeContext(vo.getContext()));
        Date now = new Date();
        conversation.setCreateDate(now);
        conversation.setLastChangeDate(now);
        aiConversationRepository.save(conversation);

        submitExchange(conversation, externalSystem, vo.getTaskType(), vo.getProfile(),
                       vo.getUserInstructions(), vo.getParameters(), vo.getContext(), null);
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

        // A follow-up may move the user's context; when it carries one, it
        // replaces the conversation's stored context, otherwise the stored one
        // is reused for this exchange.
        List<AiContextObjectVO> context = vo.getContext();
        if (context != null) {
            conversation.setContextType(primaryContextType(context));
            conversation.setContext(serializeContext(context));
        } else {
            context = deserializeContext(conversation.getContext());
        }
        conversation.setLastChangeDate(new Date());
        aiConversationRepository.save(conversation);

        String profile = vo.getProfile() != null ? vo.getProfile() : last.getProfile();
        submitExchange(conversation, externalSystem, last.getTaskType(), profile,
                       vo.getUserInstructions(), vo.getParameters(), context, last.getTaskUid());
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
                logger.debug("Best-effort cancel of AI task {} failed: {}",
                        request.getTaskUid(), e.getMessage());
            }
            addEvent(request, AiRequestEvent.TYPE_CANCEL, null);
        }
        return toVO(request, aiRequestEventRepository
                .findByAiRequestIdInOrderByCreateDateAscAiRequestEventIdAsc(
                        List.of(request.getAiRequestId())));
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
                                final String taskType, final String profile, final String userInstructions,
                                final List<AiContextObjectVO> parameters, final List<AiContextObjectVO> context,
                                final String parentTaskUid) {
        AiRequest request = new AiRequest();
        request.setAiConversationId(conversation.getAiConversationId());
        request.setRequestId(UUID.randomUUID().toString());
        request.setTaskType(taskType);
        request.setProfile(profile);
        request.setState("queued");
        request.setUserInstructions(userInstructions);
        request.setParameters(serializeContext(parameters));
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
                .profile(profile)
                .userInstructions(userInstructions)
                .parameters(buildParameters(taskType, parameters, externalSystem, conversation.getUserId()))
                .context(contextResolver.resolveAll(context))
                .tools(toolRegistry.toolNames())
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
            // The exchange is not lost: it is stored in state "error" so the user
            // sees the failure in the thread. e.getMessage() of the generated
            // client's RestClientResponseException includes the HTTP status and body.
            logger.warn("AI task submit failed for conversation {} (request {}, taskType {},"
                    + " provider {} at {}): {}",
                    conversation.getAiConversationId(), request.getRequestId(), taskType,
                    externalSystem.getCode(), externalSystem.getUrl(), e.getMessage());
            logger.debug("AI task submit failure detail (request {})", request.getRequestId(), e);
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

    /**
     * Builds the provider's typed task parameters (a name→object map). Each
     * context object the UI supplied under {@code parameters} is resolved to a
     * provider object and assigned to the task's declared parameter whose object
     * type matches (looked up from the provider's {@code GET /info}); a resolved
     * object with no matching declared parameter is skipped.
     */
    private Map<String, AiObject> buildParameters(final String taskType,
                                                  final List<AiContextObjectVO> parameterContext,
                                                  final AiExternalSystem externalSystem,
                                                  final Integer userId) {
        Map<String, AiObject> parameters = new HashMap<>();
        // A parameter is one object per supplied context; resolvePrimary yields
        // the single primary object (a node → its own level, no ancestors/fund).
        List<AiObject> resolved = new ArrayList<>();
        if (parameterContext != null) {
            for (AiContextObjectVO ctx : parameterContext) {
                contextResolver.resolvePrimary(ctx).ifPresent(resolved::add);
            }
        }
        if (!resolved.isEmpty()) {
            List<TaskParameterInfo> declared = declaredParameters(externalSystem, taskType, userId);
            for (AiObject object : resolved) {
                declared.stream()
                        .filter(p -> object.getObjectType().equals(p.getType()))
                        .findFirst()
                        .ifPresentOrElse(
                                p -> parameters.put(p.getName(), object),
                                () -> logger.info("Task {} declares no parameter of type {}; context object skipped",
                                        taskType, object.getObjectType()));
            }
        }
        return parameters;
    }

    /** The task's declared parameters from the provider's catalog; empty when unavailable. */
    private List<TaskParameterInfo> declaredParameters(final AiExternalSystem externalSystem, final String taskType,
                                                       final Integer userId) {
        try {
            AiServiceInfo info = aiProviderService.fetchServiceInfo(externalSystem, userId);
            if (info.getTaskTypes() != null) {
                for (TaskTypeInfo type : info.getTaskTypes()) {
                    if (taskType.equals(type.getCode()) && type.getParameters() != null) {
                        return type.getParameters();
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("Could not read AI provider info to match parameters for task {}: {}",
                    taskType, e.getMessage());
        }
        return List.of();
    }

    /** Serializes context objects to JSON with the element type so the discriminator is kept. */
    private String serializeContext(final List<AiContextObjectVO> context) {
        if (context == null || context.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writerFor(new TypeReference<List<AiContextObjectVO>>() { })
                    .writeValueAsString(context);
        } catch (Exception e) {
            logger.warn("Failed to serialize AI context objects: {}", e.getMessage());
            return null;
        }
    }

    /** Reads stored context JSON back into typed objects; null/blank yields null. */
    private List<AiContextObjectVO> deserializeContext(final String json) {
        if (StringUtils.isBlank(json)) {
            return null;
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<AiContextObjectVO>>() { });
        } catch (Exception e) {
            logger.warn("Failed to deserialize AI context objects: {}", e.getMessage());
            return null;
        }
    }

    /** {@code AiContextType} of the first context object (for conversation list filtering), or null. */
    private String primaryContextType(final List<AiContextObjectVO> context) {
        if (context == null || context.isEmpty() || context.get(0).getType() == null) {
            return null;
        }
        return context.get(0).getType().getValue();
    }

    // -----------------------------------------------------------------------
    // Mapping & helpers
    // -----------------------------------------------------------------------

    private AiConversationDetailVO getDetail(final AiConversation conversation,
                                             final AiExternalSystem externalSystem) {
        List<AiRequest> stored = aiRequestRepository
                .findByAiConversationIdOrderByCreateDateAsc(conversation.getAiConversationId());
        Map<Integer, List<AiRequestEvent>> eventsByRequest = loadEvents(stored);
        List<AiRequestVO> requests = new ArrayList<>();
        for (AiRequest request : stored) {
            requests.add(toVO(request, eventsByRequest.getOrDefault(request.getAiRequestId(), List.of())));
        }
        return new AiConversationDetailVO()
                .conversation(toVO(conversation, externalSystem))
                .requests(requests);
    }

    /** Event logs of the given requests (one query), grouped by request id in stable order. */
    private Map<Integer, List<AiRequestEvent>> loadEvents(final List<AiRequest> requests) {
        if (requests.isEmpty()) {
            return Map.of();
        }
        List<Integer> ids = requests.stream().map(AiRequest::getAiRequestId).toList();
        Map<Integer, List<AiRequestEvent>> byRequest = new HashMap<>();
        for (AiRequestEvent event : aiRequestEventRepository
                .findByAiRequestIdInOrderByCreateDateAscAiRequestEventIdAsc(ids)) {
            byRequest.computeIfAbsent(event.getAiRequestId(), k -> new ArrayList<>()).add(event);
        }
        return byRequest;
    }

    private AiConversationVO toVO(final AiConversation conversation, final AiExternalSystem externalSystem) {
        return new AiConversationVO()
                .id(conversation.getAiConversationId())
                .externalSystemCode(externalSystem.getCode())
                .title(conversation.getTitle())
                .contextType(conversation.getContextType())
                .context(deserializeContext(conversation.getContext()))
                .createDate(toOffset(conversation.getCreateDate()))
                .lastChangeDate(toOffset(conversation.getLastChangeDate()));
    }

    private AiRequestVO toVO(final AiRequest request, final List<AiRequestEvent> events) {
        AiRequestVO vo = new AiRequestVO()
                .id(request.getAiRequestId())
                .taskType(request.getTaskType())
                .state(request.getState())
                .userInstructions(request.getUserInstructions())
                .errorCode(request.getErrorCode())
                .errorMessage(request.getErrorMessage())
                .promptVersion(request.getPromptVersion())
                .profile(request.getProfile())
                .createDate(toOffset(request.getCreateDate()))
                .finishDate(toOffset(request.getFinishDate()));
        if (!isTerminal(request.getState())) {
            vo.setProgressMessage(request.getProgressMessage());
            vo.setProgressPercent(request.getProgressPercent() != null
                    ? request.getProgressPercent().floatValue() : null);
        }
        List<AiRequestActivityVO> activities = activityMapper.map(events);
        if (!activities.isEmpty()) {
            vo.setActivities(activities);
        }
        if ("done".equals(request.getState()) && request.getOutput() != null) {
            vo.setBlocks(blockMapperRegistry.map(request.getOutput()));
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
