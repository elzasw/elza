package cz.tacr.elza.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Chybový handler pro autentikaci.
 *
 * Answers 401 with the reason as a code, so the login form can show a message:
 * {@value #CODE_USER_INACTIVE} for a deactivated user, {@value #CODE_BAD_CREDENTIALS}
 * otherwise. A deactivated user is reported only after the password was verified, so the
 * code does not reveal the state of an account to someone without its password.
 *
 * @since 11.04.2016
 */
@Component
public class ApiAuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {

	private static final Logger LOGGER = LoggerFactory.getLogger(ApiAuthenticationFailureHandler.class);

	public static final String CODE_USER_INACTIVE = "USER_INACTIVE";

	public static final String CODE_BAD_CREDENTIALS = "BAD_CREDENTIALS";

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException exception) throws IOException, ServletException {
    	if(LOGGER.isDebugEnabled()) {
			LOGGER.debug("Authentication failure, exception: ", exception);
    	}
        String code = exception instanceof LockedException ? CODE_USER_INACTIVE : CODE_BAD_CREDENTIALS;
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("{\"code\":\"" + code + "\"}");
    }
}
