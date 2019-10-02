package cz.tacr.elza.common.security;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.X509TrustManager;

/**
 * TrustManager který věří všem. Odpadají starosti s prošlými a self-signed certifikáty.
 *
 * @since 27. 11. 2016
 */
public class NoCheckTrustManager implements X509TrustManager {

    public NoCheckTrustManager(final X509TrustManager tm) {
    }

    @Override
    public X509Certificate[] getAcceptedIssuers() {
        return new X509Certificate[0];
    }

    @Override
    public void checkClientTrusted(final X509Certificate[] chain, final String authType)
            throws CertificateException {
    }

    @Override
    public void checkServerTrusted(final X509Certificate[] chain, final String authType)
            throws CertificateException {
    }
}
