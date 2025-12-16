package cz.tacr.elza.security.ssoheader;

import org.apache.commons.lang3.StringUtils;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import cz.tacr.elza.domain.UsrUser;
import cz.tacr.elza.service.UserService;

public class SsoHeaderAuthenticationProvider implements AuthenticationProvider {

	private final UserService userService;
	private PlatformTransactionManager txManager;

	public SsoHeaderAuthenticationProvider(UserService userService, final PlatformTransactionManager txManager) {
		this.userService = userService;
		this.txManager = txManager;
	}

	@Override
	public Authentication authenticate(Authentication authentication) throws AuthenticationException {
		String username = authentication.getName();

		// Why credentials is empty?
		if (!StringUtils.EMPTY.equals(authentication.getCredentials())) {
			throw new BadCredentialsException("Neplatné uživatelské jméno nebo heslo: "+username);
		}

		return new TransactionTemplate(txManager).execute(r -> {
		UsrUser user = userService.findByUsername(username);
		if (user == null) {
			throw new UsernameNotFoundException("Neplatné uživatelské jméno nebo heslo: "+username);
		}
		
		return userService.createAuthentication(user);
		});
	}

	@Override
	public boolean supports(Class<?> authentication) {
		return PreAuthenticatedAuthenticationToken.class.isAssignableFrom(authentication);
	}
}
