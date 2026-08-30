package cz.tacr.elza.websocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

/**
 * Pushes a message to a single user's WebSocket topic
 * {@code /topic/user/{userId}} — the per-user counterpart of the shared
 * {@code /topic/api/changes} broadcast. The user's client subscribes to its own
 * topic and {@link UserTopicSubscriptionInterceptor} refuses a subscription to
 * anyone else's, so the payload may carry that user's private content. Each
 * message is expected to carry an {@code eventType} discriminator, so the client
 * routes it the same way it routes broadcast events.
 *
 * <p>A plain topic is used deliberately rather than a Spring user destination
 * ({@code /user/**}): in this application's customized STOMP channel stack the
 * translated user-destination subscription is never registered with the simple
 * broker, so those messages are silently dropped; plain topic subscriptions
 * register and deliver reliably.
 *
 * <p>Delivery is best effort — the simple broker neither persists nor replays,
 * and a client disconnects hidden tabs; a send failure only logs. Senders that
 * need the client to recover a missed message must make it re-fetch on
 * reconnect (as the AI panel does).
 */
@Service
public class UserEventPushService {

    private static final Logger logger = LoggerFactory.getLogger(UserEventPushService.class);

    /** Per-user topic prefix; the user's client subscribes to {@code PREFIX + userId}. */
    public static final String USER_TOPIC_PREFIX = "/topic/user/";

    /** Shared segment used by system users without a persisted id (bootstrap admin). */
    public static final String ADMIN_TOPIC_ID = "admin";

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    /**
     * Sends a message to the given user's topic. The payload should carry an
     * {@code eventType} field so the client can dispatch it.
     *
     * @param userId  recipient user id
     * @param message payload (serialized to JSON)
     */
    public void push(final Integer userId, final Object message) {
        if (userId == null || message == null) {
            return;
        }
        push(userId.toString(), message);
    }

    /**
     * Sends a message to a named user topic segment. Use this for system users
     * without a numeric id (see {@link #ADMIN_TOPIC_ID}).
     */
    public void push(final String userTopic, final Object message) {
        if (userTopic == null || message == null) {
            return;
        }
        try {
            messagingTemplate.convertAndSend(USER_TOPIC_PREFIX + userTopic, message);
        } catch (Exception e) {
            logger.warn("Push to user {} topic failed: {}", userTopic, e.getMessage());
            logger.debug("User topic push failure detail", e);
        }
    }
}
