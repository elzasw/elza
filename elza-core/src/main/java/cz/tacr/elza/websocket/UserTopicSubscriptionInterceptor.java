package cz.tacr.elza.websocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import cz.tacr.elza.security.UserDetail;

/**
 * Guards the per-user topics ({@link UserEventPushService#USER_TOPIC_PREFIX}):
 * a client may subscribe only to its own {@code /topic/user/{userId}}, so one
 * user's messages never reach another. Any other destination passes through
 * untouched — this interceptor only ever rejects a mismatched per-user
 * subscription, so it cannot affect the rest of the messaging.
 */
@Component
public class UserTopicSubscriptionInterceptor implements ChannelInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(UserTopicSubscriptionInterceptor.class);

    @Override
    public Message<?> preSend(final Message<?> message, final MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
        if (!StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            return message;
        }
        String destination = accessor.getDestination();
        if (destination == null || !destination.startsWith(UserEventPushService.USER_TOPIC_PREFIX)) {
            return message;
        }
        // The user id is the first path segment after the prefix (a further
        // "/sub-topic" is allowed but does not change ownership).
        String tail = destination.substring(UserEventPushService.USER_TOPIC_PREFIX.length());
        String requested = tail.split("/", 2)[0];
        Integer userId = authenticatedUserId(accessor);
        if (userId != null && userId.toString().equals(requested)) {
            return message;
        }
        logger.warn("Rejected per-user topic subscription: user {} may not subscribe to {}", userId, destination);
        return null; // drop the SUBSCRIBE — the client is not subscribed to this topic
    }

    /** Id of the authenticated user of the STOMP session, or null when unresolved. */
    private static Integer authenticatedUserId(final StompHeaderAccessor accessor) {
        if (accessor.getUser() instanceof Authentication authentication
                && authentication.getDetails() instanceof UserDetail userDetail) {
            return userDetail.getId();
        }
        return null;
    }
}
