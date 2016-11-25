package cz.tacr.elza.other;

import java.io.Reader;
import java.io.StringReader;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.List;

import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.JAXBIntrospector;
import javax.xml.bind.Unmarshaller;

import org.apache.cxf.configuration.jsse.TLSClientParameters;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.interceptor.LoggingInInterceptor;
import org.apache.cxf.interceptor.LoggingOutInterceptor;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.cxf.transports.http.configuration.HTTPClientPolicy;
import org.junit.Test;
import org.springframework.util.Assert;

import cz.tacr.elza.interpi.ws.WssoapSoap;
import cz.tacr.elza.interpi.ws.wo.EntitaTyp;
import cz.tacr.elza.interpi.ws.wo.SetTyp;

/**
 * Testy na volání INTERPI.
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

    @Test
    public void convertSearchResultToJava() throws JAXBException {
        WssoapSoap client = createClient();
        String oneRecord = client.getOneRecord("n000382567", username, password);

        StringReader stringReader = new StringReader(oneRecord);
        SetTyp set = unmarshallData(stringReader, SetTyp.class);
        List<EntitaTyp> entita = set.getEntita();
        for (EntitaTyp entitaTyp : entita) {
            entitaTyp.getContent().forEach(e -> {
                JAXBElement element = (JAXBElement) e;
                System.out.println(element.getName() + " " + element.getValue());
            });
        }
    }

    /**
     * Převede data z xml do objektu.
     *
     * @param inputStream stream
     *
     * @return objekt typu T
     */
    public static <T> T unmarshallData(final Reader reader, final Class<T> cls) throws JAXBException {
        Assert.notNull(reader);

//        Unmarshaller unmarshaller = createUnmarshaller(cls);
//        return (T) unmarshaller.unmarshal(reader);
        JAXBContext jaxbContext = JAXBContext.newInstance(cls);
        Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
        JAXBIntrospector jaxbIntrospector = jaxbContext.createJAXBIntrospector();

        Object unmarshal = unmarshaller.unmarshal(reader);
        System.out.println(unmarshal.getClass());
        System.out.println(jaxbIntrospector.getValue(unmarshal).getClass());


        return (T) jaxbIntrospector.getValue(unmarshal);
    }

    /**
     * Vytvoří unmarshaller pro data.
     *
     * @return unmarshaller pro data
     */
    private static <C> Unmarshaller createUnmarshaller(final Class<C> cls) throws JAXBException {
        JAXBContext jaxbContext = JAXBContext.newInstance(cls);

        return jaxbContext.createUnmarshaller();
    }

    private WssoapSoap createClient() {
        JaxWsProxyFactoryBean factory = new JaxWsProxyFactoryBean();
        factory.setServiceClass(WssoapSoap.class);
        factory.setAddress("https://195.113.132.114:443/csp/interpi/cust.interpi.ws.soap.cls");

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
