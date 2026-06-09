package cz.tacr.elza.security;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;

/**
 * Wraps the authentication provider chain and brackets each attempt with
 * {@link DeferredAuthFailureLog}. Individual provider failures are then logged
 * according to the overall outcome instead of being logged at ERROR by every
 * provider as soon as it fails.
 *
 * @see DeferredAuthFailureLog
 */
public class DeferredFailureAuthenticationManager implements AuthenticationManager {

    private final AuthenticationManager delegate;

    public DeferredFailureAuthenticationManager(final AuthenticationManager delegate) {
        this.delegate = delegate;
    }

    @Override
    public Authentication authenticate(final Authentication authentication) throws AuthenticationException {
        DeferredAuthFailureLog.start();
        try {
            Authentication result = delegate.authenticate(authentication);
            DeferredAuthFailureLog.flushAsSuccess();
            return result;
        } catch (AuthenticationException e) {
            DeferredAuthFailureLog.flushAsFailure();
            throw e;
        } finally {
            DeferredAuthFailureLog.clear();
        }
    }
}
