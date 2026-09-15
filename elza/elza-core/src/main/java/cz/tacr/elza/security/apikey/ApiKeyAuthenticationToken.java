package cz.tacr.elza.security.apikey;

import java.util.Collections;

import org.springframework.security.authentication.AbstractAuthenticationToken;

/**
 * Unauthenticated carrier the filter hands to the provider: principal = key id, credentials =
 * secret. After the provider validates the credentials it returns a different, authenticated
 * token (the standard {@code UsernamePasswordAuthenticationToken} with a {@link
 * cz.tacr.elza.security.UserDetail} in its details).
 */
public class ApiKeyAuthenticationToken extends AbstractAuthenticationToken {

    private final String keyId;
    private final String secret;

    public ApiKeyAuthenticationToken(String keyId, String secret) {
        super(Collections.emptyList());
        this.keyId = keyId;
        this.secret = secret;
        setAuthenticated(false);
    }

    public String getKeyId() {
        return keyId;
    }

    @Override
    public Object getCredentials() {
        return secret;
    }

    @Override
    public Object getPrincipal() {
        return keyId;
    }
}
