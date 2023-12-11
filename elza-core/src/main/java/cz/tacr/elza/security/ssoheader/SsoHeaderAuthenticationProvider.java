package cz.tacr.elza.security.ssoheader;

import org.apache.commons.lang3.StringUtils;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;

import cz.tacr.elza.domain.UsrUser;
import cz.tacr.elza.security.UserDetail;
import cz.tacr.elza.service.UserService;

public class SsoHeaderAuthenticationProvider implements AuthenticationProvider {

	private final UserService userService;

	public SsoHeaderAuthenticationProvider(UserService userService) {
		this.userService = userService;
	}

	@Override
	public Authentication authenticate(Authentication authentication) throws AuthenticationException {
		String username = authentication.getName();

		if (!StringUtils.EMPTY.equals(authentication.getCredentials())) {
			throw new BadCredentialsException("Neplatné uživatelské jméno nebo heslo");
		}

		UsrUser user = userService.findByUsername(username);
		if (user == null) {
			throw new UsernameNotFoundException("Neplatné uživatelské jméno nebo heslo");
		}

		UserDetail userDetail = userService.createUserDetail(user);

		UsernamePasswordAuthenticationToken result = new UsernamePasswordAuthenticationToken(username,
				StringUtils.EMPTY, null);
		result.setDetails(userDetail);
		return result;
	}

	@Override
	public boolean supports(Class<?> authentication) {
		return PreAuthenticatedAuthenticationToken.class.isAssignableFrom(authentication);
	}
}
