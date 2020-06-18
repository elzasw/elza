package cz.tacr.elza.connector;

import cz.tacr.cam.client.ApiClient;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.commons.lang3.StringUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

public class ApiClientCam extends ApiClient {

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
        camInit(url, apiKey, apiValue);
    }

    protected void camInit(final String url, final String apiKey, final String apiValue) {
        setBasePath(url);

        OkHttpClient.Builder builder = new OkHttpClient.Builder().addInterceptor(new Interceptor() {
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
