package cz.tacr.elza.security.kerberos;

import java.io.Serializable;

import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;

/**
 * Reason of a failed Windows (Kerberos) sign-in, kept in the HTTP session
 * and displayed once in the login dialog.
 *
 * @param code     USER_NOT_FOUND, USER_INACTIVE or FAILED
 * @param username ELZA username, set only for USER_NOT_FOUND
 */
public record KerberosSsoError(String code, String username) implements Serializable {

	public static final String USER_NOT_FOUND = "USER_NOT_FOUND";
	public static final String USER_INACTIVE = "USER_INACTIVE";
	public static final String FAILED = "FAILED";

	public static KerberosSsoError from(final AuthenticationException exception) {
		if (exception instanceof KerberosUserNotFoundException notFound) {
			return new KerberosSsoError(USER_NOT_FOUND, notFound.getUsername());
		}
		if (exception instanceof LockedException) {
			return new KerberosSsoError(USER_INACTIVE, null);
		}
		return new KerberosSsoError(FAILED, null);
	}
}
