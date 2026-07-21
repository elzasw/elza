package cz.tacr.elza.service.ai;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

/**
 * In-memory store of the streamed partial answer of running AI requests, fed
 * by {@code answer_delta} task events. Deliberately not persisted: the text is
 * provisional by contract (the durable answer is {@code ai_request.output}),
 * so an application restart simply loses the buffer and the panel shows phases
 * until the turn completes.
 */
@Component
public class AiAnswerBuffer {

    /** Accumulated text per {@code aiRequestId}; values are immutable snapshots. */
    private final Map<Integer, String> buffers = new ConcurrentHashMap<>();

    /** Appends a streamed text fragment to the request's partial answer. */
    public void append(final Integer aiRequestId, final String text) {
        if (text == null || text.isEmpty()) {
            return;
        }
        buffers.merge(aiRequestId, text, String::concat);
    }

    /** The accumulated partial answer, or null when nothing was streamed. */
    public String get(final Integer aiRequestId) {
        return buffers.get(aiRequestId);
    }

    /** Drops the request's buffer (called when the request reaches a terminal state). */
    public void clear(final Integer aiRequestId) {
        buffers.remove(aiRequestId);
    }
}
