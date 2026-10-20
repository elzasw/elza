package cz.tacr.elza.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Handler úspěšného odhlášení.
 *
 * @author <a href="mailto:jan.pikl@marbes.cz">Jan Pikl</a>
 *         Date: 15.12.2016
 */
@Component
public class ApiLogoutSuccessHandler implements LogoutSuccessHandler {

    @Autowired
    private SiemAuditLogger siemAuditLogger;

    @Override
    public void onLogoutSuccess(HttpServletRequest request, HttpServletResponse response,
                                Authentication authentication) throws IOException, ServletException {
        // null when the session had already expired - nobody was logged in
        if (authentication != null) {
            siemAuditLogger.logout(authentication.getName(), request.getRemoteAddr());
        }
        response.setStatus(HttpStatus.OK.value());
        response.getWriter().flush();
    }

}
