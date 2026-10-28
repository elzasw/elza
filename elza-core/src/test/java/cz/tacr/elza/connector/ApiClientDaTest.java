package cz.tacr.elza.connector;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import cz.tacr.elza.api.DaApiAuthentication;
import cz.tacr.elza.common.security.NoCheckTrustManager;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * TLS verification of the DA REST client is set by the repository option only; the
 * authentication method decides the Authorization header, not the certificate check.
 */
public class ApiClientDaTest {

    private static final String URL = "https://da.example/api";

    private static final OkHttpClient DEFAULT_CLIENT = new OkHttpClient();

    private static ApiClientDa createClient(DaApiAuthentication authentication, boolean tlsVerify) {
        return new ApiClientDa(URL, authentication, "elza", "secret", "user", "pass", tlsVerify);
    }

    /**
     * Sends a request through the client's interceptors and returns it as it would leave ELZA;
     * the last interceptor answers instead of the network.
     */
    private static Request sentRequest(ApiClientDa apiClient) throws IOException {
        AtomicReference<Request> sent = new AtomicReference<>();
        OkHttpClient client = apiClient.getHttpClient().newBuilder()
                .addInterceptor(chain -> {
                    sent.set(chain.request());
                    return new Response.Builder()
                            .request(chain.request())
                            .protocol(Protocol.HTTP_1_1)
                            .code(200)
                            .message("OK")
                            .body(ResponseBody.create("{}", MediaType.get("application/json")))
                            .build();
                })
                .build();
        client.newCall(new Request.Builder().url(URL + "/updates?count=1").build()).execute().close();
        return sent.get();
    }

    @Test
    void verify_withApiKey_keepsStandardVerification() {
        OkHttpClient client = createClient(DaApiAuthentication.API_KEY, true).getHttpClient();

        assertSame(DEFAULT_CLIENT.hostnameVerifier(), client.hostnameVerifier());
        assertFalse(client.x509TrustManager() instanceof NoCheckTrustManager);
    }

    @Test
    void verify_withoutAuthentication_keepsStandardVerification() {
        OkHttpClient client = createClient(DaApiAuthentication.NONE, true).getHttpClient();

        assertSame(DEFAULT_CLIENT.hostnameVerifier(), client.hostnameVerifier());
        assertFalse(client.x509TrustManager() instanceof NoCheckTrustManager);
    }

    @Test
    void skip_withoutAuthentication_trustsAnyCertificateAndHost() {
        OkHttpClient client = createClient(DaApiAuthentication.NONE, false).getHttpClient();

        assertInstanceOf(NoCheckTrustManager.class, client.x509TrustManager());
        assertTrue(client.hostnameVerifier().verify("other.example", null));
    }

    @Test
    void skip_withApiKey_trustsAnyCertificateAndHost() {
        OkHttpClient client = createClient(DaApiAuthentication.API_KEY, false).getHttpClient();

        assertInstanceOf(NoCheckTrustManager.class, client.x509TrustManager());
        assertTrue(client.hostnameVerifier().verify("other.example", null));
    }

    @Test
    void none_sendsNoAuthorization() throws IOException {
        Request request = sentRequest(createClient(DaApiAuthentication.NONE, true));

        assertNull(request.header(ApiClientDa.AUTHORIZATION));
        assertNull(request.header(ApiClientDa.X_NDA_DATE));
    }

    @Test
    void apiKey_signsRequest() throws IOException {
        Request request = sentRequest(createClient(DaApiAuthentication.API_KEY, true));

        assertTrue(request.header(ApiClientDa.AUTHORIZATION).startsWith("NDA-HMAC-SHA256 KeyId=elza,Signature="));
        assertEquals(14, request.header(ApiClientDa.X_NDA_DATE).length());
    }

    @Test
    void basic_sendsUsernameAndPassword() throws IOException {
        Request request = sentRequest(createClient(DaApiAuthentication.BASIC, true));

        // base64("user:pass")
        assertEquals("Basic dXNlcjpwYXNz", request.header(ApiClientDa.AUTHORIZATION));
        assertNull(request.header(ApiClientDa.X_NDA_DATE));
    }

    @Test
    void basic_withoutPassword_sendsEmptyPassword() throws IOException {
        Request request = sentRequest(new ApiClientDa(URL, DaApiAuthentication.BASIC, null, null, "user", null, true));

        // base64("user:")
        assertEquals("Basic dXNlcjo=", request.header(ApiClientDa.AUTHORIZATION));
    }

    @Test
    void apiKey_withoutKey_isRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new ApiClientDa(URL, DaApiAuthentication.API_KEY, null, null, "user", "pass", true));
    }

    @Test
    void basic_withoutUsername_isRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new ApiClientDa(URL, DaApiAuthentication.BASIC, "elza", "secret", null, "pass", true));
    }
}
