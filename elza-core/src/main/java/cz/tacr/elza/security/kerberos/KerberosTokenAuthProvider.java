package cz.tacr.elza.security.kerberos;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.kerberos.authentication.KerberosServiceRequestToken;
import org.springframework.security.kerberos.authentication.KerberosTicketValidation;
import org.springframework.security.kerberos.authentication.KerberosTicketValidator;
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import cz.tacr.elza.domain.UsrUser;
import cz.tacr.elza.security.DeferredAuthFailureLog;
import cz.tacr.elza.security.SiemAuditLogger;
import cz.tacr.elza.security.SiemAuditLogger.AuthenticationType;
import cz.tacr.elza.service.UserService;

/**
 * Kerberos authentication provider based on token
 */
public class KerberosTokenAuthProvider implements AuthenticationProvider {
	private static final Logger LOG = LoggerFactory.getLogger(KerberosTokenAuthProvider.class);

	private final UserService userService;
	private final PlatformTransactionManager txManager;
	private final SiemAuditLogger siemAuditLogger;
	private final KerberosTicketValidator ticketValidator;
	
	public KerberosTokenAuthProvider(final KerberosTicketValidator ticketValidator,
			final UserService userService, 
			final PlatformTransactionManager txManager,
			final SiemAuditLogger siemAuditLogger) {
		this.ticketValidator = ticketValidator;
		this.userService = userService;
		this.txManager = txManager;
		this.siemAuditLogger = siemAuditLogger;
	}

	@Override
	public Authentication authenticate(Authentication authentication) throws AuthenticationException {
		String sourceIp = null;
	    if (authentication.getDetails() instanceof WebAuthenticationDetails) {
	    	var details = (WebAuthenticationDetails)authentication.getDetails();
	        sourceIp = details.getRemoteAddress();
	    }

	    KerberosServiceRequestToken auth = (KerberosServiceRequestToken) authentication;
		byte[] token = auth.getToken();
		LOG.debug("Try to validate Kerberos Token");
		KerberosTicketValidation ticketValidation = null;
		try {
			ticketValidation = this.ticketValidator.validateTicket(token);
			LOG.debug("Successfully validated " + ticketValidation.username());
		} catch (Exception e) {
			DeferredAuthFailureLog.defer(LOG, "Failed to validate Kerberos Token", e);
			// try another method
			return null;
		}
		
		var username = ticketValidation.username();
		try {
			var ret = prepareDetails(authentication, ticketValidation, token);

			siemAuditLogger.loginSuccess(username, sourceIp, AuthenticationType.KERBEROS);
			return ret;
		} catch (UsernameNotFoundException e) {
			siemAuditLogger.loginFailed(username, sourceIp, "INVALID_USERNAME");
			throw e;
		}
	}

	private KerberosServiceRequestToken prepareDetails(Authentication authentication, KerberosTicketValidation ticketValidation,
			byte[] token) {
		var username = ticketValidation.username();

		var usernameFirstPart = username.split("@")[0];
		var ret = new TransactionTemplate(txManager).execute(r -> {
			UsrUser user = userService.findByUsername(usernameFirstPart);
			if (user == null) {
				throw new UsernameNotFoundException("Neplatné uživatelské jméno: " + usernameFirstPart);
			}

			KerberosServiceRequestToken responseAuth = new KerberosServiceRequestToken(
					user.getUsername(), ticketValidation,
					null, token);
			var userDetail = userService.createUserDetail(user);
			responseAuth.setDetails(userDetail);
			return responseAuth;
		});
		return ret;
	}

	@Override
	public boolean supports(Class<? extends Object> auth) {
		return KerberosServiceRequestToken.class.isAssignableFrom(auth);
	}

}
