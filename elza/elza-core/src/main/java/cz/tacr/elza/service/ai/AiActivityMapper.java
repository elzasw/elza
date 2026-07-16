package cz.tacr.elza.service.ai;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import cz.tacr.elza.controller.vo.AiActivityLinkVO;
import cz.tacr.elza.controller.vo.AiContextAccesspointVO;
import cz.tacr.elza.controller.vo.AiContextNodeVO;
import cz.tacr.elza.controller.vo.AiContextTypeVO;
import cz.tacr.elza.controller.vo.AiRequestActivityVO;
import cz.tacr.elza.domain.AiRequestEvent;

/**
 * Derives the user-visible steps of an exchange ({@code AiRequest.activities})
 * from its stored transparency events, oldest first:
 *
 * <ul>
 * <li><b>Client tools</b> — each tool call the model requested (a
 * {@code TOOL_CALLS} event entry) becomes one activity, completed by the
 * matching {@code TOOL_RESULTS} entry (paired by {@code callId}). Elza
 * executed these itself, so the full arguments and results are available
 * (query, result size, touched objects as navigable links).</li>
 * <li><b>Provider-internal tools</b> — {@code tool_call}/{@code tool_result}
 * wire events (protocol 0.8+ task-event stream) of tools the provider ran
 * itself ({@code search_knowledge}, …). From protocol 0.9 each carries a
 * localized {@code label} and {@code summary} and {@code refs} to the objects
 * it touched (rendered as links). Wire events of a <i>delegated</i> tool
 * ({@code origin} = {@code client}) are skipped — Elza's own
 * {@code TOOL_CALLS}/{@code TOOL_RESULTS} records (paired by the same
 * {@code callId}) are strictly richer; an older provider without
 * {@code origin} is recognized by the wire tool name
 * ({@link AiToolRegistry#isClientTool}).</li>
 * <li><b>Preparation steps</b> — {@code preparation} wire events (e.g.
 * resolving rule-set dictionaries before the model turn).</li>
 * </ul>
 *
 * Best-effort: an unreadable event is skipped (logged), the raw event log
 * stays available for retrospection.
 */
@Component
public class AiActivityMapper {

    private static final Logger logger = LoggerFactory.getLogger(AiActivityMapper.class);

    /** Activity kinds (open set on the wire). */
    public static final String KIND_TOOL_CALL = "TOOL_CALL";
    public static final String KIND_PREPARATION = "PREPARATION";

    /** Activity states (open set on the wire; unknown = still running). */
    public static final String STATE_RUNNING = "RUNNING";
    public static final String STATE_DONE = "DONE";
    public static final String STATE_ERROR = "ERROR";

    /** Wire names of the standard tools (provider contract {@code StandardToolName}). */
    private static final String TOOL_SEARCH_NODES = "searchNodes";
    private static final String TOOL_GET_ITEM_TYPES = "getItemTypes";

    /** {@code TaskEvent.origin} value marking a tool the provider delegated to Elza. */
    private static final String ORIGIN_CLIENT = "client";

    /** Detail keys tried for the query summary of a provider-internal tool call. */
    private static final List<String> QUERY_KEYS = List.of("query", "fulltext", "q");

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AiToolRegistry toolRegistry;

    /**
     * Maps a request's ordered event log to its activities, oldest first. Only
     * tool and preparation events contribute; the other event types are covered
     * by the request itself (instructions, progress, blocks, error).
     *
     * <p>When {@code requestFinished} is true the exchange has reached a terminal
     * state, so any step still {@link #STATE_RUNNING} is settled as
     * {@link #STATE_DONE}: the task-event stream is advisory and best-effort — a
     * provider may never deliver a tool's {@code tool_result} event (client-tool
     * calls in particular are never completed on the stream), and the poll stops
     * at the terminal state — so a finished exchange must not render a step as a
     * perpetual spinner.
     */
    public List<AiRequestActivityVO> map(final List<AiRequestEvent> events, final boolean requestFinished) {
        Mapping mapping = new Mapping();
        for (AiRequestEvent event : events) {
            try {
                switch (event.getEventType()) {
                    case AiRequestEvent.TYPE_TOOL_CALLS
                        -> addCalls(event, mapping.byCallId);
                    case AiRequestEvent.TYPE_TOOL_RESULTS
                        -> applyResults(event, mapping.byCallId);
                    case AiRequestEvent.TYPE_PROVIDER_TOOL_CALL
                        -> addProviderCall(event, mapping);
                    case AiRequestEvent.TYPE_PROVIDER_TOOL_RESULT
                        -> applyProviderResult(event, mapping);
                    case AiRequestEvent.TYPE_PROVIDER_PREPARATION
                        -> addPreparation(event, mapping);
                    default -> { /* not a user-visible step */ }
                }
            } catch (Exception e) {
                logger.warn("Unreadable AI request event {} ({}) skipped: {}",
                        event.getAiRequestEventId(), event.getEventType(), e.getMessage());
            }
        }
        List<AiRequestActivityVO> activities = new ArrayList<>(mapping.byCallId.values());
        if (requestFinished) {
            for (AiRequestActivityVO activity : activities) {
                if (STATE_RUNNING.equals(activity.getState())) {
                    activity.setState(STATE_DONE);
                }
            }
        }
        return activities;
    }

