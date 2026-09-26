package cz.tacr.elza.security.kerberos;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;

import jakarta.servlet.http.HttpServletResponse;

class KerberosSsoFailureHandlerTest {

	private static final String SSO_URL = "/authenticate/sso";

	private final KerberosSsoFailureHandler handler = new KerberosSsoFailureHandler(SSO_URL,
			(request, response, exception) -> response.sendError(HttpServletResponse.SC_UNAUTHORIZED));

	private MockHttpServletRequest request(String servletPath) {
		var request = new MockHttpServletRequest("GET", "/elza" + servletPath);
		request.setContextPath("/elza");
		request.setServletPath(servletPath);
		return request;
	}

	private KerberosSsoError handleSso(AuthenticationException exception, MockHttpServletResponse response)
			throws Exception {
		var request = request(SSO_URL);
		handler.onAuthenticationFailure(request, response, exception);
		return (KerberosSsoError) request.getSession().getAttribute(KerberosSsoFailureHandler.SESSION_ATTR_SSO_ERROR);
	}

	@Test
	void unknownUserRedirectsWithUsername() throws Exception {
		var response = new MockHttpServletResponse();
		var ssoError = handleSso(new KerberosUserNotFoundException("pyta"), response);

		assertEquals("/elza/", response.getRedirectedUrl());
		assertEquals(new KerberosSsoError(KerberosSsoError.USER_NOT_FOUND, "pyta"), ssoError);
	}

	@Test
	void inactiveUser() throws Exception {
		var ssoError = handleSso(new LockedException("User is not active"), new MockHttpServletResponse());

		assertEquals(new KerberosSsoError(KerberosSsoError.USER_INACTIVE, null), ssoError);
	}

	@Test
	void otherFailure() throws Exception {
		var ssoError = handleSso(new BadCredentialsException("invalid ticket"), new MockHttpServletResponse());

		assertEquals(new KerberosSsoError(KerberosSsoError.FAILED, null), ssoError);
	}

	@Test
	void otherRequestsUseDelegate() throws Exception {
		var request = request("/api/user/detail");
		var response = new MockHttpServletResponse();
		handler.onAuthenticationFailure(request, response, new KerberosUserNotFoundException("pyta"));

		assertEquals(HttpServletResponse.SC_UNAUTHORIZED, response.getStatus());
		assertNull(response.getRedirectedUrl());
		assertNull(request.getSession(false));
	}
}
