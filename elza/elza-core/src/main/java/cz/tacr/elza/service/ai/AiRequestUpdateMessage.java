package cz.tacr.elza.service.ai;

import cz.tacr.elza.controller.vo.AiRequestVO;

/**
 * WebSocket message pushed to the conversation owner's user queue
 * ({@link AiRequestPushService#AI_REQUEST_DESTINATION}) whenever an AI request
 * changes. Carries the complete render-ready request snapshot, so the client
 * simply replaces the request in its conversation state — messages are
 * idempotent and a lost one is corrected by the next (the client refetches the
 * conversation on reconnect).
 *
 * <p>The {@code eventType} discriminator follows the shape of the broadcast
 * API-change messages, letting the client route it through the same handler.
 */
public class AiRequestUpdateMessage {

    public static final String EVENT_TYPE = "AI_REQUEST_UPDATE";

    private final Integer conversationId;

    private final AiRequestVO request;

    public AiRequestUpdateMessage(final Integer conversationId, final AiRequestVO request) {
        this.conversationId = conversationId;
        this.request = request;
    }

    public String getEventType() {
        return EVENT_TYPE;
    }

    public Integer getConversationId() {
        return conversationId;
    }

    public AiRequestVO getRequest() {
        return request;
    }
}
