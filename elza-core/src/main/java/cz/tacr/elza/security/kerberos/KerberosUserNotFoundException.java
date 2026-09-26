package cz.tacr.elza.security.kerberos;

import org.springframework.security.core.userdetails.UsernameNotFoundException;

/**
 * Kerberos ticket was valid, but the authenticated principal has no ELZA user.
 *
 * Carries the username (principal without realm) so the SSO failure handler
 * can tell the user which account is missing.
 */
public class KerberosUserNotFoundException extends UsernameNotFoundException {

	private static final long serialVersionUID = 1L;

	private final String username;

	public KerberosUserNotFoundException(final String username) {
		super("Neplatné uživatelské jméno: " + username);
		this.username = username;
	}

	public String getUsername() {
		return username;
	}
}
