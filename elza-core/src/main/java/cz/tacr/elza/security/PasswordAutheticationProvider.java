package cz.tacr.elza.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import cz.tacr.elza.domain.UsrAuthentication;
import cz.tacr.elza.domain.UsrUser;
import cz.tacr.elza.service.UserService;

public class PasswordAutheticationProvider implements AuthenticationProvider {

	private static final Logger log = LoggerFactory.getLogger(PasswordAutheticationProvider.class);

	private final UserService userService;

	public PasswordAutheticationProvider(UserService userService) {
		this.userService = userService;
	}

	@Override
	public Authentication authenticate(final Authentication authentication) throws AuthenticationException {
		String username = authentication.getName();
		String password = authentication.getCredentials().toString();
		String encodePassword = userService.encodePassword(password);

		UsrUser user = userService.findByUsername(username);

		if (user != null) {
			UsrAuthentication usrAuthentication = userService.findAuthentication(user,
					UsrAuthentication.AuthType.PASSWORD);
			if (usrAuthentication == null) {
				throw new UsernameNotFoundException("Pro uživatele není povolen tento typ přihlášení");
			}

			encodePassword = usrAuthentication.getAuthValue();
			if (!userService.matchesPassword(password, encodePassword, username)) {
				throw new UsernameNotFoundException("Neplatné uživatelské jméno nebo heslo");
			}

			if (!user.getActive()) {
				throw new LockedException("User is not active");
			}
		} else {
			// TODO: smazat po vytvoření správy uživatelů
			log.warn(username + ":" + encodePassword);
			throw new UsernameNotFoundException("Neplatné uživatelské jméno nebo heslo");
		}

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
