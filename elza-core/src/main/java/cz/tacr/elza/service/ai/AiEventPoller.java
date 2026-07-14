package cz.tacr.elza.service.ai;

import java.time.OffsetDateTime;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.RestClientResponseException;

import com.fasterxml.jackson.databind.ObjectMapper;

import cz.tacr.elza.aiprovider.client.ElzaAiApi;
import cz.tacr.elza.aiprovider.client.vo.AiServiceInfo;
import cz.tacr.elza.aiprovider.client.vo.TaskEvent;
import cz.tacr.elza.aiprovider.client.vo.TaskEvents;
import cz.tacr.elza.domain.AiConversation;
import cz.tacr.elza.domain.AiExternalSystem;
import cz.tacr.elza.domain.AiRequest;
import cz.tacr.elza.domain.AiRequestEvent;
import cz.tacr.elza.repository.AiConversationRepository;
import cz.tacr.elza.repository.AiExternalSystemRepository;
import cz.tacr.elza.repository.AiRequestEventRepository;
import cz.tacr.elza.repository.AiRequestRepository;
import cz.tacr.elza.service.AiProviderService;

/**
 * Consumes the provider's advisory task-event stream (protocol 0.8,
 * {@code GET /tasks/{id}/events?since&wait}) for every open AI request: one
 * worker per request long-polls with the persisted cursor
 * ({@code ai_request.event_seq}), stores each event as an
 * {@link AiRequestEvent} row (audit trail; {@code event_type} = the lowercase
 * wire code, {@code data} = the wire event JSON), mirrors {@code phase} events
 * into the request's progress columns, accumulates {@code answer_delta} text
 * in the {@link AiAnswerBuffer}, and pushes the updated request snapshot to
 * the conversation owner ({@link cz.tacr.elza.websocket.UserEventPushService}).
 *
 * <p>Strictly an enhancement next to the authoritative task poll
 * ({@link AiRequestPoller}): the stream is advisory by contract — a provider
 * may emit no events at all — so nothing here participates in the request's
 * state machine. Providers below protocol 0.8 are skipped. The cursor advances
 * with each batch in the same transaction, so a restart resumes without
 * duplicating rows (the provider stream is replayable).
 */
@Component
public class AiEventPoller {

    private static final Logger logger = LoggerFactory.getLogger(AiEventPoller.class);

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
    private AiRequestViewMapper requestViewMapper;

    @Autowired
    private cz.tacr.elza.websocket.UserEventPushService pushService;

    @Autowired
    private AiAnswerBuffer answerBuffer;

    @Autowired
    private AiRequestEventRepository aiRequestEventRepository;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    private final Set<Integer> active = ConcurrentHashMap.newKeySet();

    private final ExecutorService executor = Executors.newFixedThreadPool(4, runnable -> {
        Thread thread = new Thread(runnable, "ai-event-poller-" + THREAD_SEQ.incrementAndGet());
        thread.setDaemon(true);
        return thread;
    });

    private static final AtomicInteger THREAD_SEQ = new AtomicInteger();

    /** Starts consuming the request's event stream unless already being consumed. */
    public void ensurePolling(final Integer aiRequestId) {
        if (active.add(aiRequestId)) {
            executor.submit(() -> pollLoop(aiRequestId));
        }
    }

    /** Resumes the event streams of all open requests after an application (re)start. */
    @EventListener(ApplicationReadyEvent.class)
    public void resumeOpenRequests() {
        List<AiRequest> open = aiRequestRepository.findByStateNotInAndTaskUidIsNotNull(TERMINAL_STATES);
        for (AiRequest request : open) {
            ensurePolling(request.getAiRequestId());
        }
    }

