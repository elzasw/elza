package cz.tacr.elza.service.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

/**
 * Pushes {@link AiRequestUpdateMessage} snapshots to the conversation owner
 * over the STOMP user destination — only that user's sessions (all their tabs)
 * receive it, unlike the broadcast {@code /topic/api/changes}, so the payload
 * may carry conversation content.
 *
 * <p>Delivery is best effort by design: the simple broker neither persists nor
 * replays, and the client disconnects hidden tabs. Snapshots make that safe —
 * the next push overwrites, and the client refetches the conversation on
 * reconnect. A send failure therefore only logs; it must never break a poller.
 */
@Service
public class AiRequestPushService {

    private static final Logger logger = LoggerFactory.getLogger(AiRequestPushService.class);

    /** User-queue destination; the client subscribes to {@code /user/queue/ai-request}. */
    public static final String AI_REQUEST_DESTINATION = "/queue/ai-request";

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    /**
     * Sends the request snapshot to all sessions of the given user.
     *
     * @param username login of the conversation owner (the STOMP principal name)
     */
    public void push(final String username, final AiRequestUpdateMessage message) {
        if (username == null || message == null) {
            return;
        }
        try {
            messagingTemplate.convertAndSendToUser(username, AI_REQUEST_DESTINATION, message);
        } catch (Exception e) {
            logger.warn("Push of AI request {} update to user {} failed: {}",
                    message.getRequest() != null ? message.getRequest().getId() : null,
                    username, e.getMessage());
            logger.debug("AI request push failure detail", e);
        }
    }
}
