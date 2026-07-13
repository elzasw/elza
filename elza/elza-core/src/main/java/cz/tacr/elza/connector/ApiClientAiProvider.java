package cz.tacr.elza.connector;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import cz.tacr.elza.aiprovider.ApiClient;
import cz.tacr.elza.exception.SystemException;

/**
 * HTTP client for an AI provider (protocol "Elza AI Provider API"), signing
 * every request with the {@code ELZA-AI-HMAC-SHA256} scheme — see
 * {@code elza-development/typespec-ai/security.md} for the authoritative
 * definition (six-line canonical string incl. the body hash; the secret is
 * never transmitted). Read timeout is sized for the protocol's long poll.
 */
public class ApiClientAiProvider extends ApiClient {

    private static final Logger logger = LoggerFactory.getLogger(ApiClientAiProvider.class);

    public static final String SCHEME = "ELZA-AI-HMAC-SHA256";

    public static final String DATE_HEADER = "X-AI-Date";

    /** Long-poll friendly read timeout (protocol wait is capped at 60 s). */
    private static final Duration READ_TIMEOUT = Duration.ofMillis(90_000);

    private static final Duration CONNECT_TIMEOUT = Duration.ofMillis(10_000);

    public ApiClientAiProvider(final String url, final String keyId, final String secret) {
        super(buildRestClient(keyId, secret));
        setBasePath(StringUtils.removeEnd(url, "/"));
    }

    private static RestClient buildRestClient(final String keyId, final String secret) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(CONNECT_TIMEOUT);
        requestFactory.setReadTimeout(READ_TIMEOUT);

        // buildRestClientBuilder registers the Jackson converter with the same
        // ObjectMapper config the generated client uses (typed AiObject subtypes).
        return buildRestClientBuilder(createDefaultObjectMapper(null))
                .requestFactory(requestFactory)
                .requestInterceptor(new SigningInterceptor(keyId, secret))
                .build();
    }

    /**
     * Signs each outgoing request and logs its outcome. Because interceptors run
     * after the body is serialized, {@code body} is the exact bytes hashed into
     * the canonical string, and the (mutable) headers are the ones transmitted —
     * so the signed {@code X-AI-Date} is always the one sent.
     */
    private static final class SigningInterceptor implements ClientHttpRequestInterceptor {

        private final String keyId;
        private final String secret;

        SigningInterceptor(final String keyId, final String secret) {
            this.keyId = keyId;
            this.secret = secret;
        }

        @Override
        public ClientHttpResponse intercept(HttpRequest request, byte[] body,
                                            ClientHttpRequestExecution execution) throws IOException {
            String date = Instant.now().truncatedTo(ChronoUnit.SECONDS).toString();
            String signature = sign(secret, stringToSign(request, body, date));

            HttpHeaders headers = request.getHeaders();
            headers.set(DATE_HEADER, date);
            headers.set(HttpHeaders.AUTHORIZATION,
                    SCHEME + " KeyId=" + keyId + ",Signature=" + signature);

            // The Authorization header carries only the KeyId and the per-request
            // signature (never the secret), so logging the request line and auth
            // identity here is safe.
            logger.debug("AI provider request: {} {} (KeyId={}, {}={})",
                    request.getMethod(), request.getURI(), keyId, DATE_HEADER, date);

            long startNanos = System.nanoTime();
            ClientHttpResponse response;
            try {
                response = execution.execute(request, body);
            } catch (IOException | RuntimeException e) {
                // No HTTP response at all (connect refused, timeout, TLS…):
                // name the target so the failure is not a bare stack trace.
                logger.warn("AI provider request failed (no response): {} {} (KeyId={}): {}",
                        request.getMethod(), request.getURI(), keyId, e.toString());
                throw e;
            }
            long elapsedMs = (System.nanoTime() - startNanos) / 1_000_000;

            // Reading status/headers does not consume the body stream, so the
            // generated client can still read it (or a RestClientResponseException
            // can still carry it on a non-2xx status).
            if (response.getStatusCode().is2xxSuccessful()) {
                logger.debug("AI provider response: {} {} -> {} in {} ms",
                        request.getMethod(), request.getURI(), response.getStatusCode().value(), elapsedMs);
            } else {
                HttpHeaders responseHeaders = response.getHeaders();
                logger.warn("AI provider response: {} {} -> {} in {} ms (Server={}, Content-Type={})",
                        request.getMethod(), request.getURI(), response.getStatusCode().value(), elapsedMs,
                        responseHeaders.getFirst("Server"), responseHeaders.getFirst("Content-Type"));
            }
            return response;
        }
    }

    /** The six-line canonical string (LF-joined, no trailing newline). */
    private static String stringToSign(HttpRequest request, byte[] body, String date) {
        URI uri = request.getURI();
        String host = uri.getHost();
        int port = uri.getPort();
        if (port != -1 && port != defaultPort(uri.getScheme())) {
            host = host + ":" + port;
        }
        return String.join("\n",
                request.getMethod().name(),
                host,
                StringUtils.defaultString(uri.getRawPath()),
                StringUtils.defaultString(uri.getRawQuery()),
                date,
                sha256Hex(body == null ? new byte[0] : body));
    }

    private static int defaultPort(String scheme) {
        return "https".equalsIgnoreCase(scheme) ? 443 : 80;
    }

    private static String sha256Hex(byte[] body) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(body);
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                sb.append(Character.forDigit((b >> 4) & 0xF, 16));
                sb.append(Character.forDigit(b & 0xF, 16));
            }
            return sb.toString();
        } catch (GeneralSecurityException e) {
            throw new SystemException("SHA-256 unavailable", e);
        }
    }

    private static String sign(String secret, String stringToSign) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] signature = mac.doFinal(stringToSign.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(signature);
        } catch (GeneralSecurityException e) {
            throw new SystemException("HMAC-SHA256 unavailable", e);
        }
    }
}
