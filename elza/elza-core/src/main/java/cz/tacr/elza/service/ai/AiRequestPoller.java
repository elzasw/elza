package cz.tacr.elza.service.ai;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import cz.tacr.elza.aiprovider.client.ElzaAiApi;
import cz.tacr.elza.aiprovider.client.vo.AiObject;
import cz.tacr.elza.aiprovider.client.vo.SubmitToolResultsRequest;
import cz.tacr.elza.aiprovider.client.vo.Task;
import cz.tacr.elza.aiprovider.client.vo.ToolCall;
import cz.tacr.elza.aiprovider.client.vo.ToolResult;
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
 * Monitors open AI requests: one worker per non-terminal request long-polls
 * the provider (protocol {@code GET /tasks/{id}?wait=30}), persists every
 * observed change ({@code ai_request} state/output/usage + OUTPUT/ERROR
 * events) and pushes the updated request snapshot to the conversation owner
 * ({@link AiRequestPushService} — committed data rendered as the owner).
 * Polling of open
 * requests resumes on application start, so a restart loses nothing.
 *
 * <p>This poll is the authoritative state machine of an exchange. The finer
 * advisory activity (tool-by-tool events, streamed answer) is consumed
 * separately by {@link AiEventPoller}; a provider that emits no events still
 * works fully through this loop.
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
    private AiRequestPushService requestPushService;

    @Autowired
    private AiAnswerBuffer answerBuffer;

    @Autowired
    private AiRequestEventRepository aiRequestEventRepository;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AiToolRegistry toolRegistry;

    /**
     * Give up polling a request after this many seconds of <em>uninterrupted</em>
     * poll failures (provider unreachable, or the task gone — a {@code 404}), and
     * mark the exchange {@code error}/{@code TIMEOUT}. A single successful poll
     * resets the window, so a task that is legitimately still running (the model
     * is thinking, tools are executing) is never given up on — only one the
     * provider can no longer answer for. Default 5 minutes.
     */
    @Value("${elza.ai.poll-failure-timeout-seconds:300}")
    private long pollFailureTimeoutSeconds;

    /**
     * Absolute backstop: give up on a request still open this many seconds after
     * it was submitted, regardless of whether the provider is answering polls, and
     * mark the exchange {@code error}/{@code TIMEOUT}. Guards against a provider
     * that keeps a task {@code running} forever without ever finishing it (the
     * failure-window give-up above never triggers then, because the polls
     * succeed). Deliberately generous so a legitimately long exchange is never
     * cut off. Default 30 minutes.
     */
    @Value("${elza.ai.request-lifetime-timeout-seconds:1800}")
    private long requestLifetimeTimeoutSeconds;

    private final Set<Integer> active = ConcurrentHashMap.newKeySet();

    private final ExecutorService executor = Executors.newFixedThreadPool(4, runnable -> {
        Thread thread = new Thread(runnable, "ai-poller-" + THREAD_SEQ.incrementAndGet());
        thread.setDaemon(true);
        // Backstop for anything the poll loop's own handler cannot catch (an
        // Error, or a failure raised outside it). Submitted tasks bury their
        // throwable in a Future nobody reads, so without this a dying poller
        // thread leaves no trace whatsoever.
        thread.setUncaughtExceptionHandler((t, e) ->
                logger.error("AI poller thread {} died", t.getName(), e));
        return thread;
    });

    private static final AtomicInteger THREAD_SEQ = new AtomicInteger();

    /** Starts polling the request unless it is already being polled. */
    public void ensurePolling(final Integer aiRequestId) {
        if (active.add(aiRequestId)) {
            executor.submit(() -> pollLoop(aiRequestId));
        }
    }

    /**
     * Resumes polling of all open requests after an application (re)start. First
     * settles the non-terminal requests a restart can never advance
     * ({@link #failUnresumableRequests}), so they do not linger as perpetually
     * "running" exchanges; the rest are handed back to the poll, losing nothing.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void resumeOpenRequests() {
        failUnresumableRequests();
        List<AiRequest> open = aiRequestRepository.findByStateNotInAndTaskUidIsNotNull(TERMINAL_STATES);
        for (AiRequest request : open) {
            logger.info("Resuming polling of AI request {} (task {})", request.getAiRequestId(),
                        request.getTaskUid());
            ensurePolling(request.getAiRequestId());
        }
    }

    /**
     * Settles every non-terminal request the poll can never advance, so it does
     * not stay "running" (with spinning tool steps) forever after a restart:
     *
     * <ul>
     * <li>no {@code taskUid} — the task was never accepted by a provider, so
     *     there is nothing to poll (also excluded from the resume query);</li>
     * <li>its conversation or its AI provider is gone — {@link #loadTarget} can
     *     no longer build a poll target, so the poll loop would exit at once and
     *     the state would never change.</li>
     * </ul>
     *
     * A request that IS resumable (a live {@code taskUid} with a resolvable
     * provider) is left untouched — the poll picks it up and a restart loses
     * nothing, as designed.
     */
    private void failUnresumableRequests() {
        for (AiRequest request : aiRequestRepository.findByStateNotIn(TERMINAL_STATES)) {
            String reason = unresumableReason(request);
            if (reason != null) {
                markInterrupted(request.getAiRequestId(), reason);
            }
        }
    }

    /** Why the poll can never advance this request, or {@code null} when it is resumable. */
    private String unresumableReason(final AiRequest request) {
        if (request.getTaskUid() == null) {
            return "the task was never submitted to a provider";
        }
        AiConversation conversation = aiConversationRepository
                .findById(request.getAiConversationId()).orElse(null);
        if (conversation == null) {
            return "its conversation no longer exists";
        }
        if (aiExternalSystemRepository.findById(conversation.getExternalSystemId()).isEmpty()) {
            return "its AI provider is no longer configured";
        }
        return null;
    }

    /**
     * Marks a stranded request terminally interrupted ({@code error} /
     * {@code INTERRUPTED}) — the poll can never advance it (§
     * {@link #failUnresumableRequests}).
     */
    private void markInterrupted(final Integer aiRequestId, final String reason) {
        if (failRequest(aiRequestId, "INTERRUPTED",
                "The exchange was interrupted and could not be resumed after a restart (" + reason + ").")) {
            logger.info("AI request {} marked interrupted at startup: {}", aiRequestId, reason);
        }
    }

    /**
     * Marks a request terminally failed ({@code error} with the given code and
     * message), records the ERROR transparency event, clears the partial-answer
     * buffer and notifies the owner. Re-reads the row in its own transaction and
     * skips a request that has meanwhile become terminal, so it never races an
     * in-flight poll. Returns whether the request was actually settled.
     */
    private boolean failRequest(final Integer aiRequestId, final String errorCode, final String message) {
        Integer[] owner = new Integer[1];
        // Settle first, render after (see applyChange): a request must never
        // stay open because drawing its error card failed.
        Boolean settled = transactionTemplate.execute(status -> {
            AiRequest request = aiRequestRepository.findById(aiRequestId).orElse(null);
            if (request == null || TERMINAL_STATES.contains(request.getState())) {
                return Boolean.FALSE;
            }
            request.setState("error");
            request.setErrorCode(errorCode);
            request.setErrorMessage(message);
            // Advisory progress is display state of a running task; a finished
            // exchange shows its outcome, not the last phase.
            request.setProgressMessage(null);
            request.setProgressPercent(null);
            request.setFinishDate(new Date());
            aiRequestRepository.save(request);
            addEvent(request, AiRequestEvent.TYPE_ERROR,
                    toJson(Map.of("errorCode", errorCode, "message", message)));
            aiConversationRepository.findById(request.getAiConversationId())
                    .ifPresent(conversation -> owner[0] = conversation.getUserId());
            return Boolean.TRUE;
        });
        if (!Boolean.TRUE.equals(settled)) {
            return false;
        }
        answerBuffer.clear(aiRequestId);
        requestPushService.pushUpdate(aiRequestId, owner[0]);
        return true;
    }

    private void pollLoop(final Integer aiRequestId) {
        // Start of the current uninterrupted run of poll failures (0 = none in
        // progress); a successful poll resets it, so the give-up window measures
        // only continuous unreachability, never a slow-but-healthy task.
        long failingSinceMs = 0;
        try {
            while (true) {
                PollTarget target = loadTarget(aiRequestId);
                if (target == null) {
                    return; // terminal, deleted or never submitted
                }
                // Absolute lifetime backstop: an exchange open past the limit is
                // given up on even while the provider still answers polls (a task
                // stuck 'running' forever). Checked before the long poll so an
                // already-expired request settles at once; also catches a request
                // resumed after a long downtime.
                if (target.createDate() != null) {
                    long ageMs = System.currentTimeMillis() - target.createDate().getTime();
                    if (ageMs >= requestLifetimeTimeoutSeconds * 1000L) {
                        long ageSeconds = ageMs / 1000;
                        logger.warn("Giving up on AI request {} (task {}, provider {}): open for {} s"
                                + " (>= elza.ai.request-lifetime-timeout-seconds={}); marking the exchange"
                                + " timed out", aiRequestId, target.taskUid, target.externalSystem.getCode(),
                                ageSeconds, requestLifetimeTimeoutSeconds);
                        failRequest(aiRequestId, "TIMEOUT", "The exchange exceeded its maximum lifetime of "
                                + requestLifetimeTimeoutSeconds + " s and was timed out.");
                        return;
                    }
                }
                Task task;
                ElzaAiApi api;
                try {
                    // The conversation owner's key (task visibility at the
                    // provider is per subscriber, but the owner's personal key
                    // may be the only one configured).
                    api = aiProviderService.createApi(target.externalSystem, target.userId);
                    task = api.getTask(java.time.OffsetDateTime.now(), target.taskUid, WAIT_SECONDS);
                } catch (Exception e) {
                    // e.getMessage() of the generated client's RestClientResponseException
                    // already carries the HTTP status and response body; the full stack
                    // (incl. the request context) is at DEBUG.
                    logger.warn("Polling of AI request {} (task {}, provider {} at {}) failed: {}",
                                aiRequestId, target.taskUid, target.externalSystem.getCode(),
                                target.externalSystem.getUrl(), e.getMessage());
                    logger.debug("Polling failure detail for AI request {}", aiRequestId, e);
                    long now = System.currentTimeMillis();
                    if (failingSinceMs == 0) {
                        failingSinceMs = now;
                    }
                    long failingForMs = now - failingSinceMs;
                    if (failingForMs >= pollFailureTimeoutSeconds * 1000L) {
                        // The task has been unreachable past the give-up window
                        // (the provider forgot/expired it, or is down). Stop
                        // retrying and settle the exchange, so it does not stay
                        // "running" forever.
                        long seconds = failingForMs / 1000;
                        logger.warn("Giving up on AI request {} (task {}, provider {}): polling has failed"
                                + " continuously for {} s (>= elza.ai.poll-failure-timeout-seconds={});"
                                + " marking the exchange timed out",
                                aiRequestId, target.taskUid, target.externalSystem.getCode(), seconds,
                                pollFailureTimeoutSeconds);
                        failRequest(aiRequestId, "TIMEOUT", "The AI provider stopped responding; the exchange"
                                + " timed out after " + seconds + " s of failed polling.");
                        return;
                    }
                    Thread.sleep(RETRY_PAUSE_MS);
                    continue;
                }
                failingSinceMs = 0; // a reachable task resets the give-up window
                // The task paused for client-side tools: execute them, return the
                // results, and keep polling the resumed task.
                if ("awaiting_tools".equals(task.getState().getValue())) {
                    handleToolCalls(aiRequestId, target, task, api);
                    continue;
                }
                applyChange(aiRequestId, target, task);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (RuntimeException e) {
            // Nothing else would ever report this. The loop runs on an executor
            // whose Future nobody inspects, so an unchecked exception used to
            // end the thread in complete silence: the request stayed open, no
            // line was logged, and the user learned of it half an hour later as
            // a TIMEOUT that named the wrong cause (2026-08-07). An unexpected
            // failure must be loud AND must settle the exchange it abandoned.
            logger.error("Polling of AI request {} failed unexpectedly; settling the exchange as failed",
                         aiRequestId, e);
            try {
                failRequest(aiRequestId, "INTERNAL", "The exchange failed while being processed: "
                        + (e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()));
            } catch (RuntimeException settleFailure) {
                // Even the settle failed — log it rather than lose it; the
                // restart sweep is then the last resort for this request.
                logger.error("Settling AI request {} after the failure above also failed", aiRequestId,
                             settleFailure);
            }
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
                    request.getCostUnits(), request.getProgressMessage(), request.getProgressPercent(),
                    conversation.getAiConversationId(), conversation.getUserId(), externalSystem,
                    request.getCreateDate());
        });
    }

    /** Persists the observed task state and pushes the updated snapshot to the owner. */
    private void applyChange(final Integer aiRequestId, final PollTarget target, final Task task) {
        String newState = task.getState().getValue();
        double newCostUnits = task.getUsage() != null && task.getUsage().getCostUnits() != null
                ? task.getUsage().getCostUnits() : 0;
        String newProgressMessage = task.getProgress() != null ? task.getProgress().getMessage() : null;
        Double newProgressPercent = task.getProgress() != null && task.getProgress().getPercent() != null
                ? task.getProgress().getPercent().doubleValue() : null;
        if (Objects.equals(newState, target.state) && newCostUnits == target.costUnits
                && Objects.equals(newProgressMessage, target.progressMessage)
                && Objects.equals(newProgressPercent, target.progressPercent)) {
            return; // long poll expired without a change
        }
        transactionTemplate.execute(status -> {
            AiRequest request = aiRequestRepository.findById(aiRequestId).orElse(null);
            if (request == null || TERMINAL_STATES.contains(request.getState())) {
                return null;
            }
            request.setState(newState);
            // Advisory progress is display state of a running task; a finished
            // exchange shows its result, not the last phase.
            if (TERMINAL_STATES.contains(newState)) {
                request.setProgressMessage(null);
                request.setProgressPercent(null);
            } else {
                request.setProgressMessage(newProgressMessage);
                request.setProgressPercent(newProgressPercent);
            }
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
                String outputJson = toJsonOutput(task.getOutput());
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

            if (TERMINAL_STATES.contains(newState)) {
                answerBuffer.clear(aiRequestId);
            }
            return null;
        });
        // The view is built AFTER the result is committed, never inside that
        // transaction: rendering is derived, the provider's answer is
        // authoritative. Mapping a result block runs real logic (the proposal
        // block re-reads the level and validates every change against it), and
        // when that ran inside the persisting transaction a mapper failure
        // rolled the received result back — the exchange then hung until its
        // lifetime backstop reported a misleading TIMEOUT (2026-08-07).
        requestPushService.pushUpdate(aiRequestId, target.userId());
    }

    /**
     * Executes the pending tool calls of an {@code awaiting_tools} task and
     * returns the results to the provider so it can resume. Each call is
     * dispatched to the matching {@link AiTool}; a missing tool or a failing
     * execution becomes a {@code ToolResult.error} (the model decides how to
     * proceed). The calls and the results are recorded as request events for the
     * transparency log. Submitting is retry-safe, so a failure here simply lets
     * the next poll re-observe {@code awaiting_tools} and try again.
     */
    private void handleToolCalls(final Integer aiRequestId, final PollTarget target,
                                 final Task task, final ElzaAiApi api) throws InterruptedException {
        List<ToolCall> calls = task.getToolCalls();
        if (calls == null || calls.isEmpty()) {
            // awaiting_tools without any calls — nothing to answer; back off so
            // the loop does not spin until the provider advances the task.
            Thread.sleep(RETRY_PAUSE_MS);
            return;
        }
        recordToolEvent(aiRequestId, target, AiRequestEvent.TYPE_TOOL_CALLS, toJson(calls));
        // Tools run as the conversation owner — permission-scoped tools (e.g.
        // searchNodes) enforce that user's read permissions.
        AiToolContext toolContext = new AiToolContext(target.userId);
        List<ToolResult> results = new ArrayList<>(calls.size());
        for (ToolCall call : calls) {
            results.add(executeToolCall(call, toolContext));
        }
        recordToolEvent(aiRequestId, target, AiRequestEvent.TYPE_TOOL_RESULTS, toJson(results));
        try {
            api.submitToolResults(java.time.OffsetDateTime.now(), target.taskUid,
                    new SubmitToolResultsRequest().results(results));
        } catch (Exception e) {
            logger.warn("Submitting tool results for AI request {} (task {}, provider {}) failed: {}",
                        aiRequestId, target.taskUid, target.externalSystem.getCode(), e.getMessage());
            logger.debug("Tool-results submit failure detail for AI request {}", aiRequestId, e);
            Thread.sleep(RETRY_PAUSE_MS);
        }
    }

    /** Runs one tool call, capturing success as {@code result} or failure as {@code error}. */
    private ToolResult executeToolCall(final ToolCall call, final AiToolContext toolContext) {
        ToolResult result = new ToolResult().callId(call.getCallId());
        AiTool tool = toolRegistry.get(call.getTool());
        if (tool == null) {
            return result.error("Unknown tool: " + call.getTool());
        }
        try {
            return result.result(tool.execute(call.getArguments(), toolContext));
        } catch (Exception e) {
            logger.warn("AI tool {} failed (call {}): {}", call.getTool(), call.getCallId(), e.getMessage());
            logger.debug("AI tool {} failure detail", call.getTool(), e);
            return result.error(e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName());
        }
    }

    /**
     * Persists a tool event and pushes the updated snapshot (transparency-log
     * update). The event is committed first and rendered afterwards, for the
     * same reason as in {@link #applyChange}: a rendering failure must not roll
     * back the recorded fact.
     */
    private void recordToolEvent(final Integer aiRequestId, final PollTarget target,
                                 final String eventType, final String data) {
        transactionTemplate.execute(status -> {
            AiRequest request = aiRequestRepository.findById(aiRequestId).orElse(null);
            if (request != null) {
                addEvent(request, eventType, data);
            }
            return null;
        });
        requestPushService.pushUpdate(aiRequestId, target.userId());
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

    /**
     * Serializes the provider output with the element type so Jackson writes each
     * block's {@code objectType} discriminator; a raw {@code List<AiObject>} loses
     * it to generic erasure (elements would serialize as their bare concrete type),
     * leaving the stored blocks unmappable.
     */
    private String toJsonOutput(final List<AiObject> output) {
        if (output == null) {
            return null;
        }
        try {
            return objectMapper.writerFor(new TypeReference<List<AiObject>>() { }).writeValueAsString(output);
        } catch (Exception e) {
            return String.valueOf(output);
        }
    }

    private static long nvl(final Long value) {
        return value != null ? value : 0;
    }

    private record PollTarget(String taskUid, String state, double costUnits,
            String progressMessage, Double progressPercent,
            Integer conversationId, Integer userId,
            AiExternalSystem externalSystem, Date createDate) {
    }
}
