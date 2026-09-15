package cz.tacr.elza.security.apikey;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Reads {@code X-API-Key}, parses it, and hands it to the authentication manager. On success the
 * result goes into the {@link SecurityContextHolder} and the request continues; on failure the
 * {@link ApiKeyAuthenticationEntryPoint} writes a JSON 401 and the chain stops.
 *
 * The filter is only wired into the API-key security chain, which is selected via a request
 * matcher on the header — so a request without the header never reaches this code.
 */
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(ApiKeyAuthenticationFilter.class);

    private final ApiKeyProperties properties;
    private final AuthenticationManager authenticationManager;
    private final AuthenticationEntryPoint entryPoint;
    private final WebAuthenticationDetailsSource detailsSource = new WebAuthenticationDetailsSource();

    public ApiKeyAuthenticationFilter(ApiKeyProperties properties,
                                      AuthenticationManager authenticationManager,
                                      AuthenticationEntryPoint entryPoint) {
        this.properties = properties;
        this.authenticationManager = authenticationManager;
        this.entryPoint = entryPoint;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String header = request.getHeader(properties.getHeaderName());
        var parsed = ApiKeyToken.parse(header);
        if (parsed.isEmpty()) {
            fail(request, response, new ApiKeyAuthenticationException(ApiKeyFailure.MALFORMED_TOKEN, null));
            return;
        }
        ApiKeyToken token = parsed.get();
        ApiKeyAuthenticationToken carrier = new ApiKeyAuthenticationToken(token.keyId(), token.secret());
        carrier.setDetails(detailsSource.buildDetails(request));

        try {
            Authentication result = authenticationManager.authenticate(carrier);
            SecurityContextHolder.getContext().setAuthentication(result);
        } catch (AuthenticationException ex) {
            fail(request, response, ex);
            return;
        }

        chain.doFilter(request, response);
    }

    private void fail(HttpServletRequest request, HttpServletResponse response,
                      AuthenticationException ex) throws IOException, ServletException {
        SecurityContextHolder.clearContext();
        log.debug("API key authentication failed: {}", ex.getMessage());
        entryPoint.commence(request, response, ex);
    }
}
