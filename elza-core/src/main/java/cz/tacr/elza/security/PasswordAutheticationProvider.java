package cz.tacr.elza.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetails;

import cz.tacr.elza.domain.UsrAuthentication;
import cz.tacr.elza.domain.UsrUser;
import cz.tacr.elza.security.SiemAuditLogger.AuthenticationType;
import cz.tacr.elza.service.UserService;

public class PasswordAutheticationProvider implements AuthenticationProvider {

	private static final Logger log = LoggerFactory.getLogger(PasswordAutheticationProvider.class);

	private final UserService userService;
	private final SiemAuditLogger siemAuditLogger;

	public PasswordAutheticationProvider(UserService userService, SiemAuditLogger siemAuditLogger) {
		this.userService = userService;
		this.siemAuditLogger = siemAuditLogger;
	}

	@Override
	public Authentication authenticate(final Authentication authentication) throws AuthenticationException {
		String username = authentication.getName();
		String password = authentication.getCredentials().toString();
		String sourceIp = null;
	    if (authentication.getDetails() instanceof WebAuthenticationDetails) {
	    	var details = (WebAuthenticationDetails)authentication.getDetails();
	        sourceIp = details.getRemoteAddress();
	    }		
		String encodePassword = userService.encodePassword(password);

		UsrUser user = userService.findByUsername(username);
		if (user != null) {
			UsrAuthentication usrAuthentication = userService.findAuthentication(user,
					UsrAuthentication.AuthType.PASSWORD);
			if (usrAuthentication == null) {
				siemAuditLogger.loginFailed(username, sourceIp, "INVALID_AUTH_TYPE");
				throw new UsernameNotFoundException("Pro uživatele není povolen tento typ přihlášení");
			}

			encodePassword = usrAuthentication.getAuthValue();
			if (!userService.matchesPassword(password, encodePassword, username)) {
				siemAuditLogger.loginFailed(username, sourceIp, "INVALID_PASSWORD");
				throw new UsernameNotFoundException("Neplatné uživatelské jméno nebo heslo");
			}

			if (!user.getActive()) {
				siemAuditLogger.loginFailed(username, sourceIp, "INACTIVE_USER");
				throw new LockedException("User is not active");
			}
		} else {
			siemAuditLogger.loginFailed(username, sourceIp, "INVALID_USERNAME");
			throw new UsernameNotFoundException("Neplatné uživatelské jméno nebo heslo");
		}
		
		siemAuditLogger.loginSuccess(username, sourceIp, AuthenticationType.PASSWORD);

		UserDetail userDetail = userService.createUserDetail(user);

		UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(username, encodePassword,
				null);
		auth.setDetails(userDetail);
		return auth;
	}

	@Override
	public boolean supports(final Class<?> authentication) {
		return authentication.equals(UsernamePasswordAuthenticationToken.class);
	}
}