    /** Working state of one mapping pass. */
    private static class Mapping {

        /** All activities in first-seen order, keyed by call id (or a synthetic key). */
        final Map<String, AiRequestActivityVO> byCallId = new LinkedHashMap<>();

        /** Provider-internal activities awaiting their result, per tool, oldest first. */
        final Map<String, List<AiRequestActivityVO>> pendingByTool = new LinkedHashMap<>();
    }

    /** One activity per tool call of a {@code TOOL_CALLS} event. */
    private void addCalls(final AiRequestEvent event,
                          final Map<String, AiRequestActivityVO> byCallId) throws Exception {
        for (JsonNode call : readArray(event.getData())) {
            String callId = text(call, "callId");
            if (callId == null) {
                continue;
            }
            String tool = text(call, "tool");
            byCallId.put(callId, new AiRequestActivityVO()
                    .id(callId)
                    .kind(KIND_TOOL_CALL)
                    .tool(tool)
                    .state(STATE_RUNNING)
                    .query(extractQuery(tool, call.path("arguments")))
                    .startDate(toOffset(event.getCreateDate())));
        }
    }

    /** Completes activities from a {@code TOOL_RESULTS} event (pairing by {@code callId}). */
    private void applyResults(final AiRequestEvent event,
                              final Map<String, AiRequestActivityVO> byCallId) throws Exception {
        for (JsonNode result : readArray(event.getData())) {
            String callId = text(result, "callId");
            AiRequestActivityVO activity = callId == null ? null : byCallId.get(callId);
            if (activity == null) {
                continue;
            }
            activity.setEndDate(toOffset(event.getCreateDate()));
            String error = text(result, "error");
            if (error != null) {
                activity.setState(STATE_ERROR);
                activity.setError(error);
                continue;
            }
            activity.setState(STATE_DONE);
            summarizeResult(activity, result.path("result"));
        }
    }

    /**
     * One activity per provider-internal {@code tool_call} wire event, carrying
     * its localized {@code label}/{@code summary} and {@code refs} links. A
     * <i>delegated</i> call ({@code origin} = {@code client}, or — for a provider
     * older than 0.9 with no {@code origin} — a wire name that is a client tool)
     * is skipped: Elza executed it itself and its own
     * {@code TOOL_CALLS}/{@code TOOL_RESULTS} records (the same {@code callId})
     * produce the richer activity.
     */
    private void addProviderCall(final AiRequestEvent event, final Mapping mapping) throws Exception {
        JsonNode wire = readObject(event.getData());
        String tool = text(wire, "tool");
        if (tool == null || isDelegated(text(wire, "origin"), tool)) {
            return;
        }
        String callId = text(wire, "callId");
        String key = callId != null ? callId : "e" + text(wire, "seq");
        AiRequestActivityVO activity = new AiRequestActivityVO()
                .id(key)
                .kind(KIND_TOOL_CALL)
                .tool(tool)
                .label(text(wire, "label"))
                .state(STATE_RUNNING)
                .query(extractProviderQuery(wire.path("detail")))
                .summary(text(wire, "summary"))
                .startDate(wireDate(wire, event));
        List<AiActivityLinkVO> links = refLinks(wire.path("refs"));
        if (!links.isEmpty()) {
            activity.setLinks(links);
        }
        mapping.byCallId.put(key, activity);
        mapping.pendingByTool.computeIfAbsent(tool, k -> new ArrayList<>()).add(activity);
    }

