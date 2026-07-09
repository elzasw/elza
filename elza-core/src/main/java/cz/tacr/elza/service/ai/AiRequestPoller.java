package cz.tacr.elza.service.ai;

import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;

import cz.tacr.elza.aiprovider.client.ElzaAiApi;
import cz.tacr.elza.aiprovider.client.vo.Task;
import cz.tacr.elza.domain.AiConversation;
import cz.tacr.elza.domain.AiExternalSystem;
import cz.tacr.elza.domain.AiRequest;
import cz.tacr.elza.domain.AiRequestEvent;
import cz.tacr.elza.repository.AiConversationRepository;
import cz.tacr.elza.repository.AiExternalSystemRepository;
import cz.tacr.elza.repository.AiRequestEventRepository;
import cz.tacr.elza.repository.AiRequestRepository;
import cz.tacr.elza.service.AiProviderService;
import cz.tacr.elza.service.IEventNotificationService;
import cz.tacr.elza.service.eventnotification.events.EventId;
import cz.tacr.elza.service.eventnotification.events.EventType;

/**
 * Monitors open AI requests: one worker per non-terminal request long-polls
 * the provider (protocol {@code GET /tasks/{id}?wait=30}), persists every
 * observed change ({@code ai_request} state/output/usage + OUTPUT/ERROR
 * events) and announces it over the WebSocket as {@code AI_REQUEST_CHANGE}
 * with the conversation id — the client then refetches the conversation via
 * REST. Polling of open requests resumes on application start, so a restart
 * loses nothing.
 */
@Component
public class AiRequestPoller {

    private static final Logger logger = LoggerFactory.getLogger(AiRequestPoller.class);

    /** Provider-side long-poll window (seconds); capped by the provider's limits. */
    private static final int WAIT_SECONDS = 30;

    /** Pause after a communication failure before the next attempt. */
    private static final long RETRY_PAUSE_MS = 15_000;

    private static final Set<String> TERMINAL_STATES = Set.of("done", "error", "cancelled");

    @Autowired
    private AiRequestRepository aiRequestRepository;

    @Autowired
    private AiConversationRepository aiConversationRepository;

    @Autowired
    private AiExternalSystemRepository aiExternalSystemRepository;

    @Autowired
    private AiProviderService aiProviderService;

    @Autowired
    private IEventNotificationService eventNotificationService;

    @Autowired
    private AiRequestEventRepository aiRequestEventRepository;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    private final Set<Integer> active = ConcurrentHashMap.newKeySet();

    private final ExecutorService executor = Executors.newFixedThreadPool(4, runnable -> {
        Thread thread = new Thread(runnable, "ai-poller-" + THREAD_SEQ.incrementAndGet());
        thread.setDaemon(true);
        return thread;
    });

    private static final AtomicInteger THREAD_SEQ = new AtomicInteger();

    /** Starts polling the request unless it is already being polled. */
    public void ensurePolling(final Integer aiRequestId) {
        if (active.add(aiRequestId)) {
            executor.submit(() -> pollLoop(aiRequestId));
        }
    }

    /** Resumes polling of all open requests after an application (re)start. */
    @EventListener(ApplicationReadyEvent.class)
    public void resumeOpenRequests() {
        List<AiRequest> open = aiRequestRepository.findByStateNotInAndTaskUidIsNotNull(TERMINAL_STATES);
        for (AiRequest request : open) {
            logger.info("Resuming polling of AI request {} (task {})", request.getAiRequestId(),
                        request.getTaskUid());
            ensurePolling(request.getAiRequestId());
        }
    }

