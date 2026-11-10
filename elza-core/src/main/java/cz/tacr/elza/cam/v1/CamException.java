package cz.tacr.elza.cam.v1;

import java.security.cert.CertificateException;

import javax.net.ssl.SSLPeerUnverifiedException;

import org.apache.commons.lang3.StringUtils;

import cz.tacr.cam.v1.client.ApiException;
import cz.tacr.elza.exception.AbstractException;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.ExternalCode;

public class CamException {

    public static AbstractException prepareExtSystemException(ApiException e) {
        if (isTlsCertificateError(e)) {
            return new SystemException("TLS certificate of the external system was not verified", e,
                    ExternalCode.TLS_CERTIFICATE_ERROR);
        }
        return new SystemException("Došlo k chybě při komunikaci s externím systémem.", e)
                .set("responseBody", e.getResponseBody())
                .set("responseCode", e.getCode())
                .set("responseHeaders", e.getResponseHeaders());
    }

    /**
     * Certificate not trusted (self-signed, unknown CA, expired, weak algorithm)
     * or issued for another host name.
     */
    private static boolean isTlsCertificateError(Throwable e) {
        for (Throwable t = e; t != null; t = t.getCause()) {
            if (t instanceof SSLPeerUnverifiedException || t instanceof CertificateException) {
                return true;
            }
        }
        return false;
    }

    public static String getApiExceptionInfo(ApiException e) {
        StringBuilder sb = new StringBuilder(e.getMessage());
        sb.append(", code: ").append(e.getCode());
        String body = e.getResponseBody();
        if (StringUtils.isNotEmpty(body)) {
            sb.append(", response: ").append(body);
        }
        return sb.toString();
    }
}
