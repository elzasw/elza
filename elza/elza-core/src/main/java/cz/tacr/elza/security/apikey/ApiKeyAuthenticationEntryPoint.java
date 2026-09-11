package cz.tacr.elza.security.apikey;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Writes a JSON body for a rejected {@code X-API-Key} request. The {@code code} distinguishes
 * the reason so the client can react programmatically; the {@code message} is what the person
 * running the integration reads.
 */
@Component
public class ApiKeyAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        ApiKeyFailure failure = (authException instanceof ApiKeyAuthenticationException apiEx)
                ? apiEx.getFailure()
                : ApiKeyFailure.INVALID_SECRET;

        Map<String, String> body = new LinkedHashMap<>();
        body.put("code", failure.name());
        body.put("message", failure.getMessage());

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
