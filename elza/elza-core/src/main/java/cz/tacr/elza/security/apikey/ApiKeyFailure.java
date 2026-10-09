package cz.tacr.elza.security.apikey;

import cz.tacr.elza.core.data.CoreMessage;

/**
 * Reason an API-key authentication attempt was refused. Kept as an enum so the entry point can
 * write a stable {@code code} the client can react to, distinct from the human-readable message
 * (a message of the core, rendered in the language of the request).
 */
public enum ApiKeyFailure {
    MALFORMED_TOKEN(CoreMessage.API_KEY_MALFORMED_TOKEN),
    UNKNOWN_KEY(CoreMessage.API_KEY_UNKNOWN_KEY),
    INVALID_SECRET(CoreMessage.API_KEY_INVALID_SECRET),
    EXPIRED(CoreMessage.API_KEY_EXPIRED),
    REVOKED(CoreMessage.API_KEY_REVOKED),
    USER_INACTIVE(CoreMessage.API_KEY_USER_INACTIVE);

    private final CoreMessage message;

    ApiKeyFailure(CoreMessage message) {
        this.message = message;
    }

    /**
     * @return the message shown to the person running the integration
     */
    public CoreMessage getCoreMessage() {
        return message;
    }

    /**
     * @return the text of the message as written in the code (for logs; the response renders it
     *         in the language of the request)
     */
    public String getMessage() {
        return message.text();
    }
}
