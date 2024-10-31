package cz.tacr.elza.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
 * @since 11.04.2016
 */
@Component
public class ApiAuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(ApiAuthenticationFailureHandler.class);
	
    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException exception) throws IOException, ServletException {
    	if(LOGGER.isDebugEnabled()) {
			LOGGER.debug("Authentication failure, exception: ", exception);
    	}
        super.onAuthenticationFailure(request, response, exception);
    }
}