    /**
     * Completes a provider-internal activity from a {@code tool_result} wire
     * event: by {@code callId} when present, else the oldest still-running call
     * of the same tool (events arrive in emission order). Refreshes the localized
     * {@code summary} (a {@code tool_result} carries the result summary) and the
     * {@code refs} links. A delegated result — which does not reach Elza on this
     * stream — is ignored defensively.
     */
    private void applyProviderResult(final AiRequestEvent event, final Mapping mapping) throws Exception {
        JsonNode wire = readObject(event.getData());
        String tool = text(wire, "tool");
        if (tool != null && isDelegated(text(wire, "origin"), tool)) {
            return;
        }
        String callId = text(wire, "callId");
        AiRequestActivityVO activity = callId != null ? mapping.byCallId.get(callId) : null;
        if (activity == null && tool != null) {
            List<AiRequestActivityVO> pending = mapping.pendingByTool.getOrDefault(tool, List.of());
            activity = pending.stream()
                    .filter(a -> STATE_RUNNING.equals(a.getState()))
                    .findFirst().orElse(null);
        }
        if (activity == null) {
            return;
        }
        activity.setEndDate(wireDate(wire, event));
        String summary = text(wire, "summary");
        if (summary != null) {
            activity.setSummary(summary);
        }
        List<AiActivityLinkVO> links = refLinks(wire.path("refs"));
        if (!links.isEmpty()) {
            activity.setLinks(links);
        }
        applyOutcome(activity, wire.path("detail"));
    }

    /**
     * Sets the terminal state of a provider-internal activity from a
     * {@code tool_result}'s {@code detail.error}. The provider sends a boolean
     * flag ({@code false} = success, the common case; {@code true} = failure);
     * an older provider may send a string code/message. Only a {@code true} flag
     * or a non-blank string is a failure — a boolean {@code false} must never be
     * read as the error message {@code "false"} (which would flag every
     * successful step as an error).
     */
    private static void applyOutcome(final AiRequestActivityVO activity, final JsonNode detail) {
        JsonNode errorNode = detail.path("error");
        boolean failed;
        String message;
        if (errorNode.isBoolean()) {
            failed = errorNode.booleanValue();
            message = null;
        } else {
            message = text(detail, "error");
            failed = message != null;
        }
        if (failed) {
            activity.setState(STATE_ERROR);
            activity.setError(message);
        } else {
            activity.setState(STATE_DONE);
        }
    }

    /**
     * True when a tool wire event was <i>delegated</i> to Elza — {@code origin} is
     * {@code client}, or (a provider older than 0.9 sends no {@code origin}) the
     * wire tool name is one of Elza's client tools.
     */
    private boolean isDelegated(final String origin, final String tool) {
        if (ORIGIN_CLIENT.equals(origin)) {
            return true;
        }
        return origin == null && toolRegistry.isClientTool(tool);
    }

    /**
     * The {@code refs} of a tool event as navigable links: a {@code nodeId}
     * points at a description level, an {@code accessPointId} at an entity. No
     * per-object label is set (a reference carries no name — the client supplies
     * a generic one); an empty list when there are none.
     */
    private static List<AiActivityLinkVO> refLinks(final JsonNode refs) {
        List<AiActivityLinkVO> links = new ArrayList<>();
        if (!refs.isArray()) {
            return links;
        }
        for (JsonNode ref : refs) {
            JsonNode nodeId = ref.path("nodeId");
            JsonNode accessPointId = ref.path("accessPointId");
            if (nodeId.isNumber()) {
                links.add(new AiActivityLinkVO().target(
                        new AiContextNodeVO().nodeId(nodeId.asInt()).type(AiContextTypeVO.NODE)));
            } else if (accessPointId.isNumber()) {
                links.add(new AiActivityLinkVO().target(
                        new AiContextAccesspointVO().accessPointId(accessPointId.asInt())
                                .type(AiContextTypeVO.ACCESSPOINT)));
            }
        }
        return links;
    }

    /** A preparation step ({@code preparation} wire event) as one completed activity. */
    private void addPreparation(final AiRequestEvent event, final Mapping mapping) throws Exception {
        JsonNode wire = readObject(event.getData());
        String key = "e" + text(wire, "seq");
        OffsetDateTime at = wireDate(wire, event);
        mapping.byCallId.put(key, new AiRequestActivityVO()
                .id(key)
                .kind(KIND_PREPARATION)
                .state(STATE_DONE)
                .query(text(wire, "message"))
                .startDate(at)
                .endDate(at));
    }

