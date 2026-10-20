package cz.tacr.elza.connector;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import cz.tacr.elza.common.security.NoCheckTrustManager;
import okhttp3.OkHttpClient;

/**
 * TLS verification of the DA REST client is set by the repository option only; the API key
 * decides the signature, not the certificate check.
 */
public class ApiClientDaTest {

    private static final String URL = "https://da.example/api";

    private static final OkHttpClient DEFAULT_CLIENT = new OkHttpClient();

    @Test
    void verify_withApiKey_keepsStandardVerification() {
        OkHttpClient client = new ApiClientDa(URL, "elza", "secret", true).getHttpClient();

        assertSame(DEFAULT_CLIENT.hostnameVerifier(), client.hostnameVerifier());
        assertFalse(client.x509TrustManager() instanceof NoCheckTrustManager);
    }

    @Test
    void verify_withoutApiKey_keepsStandardVerification() {
        OkHttpClient client = new ApiClientDa(URL, null, null, true).getHttpClient();

        assertSame(DEFAULT_CLIENT.hostnameVerifier(), client.hostnameVerifier());
        assertFalse(client.x509TrustManager() instanceof NoCheckTrustManager);
    }

    @Test
    void skip_withoutApiKey_trustsAnyCertificateAndHost() {
        OkHttpClient client = new ApiClientDa(URL, null, null, false).getHttpClient();

        assertInstanceOf(NoCheckTrustManager.class, client.x509TrustManager());
        assertTrue(client.hostnameVerifier().verify("other.example", null));
    }

    @Test
    void skip_withApiKey_trustsAnyCertificateAndHost() {
        OkHttpClient client = new ApiClientDa(URL, "elza", "secret", false).getHttpClient();

        assertInstanceOf(NoCheckTrustManager.class, client.x509TrustManager());
        assertTrue(client.hostnameVerifier().verify("other.example", null));
    }
}