    private void pollLoop(final Integer aiRequestId) {
        try {
            while (true) {
                PollTarget target = loadTarget(aiRequestId);
                if (target == null) {
                    return; // terminal, deleted or never submitted
                }
                Task task;
                try {
                    // The conversation owner's key (task visibility at the
                    // provider is per subscriber, but the owner's personal key
                    // may be the only one configured).
                    ElzaAiApi api = aiProviderService.createApi(target.externalSystem, target.userId);
                    task = api.getTask(java.time.OffsetDateTime.now(), target.taskUid, WAIT_SECONDS);
                } catch (Exception e) {
                    // e.getMessage() of the generated client's RestClientResponseException
                    // already carries the HTTP status and response body; the full stack
                    // (incl. the request context) is at DEBUG.
                    logger.warn("Polling of AI request {} (task {}, provider {} at {}) failed: {}",
                                aiRequestId, target.taskUid, target.externalSystem.getCode(),
                                target.externalSystem.getUrl(), e.getMessage());
                    logger.debug("Polling failure detail for AI request {}", aiRequestId, e);
                    Thread.sleep(RETRY_PAUSE_MS);
                    continue;
                }
                applyChange(aiRequestId, target, task);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            active.remove(aiRequestId);
        }
    }

    /** Snapshot needed for one poll; null ends the loop. */
    private PollTarget loadTarget(final Integer aiRequestId) {
        return transactionTemplate.execute(status -> {
            AiRequest request = aiRequestRepository.findById(aiRequestId).orElse(null);
            if (request == null || request.getTaskUid() == null
                    || TERMINAL_STATES.contains(request.getState())) {
                return null;
            }
            AiConversation conversation = aiConversationRepository
                    .findById(request.getAiConversationId()).orElse(null);
            if (conversation == null) {
                return null;
            }
            AiExternalSystem externalSystem = aiExternalSystemRepository
                    .findById(conversation.getExternalSystemId()).orElse(null);
            if (externalSystem == null) {
                return null;
            }
            return new PollTarget(request.getTaskUid(), request.getState(),
                    request.getCostUnits(), conversation.getAiConversationId(),
                    conversation.getUserId(), externalSystem);
        });
    }

    /** Persists the observed task state and publishes the change notification. */
    private void applyChange(final Integer aiRequestId, final PollTarget target, final Task task) {
        String newState = task.getState().getValue();
        double newCostUnits = task.getUsage() != null && task.getUsage().getCostUnits() != null
                ? task.getUsage().getCostUnits() : 0;
        if (Objects.equals(newState, target.state) && newCostUnits == target.costUnits) {
            return; // long poll expired without a change
        }
        transactionTemplate.executeWithoutResult(status -> {
            AiRequest request = aiRequestRepository.findById(aiRequestId).orElse(null);
            if (request == null || TERMINAL_STATES.contains(request.getState())) {
                return;
            }
            request.setState(newState);
            if (task.getUsage() != null) {
                request.setInputTokens(nvl(task.getUsage().getInputTokens()));
                request.setOutputTokens(nvl(task.getUsage().getOutputTokens()));
                request.setCostUnits(newCostUnits);
                // Protocol 1.2; a provider without credit accounting omits the
                // field — then credits = cost units (multiplier 1).
                request.setChargedCredits(task.getUsage().getChargedCredits() != null
                        ? task.getUsage().getChargedCredits() : newCostUnits);
            }
            request.setPromptVersion(task.getPromptVersion());
            if ("done".equals(newState)) {
                String outputJson = toJson(task.getOutput());
                request.setOutput(outputJson);
                request.setFinishDate(new Date());
                addEvent(request, AiRequestEvent.TYPE_OUTPUT, outputJson);
            } else if ("error".equals(newState)) {
                if (task.getError() != null) {
                    request.setErrorCode(task.getError().getCode());
                    request.setErrorMessage(task.getError().getMessage());
                }
                request.setFinishDate(new Date());
                addEvent(request, AiRequestEvent.TYPE_ERROR, toJson(task.getError()));
            } else if ("cancelled".equals(newState)) {
                request.setFinishDate(new Date());
            }
            aiRequestRepository.save(request);

            AiConversation conversation = aiConversationRepository
                    .findById(target.conversationId).orElse(null);
            if (conversation != null) {
                conversation.setLastChangeDate(new Date());
                aiConversationRepository.save(conversation);
            }

            eventNotificationService.publishEvent(
                    new EventId(EventType.AI_REQUEST_CHANGE, target.conversationId));
        });
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
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            return String.valueOf(value);
        }
    }

    private static long nvl(final Long value) {
        return value != null ? value : 0;
    }

    private record PollTarget(String taskUid, String state, double costUnits,
            Integer conversationId, Integer userId, AiExternalSystem externalSystem) {
    }
}
