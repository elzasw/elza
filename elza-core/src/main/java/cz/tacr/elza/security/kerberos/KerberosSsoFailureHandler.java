package cz.tacr.elza.security.kerberos;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.DefaultRedirectStrategy;
import org.springframework.security.web.RedirectStrategy;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Failure handler of the SPNEGO filter.
 *
 * The browser opens the SSO endpoint by a full page navigation, so a plain 401
 * leaves the user on the browser's own error page without any explanation.
 * For the SSO endpoint the reason is stored in the session and the browser is
 * redirected back to the application, where the login dialog displays it.
 * Other requests (e.g. API calls carrying a Negotiate header) are handled by the delegate.
 */
public class KerberosSsoFailureHandler implements AuthenticationFailureHandler {

	private static final Logger LOG = LoggerFactory.getLogger(KerberosSsoFailureHandler.class);

	/**
	 * Session attribute holding {@link KerberosSsoError}
	 */
	public static final String SESSION_ATTR_SSO_ERROR = KerberosSsoFailureHandler.class.getName() + ".error";

	private final RequestMatcher ssoRequestMatcher;
	private final AuthenticationFailureHandler delegate;
	private final RedirectStrategy redirectStrategy = new DefaultRedirectStrategy();

	public KerberosSsoFailureHandler(final String ssoUrl, final AuthenticationFailureHandler delegate) {
		this.ssoRequestMatcher = new AntPathRequestMatcher(ssoUrl);
		this.delegate = delegate;
	}

	@Override
	public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
			AuthenticationException exception) throws IOException, ServletException {
		if (!ssoRequestMatcher.matches(request)) {
			delegate.onAuthenticationFailure(request, response, exception);
			return;
		}
		var ssoError = KerberosSsoError.from(exception);
		LOG.debug("Windows (Kerberos) sign-in failed, code: {}", ssoError.code(), exception);

		request.getSession().setAttribute(SESSION_ATTR_SSO_ERROR, ssoError);
		redirectStrategy.sendRedirect(request, response, "/");
	}
}
