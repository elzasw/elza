package cz.tacr.elza.service.ai;

import java.time.OffsetDateTime;
import java.time.ZoneId;
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
import cz.tacr.elza.controller.vo.AiContextNodeVO;
import cz.tacr.elza.controller.vo.AiContextTypeVO;
import cz.tacr.elza.controller.vo.AiRequestActivityVO;
import cz.tacr.elza.domain.AiRequestEvent;

/**
 * Derives the user-visible steps of an exchange ({@code AiRequest.activities})
 * from its stored transparency events: each tool call the model requested
 * (a {@code TOOL_CALLS} event entry) becomes one activity, completed by the
 * matching {@code TOOL_RESULTS} entry (paired by {@code callId}). The events
 * keep the full payloads; an activity carries only a render-ready summary —
 * the query, the result size and the touched objects as navigable links.
 * Best-effort: an unreadable event is skipped (logged), the raw event log
 * stays available for retrospection.
 */
@Component
public class AiActivityMapper {

    private static final Logger logger = LoggerFactory.getLogger(AiActivityMapper.class);

    /** Activity kinds (open set on the wire). */
    public static final String KIND_TOOL_CALL = "TOOL_CALL";

    /** Activity states (open set on the wire; unknown = still running). */
    public static final String STATE_RUNNING = "RUNNING";
    public static final String STATE_DONE = "DONE";
    public static final String STATE_ERROR = "ERROR";

    /** Wire names of the standard tools (provider contract {@code StandardToolName}). */
    private static final String TOOL_SEARCH_NODES = "searchNodes";
    private static final String TOOL_GET_ITEM_TYPES = "getItemTypes";

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * Maps a request's ordered event log to its activities, oldest first. Only
     * tool events contribute; the other event types are covered by the request
     * itself (instructions, blocks, error).
     */
    public List<AiRequestActivityVO> map(final List<AiRequestEvent> events) {
        Map<String, AiRequestActivityVO> byCallId = new LinkedHashMap<>();
        for (AiRequestEvent event : events) {
            try {
                if (AiRequestEvent.TYPE_TOOL_CALLS.equals(event.getEventType())) {
                    addCalls(event, byCallId);
                } else if (AiRequestEvent.TYPE_TOOL_RESULTS.equals(event.getEventType())) {
                    applyResults(event, byCallId);
                }
            } catch (Exception e) {
                logger.warn("Unreadable AI request event {} ({}) skipped: {}",
                        event.getAiRequestEventId(), event.getEventType(), e.getMessage());
            }
        }
        return new ArrayList<>(byCallId.values());
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
                            .target(new AiContextNodeVO(fundId.asInt(), nodeId.asInt(),
                                    AiContextTypeVO.NODE)));
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
