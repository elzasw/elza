package cz.tacr.elza.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

/**
 * {@link ApiAuthenticationFailureHandler}: a failed login answers 401 with the reason as a code.
 */
class ApiAuthenticationFailureHandlerTest {

    private MockHttpServletResponse fail(final AuthenticationException exception) throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        new ApiAuthenticationFailureHandler().onAuthenticationFailure(new MockHttpServletRequest(), response, exception);
        return response;
    }

    @Test
    void deactivatedUserIsReported() throws Exception {
        MockHttpServletResponse response = fail(new LockedException("User is not active"));

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentType()).startsWith("application/json");
        assertThat(response.getContentAsString()).isEqualTo("{\"code\":\"USER_INACTIVE\"}");
    }

    @Test
    void otherFailuresAreBadCredentials() throws Exception {
        assertThat(fail(new UsernameNotFoundException("x")).getContentAsString())
                .isEqualTo("{\"code\":\"BAD_CREDENTIALS\"}");
        assertThat(fail(new BadCredentialsException("x")).getContentAsString())
                .isEqualTo("{\"code\":\"BAD_CREDENTIALS\"}");
    }
}