    /**
     * Best-effort query summary of a provider-internal tool call. The wire
     * {@code detail} is curated free JSON whose exact shape is provider-owned;
     * common keys are tried (directly and under {@code arguments}), else null —
     * the activity then renders as a plain step.
     */
    private static String extractProviderQuery(final JsonNode detail) {
        if (detail == null || detail.isMissingNode() || detail.isNull()) {
            return null;
        }
        if (detail.isTextual()) {
            return StringUtils.trimToNull(detail.asText());
        }
        for (String queryKey : QUERY_KEYS) {
            String value = text(detail, queryKey);
            if (value != null) {
                return value;
            }
        }
        JsonNode arguments = detail.get("arguments");
        if (arguments != null && arguments.isObject()) {
            for (String queryKey : QUERY_KEYS) {
                String value = text(arguments, queryKey);
                if (value != null) {
                    return value;
                }
            }
        }
        return null;
    }

    /** The wire event's own timestamp; falls back to the local receive time. */
    private static OffsetDateTime wireDate(final JsonNode wire, final AiRequestEvent event) {
        String createdAt = text(wire, "createdAt");
        if (createdAt != null) {
            try {
                return OffsetDateTime.parse(createdAt);
            } catch (DateTimeParseException e) {
                // fall through to the receive time
            }
        }
        return toOffset(event.getCreateDate());
    }

    private JsonNode readObject(final String data) throws Exception {
        if (StringUtils.isBlank(data)) {
            return objectMapper.createObjectNode();
        }
        JsonNode root = objectMapper.readTree(data);
        return root.isObject() ? root : objectMapper.createObjectNode();
    }

    /** The data-like input summary shown next to the step's title. */
    private static String extractQuery(final String tool, final JsonNode arguments) {
        if (TOOL_SEARCH_NODES.equals(tool)) {
            return text(arguments, "fulltext");
        }
        if (TOOL_GET_ITEM_TYPES.equals(tool)) {
            return text(arguments, "ruleSetCode");
        }
        return null;
    }

    /** Render-ready result facts per tool: counts, truncation, links to the hits. */
    private static void summarizeResult(final AiRequestActivityVO activity, final JsonNode result) {
        if (TOOL_SEARCH_NODES.equals(activity.getTool())) {
            activity.setResultCount(result.path("totalCount").asLong(0));
            activity.setPartial(result.path("partial").asBoolean(false));
            List<AiActivityLinkVO> links = new ArrayList<>();
            for (JsonNode fund : result.path("funds")) {
                JsonNode fundId = fund.path("fundId");
                if (!fundId.isNumber()) {
                    continue;
                }
                for (JsonNode node : fund.path("nodes")) {
                    JsonNode nodeId = node.path("nodeId");
                    if (!nodeId.isNumber()) {
                        continue;
                    }
                    links.add(new AiActivityLinkVO()
                            .label(nodeLabel(node))
                            .target(new AiContextNodeVO().fundId(fundId.asInt()).nodeId(nodeId.asInt())
                                    .type(AiContextTypeVO.NODE)));
                }
            }
            if (!links.isEmpty()) {
                activity.setLinks(links);
            }
        } else if (TOOL_GET_ITEM_TYPES.equals(activity.getTool())) {
            JsonNode itemTypes = result.path("itemTypes");
            if (itemTypes.isArray()) {
                activity.setResultCount((long) itemTypes.size());
            }
        }
    }

    /** Link label of one search hit: its tree title, or the reference designation. */
    private static String nodeLabel(final JsonNode node) {
        String title = text(node, "title");
        if (title != null) {
            return title;
        }
        JsonNode marks = node.path("referenceMark");
        if (marks.isArray() && !marks.isEmpty()) {
            List<String> parts = new ArrayList<>();
            for (JsonNode mark : marks) {
                parts.add(mark.asText());
            }
            return String.join(" ", parts);
        }
        return null;
    }

    private JsonNode readArray(final String data) throws Exception {
        if (StringUtils.isBlank(data)) {
            return objectMapper.createArrayNode();
        }
        JsonNode root = objectMapper.readTree(data);
        return root.isArray() ? root : objectMapper.createArrayNode();
    }

    /** Trimmed text of an object field; null for a missing, null or blank value. */
    private static String text(final JsonNode node, final String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : StringUtils.trimToNull(value.asText());
    }

    private static OffsetDateTime toOffset(final Date date) {
        return date == null ? null
                : OffsetDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
    }
}
