package cz.tacr.elza.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Úspěšný handler pro autentikaci.
 *
 * @since 11.04.2016
 */
@Component
public class ApiAuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(ApiAuthenticationSuccessHandler.class);

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
    	if(LOGGER.isDebugEnabled()) {
    		StringBuilder sb = new StringBuilder();
    		sb.append("Authentication suceeded.");
    		if(authentication!=null) {
    			Object detail = authentication.getDetails();
    			if(detail!=null) {
    				sb.append(" Detail: ").append(detail.toString());	
    			}
    		}
    		LOGGER.debug(sb.toString());
    	}    	
    	
        clearAuthenticationAttributes(request);
    }
}
