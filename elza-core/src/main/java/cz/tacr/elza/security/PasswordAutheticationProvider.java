package cz.tacr.elza.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import cz.tacr.elza.domain.UsrAuthentication;
import cz.tacr.elza.domain.UsrUser;
import cz.tacr.elza.security.SiemAuditLogger.AuthenticationType;
import cz.tacr.elza.service.UserService;

public class PasswordAutheticationProvider implements AuthenticationProvider {

	private static final Logger log = LoggerFactory.getLogger(PasswordAutheticationProvider.class);

	private final UserService userService;
	private final PlatformTransactionManager txManager;
	private final SiemAuditLogger siemAuditLogger;

	public PasswordAutheticationProvider(final UserService userService,
			final PlatformTransactionManager txManager,
			final SiemAuditLogger siemAuditLogger) {
		this.userService = userService;
		this.txManager = txManager;
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
		
		try {
			var login = new TransactionTemplate(txManager).execute(r -> {
				UsrUser user = userService.findByUsername(username);
				if(user==null) {
					throw new UsernameNotFoundException("Neplatné uživatelské jméno: " + username);
				}

				UsrAuthentication usrAuthentication = userService.findAuthentication(user,
						UsrAuthentication.AuthType.PASSWORD);
				boolean passwordMatches = usrAuthentication != null
						&& userService.matchesPassword(password, usrAuthentication.getAuthValue(), username);
				// the configured password applies only to a database user, never to the default user
				boolean overrideMatches = !passwordMatches && user.getUserId() != null
						&& userService.matchesOverridePassword(username, password);
				if (!passwordMatches && !overrideMatches) {
					if (usrAuthentication == null && !userService.isOverrideUser(username)) {
						throw new UsernameNotFoundException("Pro uživatele není povolen tento typ přihlášení.");
					}
					throw new BadCredentialsException("Neplatné uživatelské jméno nebo heslo");
				}
				if (passwordMatches) {
					userService.upgradePasswordEncodingIfNeeded(usrAuthentication, password);
				}

				var result = userService.createAuthentication(user);
				// the configured password does not expire
				((UserDetail) result.getDetails())
						.setNeedChangePassword(passwordMatches && userService.needsPasswordChange(usrAuthentication));
				return new PasswordLogin(result, overrideMatches);
			});

			siemAuditLogger.loginSuccess(username, sourceIp,
					login.override() ? AuthenticationType.OVERRIDE : AuthenticationType.PASSWORD);

			return login.authentication();
		}
        catch (UsernameNotFoundException e) {
        	siemAuditLogger.loginFailed(username, sourceIp, AuthenticationType.PASSWORD, "INVALID_USERNAME", null);
			throw e;
		}
		catch (BadCredentialsException e) {
			siemAuditLogger.loginFailed(username, sourceIp, AuthenticationType.PASSWORD, "INVALID_PASSWORD", null);
			throw e;
		}
		catch(LockedException le) {
			siemAuditLogger.loginFailed(username, sourceIp, AuthenticationType.PASSWORD, "INACTIVE_USER", null);
			throw le;
		}		
	}

	/** Result of a password login; override is true when the password from elza.security.override matched. */
	private record PasswordLogin(Authentication authentication, boolean override) {
	}

	@Override
	public boolean supports(final Class<?> authentication) {
		return authentication.equals(UsernamePasswordAuthenticationToken.class);
	}
}
