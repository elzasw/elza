package cz.tacr.elza.utils;

import javax.net.ssl.TrustManager;

import org.apache.cxf.configuration.jsse.TLSClientParameters;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.interceptor.LoggingInInterceptor;
import org.apache.cxf.interceptor.LoggingOutInterceptor;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.cxf.transports.http.configuration.HTTPClientPolicy;

/**
 *
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 5. 12. 2016
 */
public class WSUtils {

    public static <T> T createClient(final String url, final Class<T> wsInterface) {
        JaxWsProxyFactoryBean factory = new JaxWsProxyFactoryBean();
        factory.setServiceClass(wsInterface);
        factory.setAddress(url);

        // logování dotazů a odpovědí
        LoggingInInterceptor loggingInInterceptor = new LoggingInInterceptor();
        loggingInInterceptor.setPrettyLogging(true);
        LoggingOutInterceptor loggingOutInterceptor = new LoggingOutInterceptor();
        loggingOutInterceptor.setPrettyLogging(true);

        factory.getOutFaultInterceptors().add(loggingOutInterceptor);
        factory.getOutInterceptors().add(loggingOutInterceptor);
        factory.getInFaultInterceptors().add(loggingInInterceptor);
        factory.getInInterceptors().add(loggingInInterceptor);

        T client = (T) factory.create();
        Client proxy = ClientProxy.getClient(client);

        // vypnutí kontroly certifikátů
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
}
