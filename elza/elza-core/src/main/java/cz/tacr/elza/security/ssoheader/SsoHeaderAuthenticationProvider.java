package cz.tacr.elza.security.ssoheader;

import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import cz.tacr.elza.domain.UsrUser;
import cz.tacr.elza.security.SiemAuditLogger;
import cz.tacr.elza.security.SiemAuditLogger.AuthenticationType;
import cz.tacr.elza.service.UserService;

public class SsoHeaderAuthenticationProvider implements AuthenticationProvider {

	private final UserService userService;
	private PlatformTransactionManager txManager;
	private SiemAuditLogger siemAuditLogger;

	public SsoHeaderAuthenticationProvider(UserService userService, final PlatformTransactionManager txManager,
			final SiemAuditLogger siemAuditLogger) {
		this.userService = userService;
		this.txManager = txManager;
		this.siemAuditLogger = siemAuditLogger;
	}

	@Override
	public Authentication authenticate(Authentication authentication) throws AuthenticationException {
		String username = authentication.getName();

		String sourceIp = null;
	    if (authentication.getDetails() instanceof WebAuthenticationDetails) {
	    	var details = (WebAuthenticationDetails)authentication.getDetails();
	        sourceIp = details.getRemoteAddress();
	    }
		
		// Why credentials is empty?
		if (!Objects.equals(authentication.getCredentials(), SsoHeaderAuthenticationFilter.SSO_CREDENTIALS)) {
			siemAuditLogger.loginFailed(username, sourceIp, "INVALID_CREDENTIALS");
			throw new BadCredentialsException("Neplatné credentials, username: "+username);
		}

		try {
			var ret = new TransactionTemplate(txManager).execute(r -> {
				UsrUser user = userService.findByUsername(username);
				if (user == null) {
					throw new UsernameNotFoundException("Neplatné uživatelské jméno: " + username);
				}

				return userService.createAuthentication(user);
			});

			siemAuditLogger.loginSuccess(username, sourceIp, AuthenticationType.SSO_HEADER);
			return ret;
		} catch (UsernameNotFoundException e) {
			siemAuditLogger.loginFailed(username, sourceIp, "INVALID_USERNAME");
			throw e;
		}
	}

	@Override
	public boolean supports(Class<?> authentication) {
		return PreAuthenticatedAuthenticationToken.class.isAssignableFrom(authentication);
	}
}
