package cz.tacr.elza.service.ai;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import cz.tacr.elza.controller.vo.AiRequestActivityVO;
import cz.tacr.elza.controller.vo.AiRequestVO;
import cz.tacr.elza.controller.vo.AiUsageVO;
import cz.tacr.elza.domain.AiRequest;
import cz.tacr.elza.domain.AiRequestEvent;
import cz.tacr.elza.repository.AiRequestEventRepository;

/**
 * Builds the render-ready {@link AiRequestVO} of an exchange from its stored
 * state: the request row, its event log (mapped to activities by
 * {@link AiActivityMapper}), the display blocks of a finished output and the
 * in-memory partial answer of a running one ({@link AiAnswerBuffer}). Shared
 * by the REST layer ({@link AiConversationService}) and the pollers, which
 * push the same VO over the WebSocket user queue — both channels always carry
 * an identical snapshot.
 */
@Component
public class AiRequestViewMapper {

    @Autowired
    private AiRequestEventRepository aiRequestEventRepository;

    @Autowired
    private AiActivityMapper activityMapper;

    @Autowired
    private AiBlockMapperRegistry blockMapperRegistry;

    @Autowired
    private AiAnswerBuffer answerBuffer;

    /** Maps a request and its ordered event log to the render-ready VO. */
    public AiRequestVO toVO(final AiRequest request, final List<AiRequestEvent> events) {
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
            vo.setPartialAnswer(answerBuffer.get(request.getAiRequestId()));
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

    /** The VO of a single request, loading its event log (call inside a transaction). */
    public AiRequestVO loadVO(final AiRequest request) {
        List<AiRequestEvent> events = aiRequestEventRepository
                .findByAiRequestIdInOrderByCreateDateAscAiRequestEventIdAsc(
                        List.of(request.getAiRequestId()));
        return toVO(request, events);
    }

    /** The WebSocket update message of a request (call inside a transaction). */
    public AiRequestUpdateMessage buildUpdateMessage(final AiRequest request) {
        return new AiRequestUpdateMessage(request.getAiConversationId(), loadVO(request));
    }

    /** Event logs of the given requests (one query), grouped by request id in stable order. */
    public Map<Integer, List<AiRequestEvent>> loadEventsByRequest(final List<AiRequest> requests) {
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

    /** True for the wire terminal states (an unknown state counts as in progress). */
    public static boolean isTerminal(final String state) {
        return "done".equals(state) || "error".equals(state) || "cancelled".equals(state);
    }

    private static OffsetDateTime toOffset(final Date date) {
        return date == null ? null
                : OffsetDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
    }
}
