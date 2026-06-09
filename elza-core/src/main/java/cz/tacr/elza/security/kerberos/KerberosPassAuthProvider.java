package cz.tacr.elza.security.kerberos;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.kerberos.authentication.JaasSubjectHolder;
import org.springframework.security.kerberos.authentication.KerberosClient;
import org.springframework.security.kerberos.authentication.KerberosUsernamePasswordAuthenticationToken;
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import cz.tacr.elza.domain.UsrUser;
import cz.tacr.elza.security.DeferredAuthFailureLog;
import cz.tacr.elza.security.SiemAuditLogger;
import cz.tacr.elza.security.SiemAuditLogger.AuthenticationType;
import cz.tacr.elza.service.UserService;

/**
 * Karberos authentication provider
 * 
 * This provider is based on Spring Security Kerberos authentication provider (KerberosServiceAuthenticationProvider)
 * 
 * Main difference is in returned object with UserDetail.
 */
public class KerberosPassAuthProvider implements AuthenticationProvider {
	
	private static final Logger LOG = LoggerFactory.getLogger(KerberosPassAuthProvider.class);

	private final UserService userService;
	private final PlatformTransactionManager txManager;
	private final SiemAuditLogger siemAuditLogger;
	private final KerberosClient kerberosClient;
	
	public KerberosPassAuthProvider(final KerberosClient kerberosClient,
			final UserService userService, 
			final PlatformTransactionManager txManager,
			final SiemAuditLogger siemAuditLogger) {
		this.kerberosClient = kerberosClient;
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

		LOG.debug("Try to login using Kerberos and password");
	    UsernamePasswordAuthenticationToken auth = (UsernamePasswordAuthenticationToken) authentication;
	    JaasSubjectHolder subjectHolder = null;
		try {
			subjectHolder = kerberosClient.login(auth.getName(), auth.getCredentials().toString());
			LOG.debug("Successfully validated " + auth.getName());
		} catch (Exception e) {
			DeferredAuthFailureLog.defer(LOG, "Failed to validate Kerberos Token, name: " + auth.getName(), e);
			// try another method
			return null;
		}
		
		var username = subjectHolder.getUsername();
		try {
			var ret = prepareDetails(authentication, subjectHolder);

			siemAuditLogger.loginSuccess(username, sourceIp, AuthenticationType.KERBEROS);
			return ret;
		} catch (UsernameNotFoundException e) {
			siemAuditLogger.loginFailed(username, sourceIp, "INVALID_USERNAME");
			throw e;
		}
	}

	private KerberosUsernamePasswordAuthenticationToken prepareDetails(Authentication authentication, 
			JaasSubjectHolder subjectHolder
			) {
		var username = subjectHolder.getUsername();

		var usernameFirstPart = username.split("@")[0];
		var ret = new TransactionTemplate(txManager).execute(r -> {
			UsrUser user = userService.findByUsername(usernameFirstPart);
			if (user == null) {
				throw new UsernameNotFoundException("Neplatné uživatelské jméno: " + usernameFirstPart);
			}

			KerberosUsernamePasswordAuthenticationToken responseAuth = new KerberosUsernamePasswordAuthenticationToken(
					user.getUsername(), StringUtils.EMPTY, null, subjectHolder);
			var userDetail = userService.createUserDetail(user);
			responseAuth.setDetails(userDetail);
			return responseAuth;
		});
		return ret;
	}

	@Override
	public boolean supports(Class<? extends Object> authentication) {
		return (UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication));
	}

}