    private void pollLoop(final Integer aiRequestId) {
        try {
            PollTarget first = loadTarget(aiRequestId);
            if (first == null || !supportsTaskEvents(first)) {
                return;
            }
            while (true) {
                PollTarget target = loadTarget(aiRequestId);
                if (target == null) {
                    return; // terminal, deleted or never submitted
                }
                TaskEvents batch;
                try {
                    ElzaAiApi api = aiProviderService.createApi(target.externalSystem, target.userId);
                    batch = api.getTaskEvents(OffsetDateTime.now(), target.taskUid,
                            target.eventSeq, WAIT_SECONDS);
                } catch (RestClientResponseException e) {
                    if (e.getStatusCode().value() == 404 || e.getStatusCode().value() == 405) {
                        // Task unknown at the provider, or a provider that advertises
                        // 0.8 without the endpoint. Advisory stream only - stop; the
                        // authoritative task poll keeps running.
                        logger.warn("Event stream of AI request {} (task {}, provider {}) unavailable"
                                + " (HTTP {}); event polling stopped", aiRequestId, target.taskUid,
                                target.externalSystem.getCode(), e.getStatusCode().value());
                        return;
                    }
                    logger.warn("Event poll of AI request {} (task {}, provider {} at {}) failed: {}",
                            aiRequestId, target.taskUid, target.externalSystem.getCode(),
                            target.externalSystem.getUrl(), e.getMessage());
                    logger.debug("Event poll failure detail for AI request {}", aiRequestId, e);
                    Thread.sleep(RETRY_PAUSE_MS);
                    continue;
                } catch (Exception e) {
                    logger.warn("Event poll of AI request {} (task {}, provider {} at {}) failed: {}",
                            aiRequestId, target.taskUid, target.externalSystem.getCode(),
                            target.externalSystem.getUrl(), e.getMessage());
                    logger.debug("Event poll failure detail for AI request {}", aiRequestId, e);
                    Thread.sleep(RETRY_PAUSE_MS);
                    continue;
                }
                AiRequestUpdateMessage message = applyBatch(aiRequestId, batch);
                pushService.push(target.userId(), message);
                if (batch.getState() != null
                        && TERMINAL_STATES.contains(batch.getState().getValue())) {
                    // The final lifecycle event is delivered with the terminal state;
                    // the task poll stores the authoritative outcome and clears the
                    // answer buffer.
                    return;
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            active.remove(aiRequestId);
        }
    }

    /**
     * True when the provider speaks protocol 0.8+ (task events). Read from the
     * cached {@code GET /info}; when the catalog is unreachable the stream is
     * assumed supported and the poll's own error handling decides.
     */
    private boolean supportsTaskEvents(final PollTarget target) {
        String protocolVersion;
        try {
            AiServiceInfo info = aiProviderService.fetchServiceInfo(target.externalSystem, target.userId);
            protocolVersion = info.getProtocolVersion();
        } catch (Exception e) {
            logger.debug("Provider info unavailable for event-support check: {}", e.getMessage());
            return true;
        }
        if (StringUtils.isBlank(protocolVersion)) {
            return false;
        }
        String[] parts = protocolVersion.split("\\.");
        try {
            int major = Integer.parseInt(parts[0].trim());
            int minor = parts.length > 1 ? Integer.parseInt(parts[1].trim()) : 0;
            boolean supported = major > 0 || minor >= 8;
            if (!supported) {
                logger.info("AI provider {} (protocol {}) has no task-event stream; event polling skipped",
                        target.externalSystem.getCode(), protocolVersion);
            }
            return supported;
        } catch (NumberFormatException e) {
            return false;
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
            return new PollTarget(request.getTaskUid(), request.getEventSeq(),
                    conversation.getUserId(), externalSystem);
        });
    }

    /**
     * Persists one event batch and advances the cursor in a single transaction:
     * every event becomes an {@link AiRequestEvent} row except
     * {@code answer_delta} (in-memory buffer only — provisional text whose
     * durable form is the request's output); {@code phase} events additionally
     * update the progress columns. Returns the push message, or null when the
     * batch was empty (long-poll timeout).
     */
    private AiRequestUpdateMessage applyBatch(final Integer aiRequestId, final TaskEvents batch) {
        List<TaskEvent> events = batch.getEvents();
        if (events == null || events.isEmpty()) {
            return null;
        }
        return transactionTemplate.execute(status -> {
            AiRequest request = aiRequestRepository.findById(aiRequestId).orElse(null);
            if (request == null) {
                return null;
            }
            boolean terminal = TERMINAL_STATES.contains(request.getState());
            long cursor = request.getEventSeq();
            for (TaskEvent event : events) {
                if (event.getSeq() == null || event.getSeq() <= request.getEventSeq()) {
                    continue; // defensive: never store an event twice
                }
                if (AiRequestEvent.TYPE_PROVIDER_ANSWER_DELTA.equals(event.getType())) {
                    answerBuffer.append(aiRequestId, event.getMessage());
                } else {
                    addEvent(request, event);
                    if (AiRequestEvent.TYPE_PROVIDER_PHASE.equals(event.getType()) && !terminal) {
                        request.setProgressMessage(event.getMessage());
                        request.setProgressPercent(event.getPercent() != null
                                ? event.getPercent().doubleValue() : null);
                    }
                }
                cursor = Math.max(cursor, event.getSeq());
            }
            if (cursor == request.getEventSeq()) {
                return null; // nothing new
            }
            request.setEventSeq(batch.getNextSince() != null ? batch.getNextSince() : cursor);
            aiRequestRepository.save(request);
            return requestViewMapper.buildUpdateMessage(request);
        });
    }

    /** Stores one wire event as a transparency-log row (local receive time orders the log). */
    private void addEvent(final AiRequest request, final TaskEvent event) {
        AiRequestEvent row = new AiRequestEvent();
        row.setAiRequestId(request.getAiRequestId());
        row.setEventType(StringUtils.left(event.getType(), 50));
        row.setData(toJson(event));
        row.setCreateDate(new Date());
        aiRequestEventRepository.save(row);
    }

    private String toJson(final TaskEvent event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (Exception e) {
            return String.valueOf(event);
        }
    }

    private record PollTarget(String taskUid, long eventSeq, Integer userId,
            AiExternalSystem externalSystem) {
    }
}
