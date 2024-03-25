package cz.tacr.elza.connector;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import jakarta.validation.constraints.NotNull;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cz.tacr.cam.client.ApiClient;
import cz.tacr.elza.common.security.NoCheckTrustManager;
import cz.tacr.elza.exception.SystemException;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class ApiClientCam extends ApiClient {

    private static Logger log = LoggerFactory.getLogger(ApiClientCam.class);

    /**
     * HTTP hlavičky
     */
    public static final String AUTHORIZATION = "Authorization";
    public static final String X_NDA_DATE = "X-NDA-Date";

    public static final DateTimeFormatter X_NDA_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    public static final int CONNECTION_TIMEOUT = 60000;

    public ApiClientCam(@NotNull final String url,
                        @NotNull final String apiKey,
                        @NotNull final String apiValue) {
        super();
        try {
            camInit(url, apiKey, apiValue);
        } catch (GeneralSecurityException gse) {
            throw new SystemException("Failed to initialize SSL client", gse);
        }
    }

    protected void camInit(final String url, final String apiKey, final String apiValue)
            throws GeneralSecurityException {
        setBasePath(url);

        X509TrustManager trustManager = new NoCheckTrustManager();
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null,
                        new TrustManager[] { trustManager },
                        null);
        SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .sslSocketFactory(sslSocketFactory, trustManager)
                .addInterceptor(new Interceptor() {
            @Override
            public Response intercept(Chain chain) throws IOException {
                chain = chain
                        .withConnectTimeout(CONNECTION_TIMEOUT, TimeUnit.MILLISECONDS)
                        .withReadTimeout(CONNECTION_TIMEOUT, TimeUnit.MILLISECONDS)
                        .withWriteTimeout(CONNECTION_TIMEOUT, TimeUnit.MILLISECONDS);

                Request request = chain.request();
                String xNdaDate = OffsetDateTime.now(ZoneOffset.UTC).format(X_NDA_DATE_FORMATTER);
                String dataToHash = getHost(request) + request.method() + "/" + String.join("/", request.url().pathSegments()) + StringUtils.defaultString(request.url().query()) + xNdaDate;
                String signature = computeHmac(apiValue, dataToHash);
                if (log.isDebugEnabled()) {
                    log.debug("Data to hash: {}", dataToHash);
                            log.debug("Authorization NDA-HMAC-SHA256 (KeyId={}, KeyValue={}), Signature={}", apiKey,
                                      apiValue,
                                      signature);
                }
                String authorization = "NDA-HMAC-SHA256 KeyId=" + apiKey + ",Signature=" + signature;
                Request newRequest = request.newBuilder()
                        .addHeader(X_NDA_DATE, xNdaDate)
                        .addHeader(AUTHORIZATION, authorization)
                        .build();
                return chain.proceed(newRequest);
            }
        });
        OkHttpClient httpClient = builder.build();
        setHttpClient(httpClient);
    }

    private String getHost(final Request request) {
        return request.url().host();
    }

    private String computeHmac(final String apiValue, final String dataToHash) {
        try {
            Mac hmac = Mac.getInstance("HmacSHA256");
            byte[] bytes = apiValue.getBytes(StandardCharsets.UTF_8);
            hmac.init(new SecretKeySpec(bytes, hmac.getAlgorithm()));
            byte[] result = hmac.doFinal(dataToHash.getBytes());

            return Base64.getEncoder().withoutPadding().encodeToString(result);
        } catch (Exception e) {
            return null;
        }
    }
}
