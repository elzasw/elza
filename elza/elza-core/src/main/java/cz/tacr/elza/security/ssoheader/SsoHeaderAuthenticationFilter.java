package cz.tacr.elza.security.ssoheader;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.WebAttributes;
import org.springframework.security.web.authentication.preauth.AbstractPreAuthenticatedProcessingFilter;

public class SsoHeaderAuthenticationFilter extends AbstractPreAuthenticatedProcessingFilter {

	private static final FilterChain DUMMY_FILTER_CHAIN = (request, response) -> {};

	private final String userHeader;

	public SsoHeaderAuthenticationFilter(SsoHeaderProperties properties) {
		this.userHeader = properties.getUserHeader();
		setCheckForPrincipalChanges(true);
		setInvalidateSessionOnPrincipalChange(true);
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {
		super.doFilter(request, response, DUMMY_FILTER_CHAIN);

		if (request.getAttribute(WebAttributes.AUTHENTICATION_EXCEPTION) == null) {
			chain.doFilter(request, response);
		}
	}

	@Override
	protected boolean principalChanged(HttpServletRequest request, Authentication currentAuthentication) {
		String username = getPreAuthenticatedPrincipal(request);
		if (username == null) {
			return false;
		}
		return super.principalChanged(request, currentAuthentication);
	}

	@Override
	protected String getPreAuthenticatedPrincipal(HttpServletRequest request) {
		return StringUtils.trimToNull(request.getHeader(userHeader));
	}

	@Override
	protected Object getPreAuthenticatedCredentials(HttpServletRequest request) {
		return StringUtils.EMPTY;
	}
}
