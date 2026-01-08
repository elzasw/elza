package cz.tacr.elza.security.ldap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.ldap.authentication.ad.ActiveDirectoryLdapAuthenticationProvider;
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import cz.tacr.elza.domain.UsrUser;
import cz.tacr.elza.security.SiemAuditLogger;
import cz.tacr.elza.security.SiemAuditLogger.AuthenticationType;
import cz.tacr.elza.service.UserService;

public class ActiveDirectoryUserDetailProvider implements AuthenticationProvider  {
	
	private static Logger logger = LoggerFactory.getLogger(ActiveDirectoryUserDetailProvider.class);
	private final PlatformTransactionManager txManager;
	private final UserService userService;
	private final ActiveDirectoryLdapAuthenticationProvider adProvider;
	private SiemAuditLogger siemAuditLogger;
	
	public ActiveDirectoryUserDetailProvider(
			final LdapProperties ldapProperties,
			final PlatformTransactionManager txManager,
            final UserService userService,
            final SiemAuditLogger siemAuditLogger) {
		// adding active directory domain
		this.adProvider = new ActiveDirectoryLdapAuthenticationProvider(ldapProperties.getAdDomain(), ldapProperties.getAdServer());
		// better exceptions
		adProvider.setConvertSubErrorCodesToExceptions(true);
		
		this.txManager = txManager;
		this.userService = userService;		
		this.siemAuditLogger = siemAuditLogger;
	} 

	/*
	@Override
	public UserDetails mapUserFromContext(DirContextOperations ctx, String username,
			Collection<? extends GrantedAuthority> authorities) {
		logger.debug("mapUserFromContext: {}", username);
		
        return new TransactionTemplate(txManager).execute(r -> {
        	UsrUser user = userService.findByUsername(sub);
            // prepare temporary credentials
            SecurityContext prevSecCtx = SecurityContextHolder.getContext();
            SecurityContext secCtx = userService.createSecurityContextSystem();
            SecurityContextHolder.setContext(secCtx);

            if (user == null) {
                user = createJWTUser(sub, name);
            }

            // prepare permissions and synchronize permissions
            updatePermissions(authorities, user);

            log.debug("Permissions for user '{}' are synchronized.", sub);

            // return back previous context
            SecurityContextHolder.setContext(prevSecCtx);

            return userService.createUserDetail(user);
        });
	}*/

	@Override
	public Authentication authenticate(Authentication authentication) throws AuthenticationException {
		logger.debug("ActiveDirectory - authenticate: {}", authentication.getName());
		
		String sourceIp = null;
	    if (authentication.getDetails() instanceof WebAuthenticationDetails) {
	    	var details = (WebAuthenticationDetails)authentication.getDetails();
	        sourceIp = details.getRemoteAddress();
	    }
		
		Authentication adResponse = null;
		try {
			adResponse = this.adProvider.authenticate(authentication);
		} catch (Exception e) {
			logger.debug("Authentication failed", e);
			return null;
		}
		if (adResponse == null || !adResponse.isAuthenticated()) {
            return null; // AD failed
		}
	     	    
		// prepare user detatil from DB
	    try {
	    	logger.debug("ActiveDirectory - authenticated: {}", adResponse);
	    	
		    var response = prepareDetails(authentication, adResponse);
		    siemAuditLogger.loginSuccess(authentication.getName(), sourceIp, AuthenticationType.ACTIVE_DIRECTORY);
			return response;
	    } catch (AuthenticationException e) {
	    	siemAuditLogger.loginFailed(authentication.getName(), sourceIp, e.getMessage());
		    throw e;
	    }
	}
	
	private Authentication prepareDetails(Authentication authentication, Authentication response) {
		return new TransactionTemplate(txManager).execute(r -> {
			
			String username = response.getName();
			UsrUser user = userService.findByUsername(username);
			if (user == null) {
				logger.info("ActiveDirectory - prepareDetails, user not found: {}", username);
				throw new UsernameNotFoundException("Neplatné uživatelské jméno nebo heslo: "+username);
			}

			return userService.createAuthentication(user);
		});		
	}

	@Override
	public boolean supports(Class<?> authentication) {
		// delegate to ActiveDirProvider
		return adProvider.supports(authentication);
	}

}
