package cz.tacr.elza.security.apikey;

import org.springframework.security.core.AuthenticationException;

/**
 * Wraps a {@link ApiKeyFailure} so it survives Spring Security's exception plumbing and can be
 * turned into a JSON 401 by {@link ApiKeyAuthenticationEntryPoint}. Carries the key id when it is
 * known, so audit logs can name the key without exposing its secret.
 */
public class ApiKeyAuthenticationException extends AuthenticationException {

    private final ApiKeyFailure failure;
    private final String keyId;

    public ApiKeyAuthenticationException(ApiKeyFailure failure, String keyId) {
        super(failure.getMessage());
        this.failure = failure;
        this.keyId = keyId;
    }

    public ApiKeyFailure getFailure() {
        return failure;
    }

    public String getKeyId() {
        return keyId;
    }
}
