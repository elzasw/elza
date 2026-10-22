package cz.tacr.elza.cam.v1;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import cz.tacr.elza.common.security.NoCheckTrustManager;
import okhttp3.OkHttpClient;

/**
 * TLS verification of the CAM v1 client is set by the external system option.
 */
public class ApiClientCamTest {

    private static final String URL = "https://cam.example/api/v1";

    private static final OkHttpClient DEFAULT_CLIENT = new OkHttpClient();

    @Test
    void verify_keepsStandardVerification() {
        OkHttpClient client = new ApiClientCam(URL, "elza", "secret", true).getHttpClient();

        assertSame(DEFAULT_CLIENT.hostnameVerifier(), client.hostnameVerifier());
        assertFalse(client.x509TrustManager() instanceof NoCheckTrustManager);
    }

    @Test
    void skip_trustsAnyCertificateAndHost() {
        OkHttpClient client = new ApiClientCam(URL, "elza", "secret", false).getHttpClient();

        assertInstanceOf(NoCheckTrustManager.class, client.x509TrustManager());
        assertTrue(client.hostnameVerifier().verify("other.example", null));
    }
}
