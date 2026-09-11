package cz.tacr.elza.security;

/**
 * How the current request's security context was established.
 *
 * Recorded on {@link UserDetail} so operations that must not be reachable through a machine
 * credential (password change, API-key management) can refuse a request that was authenticated
 * with an API key.
 */
public enum AuthenticationMethod {
    /** Interactive login (form, HTTP Basic, SSO, OAuth2/JWT, Kerberos). */
    SESSION,
    /** Stateless request authenticated by {@code X-API-Key}. */
    API_KEY,
}
