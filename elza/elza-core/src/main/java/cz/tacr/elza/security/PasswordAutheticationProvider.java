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
			var ret = new TransactionTemplate(txManager).execute(r -> {
				UsrUser user = userService.findByUsername(username);
				if(user==null) {
					throw new UsernameNotFoundException("Neplatné uživatelské jméno: " + username);
				}
				
				UsrAuthentication usrAuthentication = userService.findAuthentication(user,
						UsrAuthentication.AuthType.PASSWORD);
				if (usrAuthentication == null) {
					throw new UsernameNotFoundException("Pro uživatele není povolen tento typ přihlášení.");
				}

				var encodedPassword = usrAuthentication.getAuthValue();
				if (!userService.matchesPassword(password, encodedPassword, username)) {
					throw new UsernameNotFoundException("Neplatné uživatelské jméno nebo heslo");
				}

				userService.upgradePasswordEncodingIfNeeded(usrAuthentication, password);

				return userService.createAuthentication(user);
			});
			
			siemAuditLogger.loginSuccess(username, sourceIp, AuthenticationType.PASSWORD);
			
			return ret;
		}
        catch (UsernameNotFoundException e) {
        	siemAuditLogger.loginFailed(username, sourceIp, "INVALID_USERNAME");
			throw e;
		}
		catch(LockedException le) {
			siemAuditLogger.loginFailed(username, sourceIp, "INACTIVE_USER");
			throw le;
		}		
	}

	@Override
	public boolean supports(final Class<?> authentication) {
		return authentication.equals(UsernamePasswordAuthenticationToken.class);
	}
}
