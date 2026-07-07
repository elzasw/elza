package cz.tacr.elza.connector;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cz.tacr.elza.aiprovider.ApiClient;
import cz.tacr.elza.exception.SystemException;
import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okio.Buffer;

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
    private static final long READ_TIMEOUT_MS = 90_000;

    private static final long CONNECT_TIMEOUT_MS = 10_000;

    /** Response-body bytes copied into the log of a failed call (peeked, never consumed). */
    private static final long ERROR_BODY_LOG_LIMIT = 4096;

    public ApiClientAiProvider(final String url, final String keyId, final String secret) {
        super();
        setBasePath(StringUtils.removeEnd(url, "/"));

        OkHttpClient httpClient = new OkHttpClient.Builder()
                .connectTimeout(CONNECT_TIMEOUT_MS, TimeUnit.MILLISECONDS)
                .readTimeout(READ_TIMEOUT_MS, TimeUnit.MILLISECONDS)
                .writeTimeout(READ_TIMEOUT_MS, TimeUnit.MILLISECONDS)
                .addInterceptor(new Interceptor() {
                    @Override
                    public Response intercept(Chain chain) throws IOException {
                        Request request = chain.request();
                        String date = Instant.now().truncatedTo(ChronoUnit.SECONDS).toString();
                        String stringToSign = stringToSign(request, date);
                        String signature = sign(secret, stringToSign);
                        Request signed = request.newBuilder()
                                // header() replaces any value set by the generated code,
                                // so the signed date is always the transmitted one
                                .header(DATE_HEADER, date)
                                .header("Authorization",
                                        SCHEME + " KeyId=" + keyId + ",Signature=" + signature)
                                .build();
                        // The Authorization header carries only the KeyId and the
                        // per-request signature (never the secret), so logging the
                        // request line and auth identity here is safe.
                        logger.debug("AI provider request: {} {} (KeyId={}, {}={})",
                                signed.method(), signed.url(), keyId, DATE_HEADER, date);

                        long startNanos = System.nanoTime();
                        Response response;
                        try {
                            response = chain.proceed(signed);
                        } catch (IOException | RuntimeException e) {
                            // No HTTP response at all (connect refused, timeout, TLS…):
                            // name the target so the failure is not a bare stack trace.
                            logger.warn("AI provider request failed (no response): {} {} (KeyId={}): {}",
                                    signed.method(), signed.url(), keyId, e.toString());
                            throw e;
                        }
                        long elapsedMs = (System.nanoTime() - startNanos) / 1_000_000;

                        if (response.isSuccessful()) {
                            logger.debug("AI provider response: {} {} -> {} in {} ms",
                                    signed.method(), signed.url(), response.code(), elapsedMs);
                        } else {
                            // A non-2xx (e.g. 501 Not Implemented) may come from the
                            // provider or a proxy in front of it — the Server header and
                            // body snippet tell which, and the URL/method tell what was
                            // rejected.
                            logger.warn("AI provider response: {} {} -> {} {} in {} ms"
                                    + " (Server={}, Content-Type={}); body: {}",
                                    signed.method(), signed.url(), response.code(),
                                    StringUtils.defaultIfEmpty(response.message(), "?"), elapsedMs,
                                    response.header("Server"), response.header("Content-Type"),
                                    peekBody(response));
                        }
                        return response;
                    }
                })
                .build();
        setHttpClient(httpClient);
    }

    /** The six-line canonical string (LF-joined, no trailing newline). */
    private static String stringToSign(Request request, String date) throws IOException {
        HttpUrl url = request.url();
        String host = url.host();
        if (url.port() != HttpUrl.defaultPort(url.scheme())) {
            host = host + ":" + url.port();
        }
        return String.join("\n",
                request.method(),
                host,
                url.encodedPath(),
                StringUtils.defaultString(url.encodedQuery()),
                date,
                sha256Hex(bodyBytes(request.body())));
    }

    /**
     * Best-effort copy of a failed response's body for logging. Uses
     * {@code peekBody}, so the real body stream stays intact for the generated
     * client to read; the copy is capped at {@link #ERROR_BODY_LOG_LIMIT} bytes.
     */
    private static String peekBody(Response response) {
        try {
            return response.peekBody(ERROR_BODY_LOG_LIMIT).string();
        } catch (IOException e) {
            return "<unreadable: " + e.getMessage() + ">";
        }
    }

    private static byte[] bodyBytes(RequestBody body) throws IOException {
        if (body == null) {
            return new byte[0];
        }
        Buffer buffer = new Buffer();
        body.writeTo(buffer);
        return buffer.readByteArray();
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
