package cz.tacr.elza.other;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.cxf.aegis.databinding.AegisDatabinding;
import org.apache.cxf.configuration.jsse.TLSClientParameters;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.frontend.ClientProxyFactoryBean;
import org.apache.cxf.interceptor.LoggingInInterceptor;
import org.apache.cxf.interceptor.LoggingOutInterceptor;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.cxf.transports.http.configuration.HTTPClientPolicy;
import org.junit.Test;
import org.tempuri.WssoapSoap;

/**
 * Testy na volání INITERPI.
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 23. 11. 2016
 */
public class InterpiTest {

    private String username = "tacr";
    private String password = "tacrinterpi2015";
    private String from = "1";
    private String to = "10";

    @Test
    public void findDataTest() {
        WssoapSoap client = createClient();
        String query = "@attr 1=2051 @and @or @or 'r' 'o' @or 'k' 'u' @attr 1=2055 'rod'";
        String data = client.findData(query, null, from, to, username, password);
        System.out.println(data);
    }

    @Test
    public void getOneRecordTest() {
        WssoapSoap client = createClient();
        String oneRecord = client.getOneRecord("n000382567", username, password);
        System.out.println(oneRecord);
    }

    private WssoapSoap createClient() {
        ClientProxyFactoryBean factory = new ClientProxyFactoryBean();
        factory.setServiceClass(WssoapSoap.class);
        factory.setAddress("https://195.113.132.114:443/csp/interpi/cust.interpi.ws.soap.cls");
        factory.setDataBinding(new AegisDatabinding());

        LoggingInInterceptor loggingInInterceptor = new LoggingInInterceptor();
        loggingInInterceptor.setPrettyLogging(true);
        LoggingOutInterceptor loggingOutInterceptor = new LoggingOutInterceptor();
        loggingOutInterceptor.setPrettyLogging(true);

        factory.getOutFaultInterceptors().add(loggingOutInterceptor);
        factory.getOutInterceptors().add(loggingOutInterceptor);
        factory.getInFaultInterceptors().add(loggingInInterceptor);
        factory.getInInterceptors().add(loggingInInterceptor);

        WssoapSoap client = (WssoapSoap) factory.create();
        Client proxy = ClientProxy.getClient(client);

        HTTPConduit conduit = (HTTPConduit) proxy.getConduit();
        TLSClientParameters tcp = new TLSClientParameters();
        tcp.setTrustManagers(new TrustManager[] {new NoCheckTrustManager(null)});
        tcp.setDisableCNCheck(true);
        conduit.setTlsClientParameters(tcp);

        HTTPClientPolicy httpClientPolicy = new HTTPClientPolicy();
        httpClientPolicy.setAllowChunking(false);
        conduit.setClient(httpClientPolicy);

        return client;
    }

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
}
