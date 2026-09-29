package cz.tacr.elza.security.kerberos;

import java.io.IOException;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
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
 * The JSON endpoint is called from the running application instead, so its reason is
 * written straight into the response body.
 * Other requests (e.g. API calls carrying a Negotiate header) are handled by the delegate.
 */
public class KerberosSsoFailureHandler implements AuthenticationFailureHandler {

	private static final Logger LOG = LoggerFactory.getLogger(KerberosSsoFailureHandler.class);

	/**
	 * Session attribute holding {@link KerberosSsoError}
	 */
	public static final String SESSION_ATTR_SSO_ERROR = KerberosSsoFailureHandler.class.getName() + ".error";

	private final RequestMatcher ssoRequestMatcher;
	private final RequestMatcher ssoJsonRequestMatcher;
	private final AuthenticationFailureHandler delegate;
	private final RedirectStrategy redirectStrategy = new DefaultRedirectStrategy();
	private final ObjectMapper objectMapper = new ObjectMapper();

	public KerberosSsoFailureHandler(final String ssoUrl, final AuthenticationFailureHandler delegate) {
		this(ssoUrl, null, delegate);
	}

	public KerberosSsoFailureHandler(final String ssoUrl, final String ssoJsonUrl,
			final AuthenticationFailureHandler delegate) {
		this.ssoRequestMatcher = new AntPathRequestMatcher(ssoUrl);
		this.ssoJsonRequestMatcher = ssoJsonUrl == null ? null : new AntPathRequestMatcher(ssoJsonUrl);
		this.delegate = delegate;
	}

	@Override
	public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
			AuthenticationException exception) throws IOException, ServletException {
		boolean isJsonSsoRequest = ssoJsonRequestMatcher != null && ssoJsonRequestMatcher.matches(request);
		if (!isJsonSsoRequest && !ssoRequestMatcher.matches(request)) {
			delegate.onAuthenticationFailure(request, response, exception);
			return;
		}
		var ssoError = KerberosSsoError.from(exception);
		LOG.debug("Windows (Kerberos) sign-in failed, code: {}", ssoError.code(), exception);

		// The caller is a fetch from the running application - it reads the reason from the body,
		// so there is no page load to carry it through the session.
		if (isJsonSsoRequest) {
			response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
			response.setContentType(MediaType.APPLICATION_JSON_VALUE);
			objectMapper.writeValue(response.getWriter(), ssoError);
			return;
		}

		request.getSession().setAttribute(SESSION_ATTR_SSO_ERROR, ssoError);
		redirectStrategy.sendRedirect(request, response, "/");
	}
}
