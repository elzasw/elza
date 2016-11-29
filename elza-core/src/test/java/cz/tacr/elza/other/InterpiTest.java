package cz.tacr.elza.other;

import java.io.Reader;
import java.io.StringReader;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.stream.IntStream;

import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.transaction.Transactional;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.JAXBIntrospector;
import javax.xml.bind.Unmarshaller;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.cxf.configuration.jsse.TLSClientParameters;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.interceptor.LoggingInInterceptor;
import org.apache.cxf.interceptor.LoggingOutInterceptor;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.cxf.transports.http.configuration.HTTPClientPolicy;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.util.Assert;

import cz.tacr.elza.ElzaCoreTest;
import cz.tacr.elza.api.RegExternalSystemType;
import cz.tacr.elza.domain.RegExternalSystem;
import cz.tacr.elza.domain.RegRegisterType;
import cz.tacr.elza.interpi.service.InterpiService;
import cz.tacr.elza.interpi.ws.WssoapSoap;
import cz.tacr.elza.interpi.ws.wo.EntitaTyp;
import cz.tacr.elza.interpi.ws.wo.IdentifikaceTyp;
import cz.tacr.elza.interpi.ws.wo.KodovaneTyp;
import cz.tacr.elza.interpi.ws.wo.OznaceniTyp;
import cz.tacr.elza.interpi.ws.wo.PodtridaTyp;
import cz.tacr.elza.interpi.ws.wo.PopisTyp;
import cz.tacr.elza.interpi.ws.wo.SetTyp;
import cz.tacr.elza.interpi.ws.wo.SouradniceTyp;
import cz.tacr.elza.interpi.ws.wo.SouvisejiciTyp;
import cz.tacr.elza.interpi.ws.wo.StrukturaTyp;
import cz.tacr.elza.interpi.ws.wo.TitulTyp;
import cz.tacr.elza.interpi.ws.wo.TridaTyp;
import cz.tacr.elza.interpi.ws.wo.UdalostTyp;
import cz.tacr.elza.interpi.ws.wo.VyobrazeniTyp;
import cz.tacr.elza.interpi.ws.wo.ZarazeniTyp;
import cz.tacr.elza.interpi.ws.wo.ZaznamTyp;
import cz.tacr.elza.interpi.ws.wo.ZdrojTyp;
import cz.tacr.elza.repository.RegExternalSystemRepository;
import cz.tacr.elza.repository.RegisterTypeRepository;

/**
 * Testy na volání INTERPI.
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 23. 11. 2016
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = ElzaCoreTest.class)
public class InterpiTest {

    private String url = "https://195.113.132.114:443/csp/interpi/cust.interpi.ws.soap.cls";
    private String username = "tacr";
    private String password = "tacrinterpi2015";
    private String from = "1";
    private String to = "10";

    @Autowired
    private InterpiService interpiService;
    @Autowired
    private RegisterTypeRepository registerTypeRepository;
    @Autowired
    private RegExternalSystemRepository regExternalSystemRepository;

    private Integer systemId;

    @Before
    @Transactional
    public void setUp() {
        RegExternalSystem externalSystem = new RegExternalSystem();
        externalSystem.setCode("INTERPI");
        externalSystem.setName("INTERPI");
        externalSystem.setPassword(password);
        externalSystem.setType(RegExternalSystemType.INTERPI);
        externalSystem.setUrl(url);
        externalSystem.setUsername(username);

        systemId = regExternalSystemRepository.save(externalSystem).getExternalSystemId();
    }

    @After
    @Transactional
    public void clean() {
        regExternalSystemRepository.delete(systemId);
    }

    @Test
    public void findDataByServiceTest() {
        List<RegRegisterType> types = registerTypeRepository.findAll();
        Object data = interpiService.findByName("rod", types, systemId);

        System.out.println(data);
    }

    @Test
    public void findDataTest() throws JAXBException {
        WssoapSoap client = createClient();
        String query = "@attr 1=2055 'rod'";
        String data = client.findData(query, null, from, to, username, password);

        String query2 = "@and @attr 1=2051 @or @or 'g' 'd' @or @or 'r' 'o' @or 'u' @or 'k' 'p' @attr 1=2055 'rod'";
        String data2 = client.findData(query2, null, from, to, username, password);

        String query3 = " @and  @attr 1=2051  @or  @or  @or  @or  @or  @or 'g' 'd' 'r' 'o' 'u' 'k' 'p'  @attr 1=2055 'rod'";
        String data3 = client.findData(query3, null, from, to, username, password);

        System.out.println(data);
        System.out.println(data2);
        System.out.println(data3);

        SetTyp setTyp = unmarshallData(new StringReader(data), SetTyp.class);
        SetTyp setTyp2 = unmarshallData(new StringReader(data2), SetTyp.class);
        SetTyp setTyp3 = unmarshallData(new StringReader(data3), SetTyp.class);
    }

    @Test
    public void getOneRecordTest() {
        WssoapSoap client = createClient();
        String oneRecord = client.getOneRecord("n000382567", username, password);
        System.out.println(oneRecord);
    }

    @Test
    public void convertOneRecordSearchResultToJava() throws JAXBException {
        WssoapSoap client = createClient();
        String oneRecord = client.getOneRecord("n000382567", username, password);

        StringReader stringReader = new StringReader(oneRecord);
        SetTyp set = unmarshallData(stringReader, SetTyp.class);

        Assert.notEmpty(set.getEntita());
        Assert.isTrue(set.getEntita().size() == 1);

        List<EntitaTyp> entita = set.getEntita();
        for (EntitaTyp entitaTyp : entita) {
            entitaTyp.getContent().forEach(e -> {
                JAXBElement element = (JAXBElement) e;
                printElement2(0, element);
            });
        }
    }

    @Test
    public void convertFindDataSearchResultToJava() throws JAXBException {
        WssoapSoap client = createClient();
        String query = "@attr 1=2051 @and @or @or 'r' 'o' @or 'k' 'u' @attr 1=2055 'rod'";
        String data = client.findData(query, null, from, to, username, password);

        StringReader stringReader = new StringReader(data);
        SetTyp set = unmarshallData(stringReader, SetTyp.class);
        List<EntitaTyp> entita = set.getEntita();
        for (EntitaTyp entitaTyp : entita) {
            entitaTyp.getContent().forEach(e -> {
                JAXBElement element = (JAXBElement) e;
                printElement2(0, element);
            });
        }
    }

    private void printElement(final int depth, final JAXBElement element) {
        Object value = element.getValue();
        String localPart = element.getName().getLocalPart();
        switch (localPart) {
            case "trida":
                TridaTyp tridaTyp = getEntity(value, TridaTyp.class);
                break;
            case "podtrida":
                PodtridaTyp podtridaTyp = getEntity(value, PodtridaTyp.class);
                break;
            case "identifikace":
                IdentifikaceTyp identifikaceTyp = getEntity(value, IdentifikaceTyp.class);
                break;
            case "zaznam":
                ZaznamTyp zaznamTyp = getEntity(value, ZaznamTyp.class);
                break;
            case "preferovane_oznaceni":
                OznaceniTyp prefereovane = getEntity(value, OznaceniTyp.class);
                break;
            case "variantni_oznaceni":
                OznaceniTyp variantni = getEntity(value, OznaceniTyp.class);
                break;
            case "udalost":
                UdalostTyp udalostTyp = getEntity(value, UdalostTyp.class);
                break;
            case "pocatek_existence":
                UdalostTyp pocatek = getEntity(value, UdalostTyp.class);
                break;
            case "konec_existence":
                UdalostTyp konec = getEntity(value, UdalostTyp.class);
                break;
            case "zmena":
                UdalostTyp zmena = getEntity(value, UdalostTyp.class);
                break;
            case "popis":
                PopisTyp popisTyp = getEntity(value, PopisTyp.class);
                break;
            case "souradnice":
                SouradniceTyp souradniceTyp = getEntity(value, SouradniceTyp.class);
                break;
            case "titul":
                TitulTyp titulTyp = getEntity(value, TitulTyp.class);
                break;
            case "kodovane_udaje":
                KodovaneTyp kodovaneTyp = getEntity(value, KodovaneTyp.class);
                break;
            case "souvisejici_entita":
                SouvisejiciTyp souvisejiciTyp = getEntity(value, SouvisejiciTyp.class);
                break;
            case "hierarchicka_struktura":
                StrukturaTyp strukturaTyp = getEntity(value, StrukturaTyp.class);
                break;
            case "zarazeni":
                ZarazeniTyp zarazeniTyp = getEntity(value, ZarazeniTyp.class);
                break;
            case "vyobrazeni":
                VyobrazeniTyp vyobrazeniTyp = getEntity(value, VyobrazeniTyp.class);
                break;
            case "zdroj_informaci":
                ZdrojTyp zdrojTyp = getEntity(value, ZdrojTyp.class);
                break;
        }

        if (value instanceof ZaznamTyp) {
            ZaznamTyp zaznamTyp = getEntity(value, ZaznamTyp.class);
            zaznamTyp.getHistorieZaznamu();
            zaznamTyp.getJazyk();
            zaznamTyp.getPoznamka();
            zaznamTyp.getSouhlas();
            zaznamTyp.getStav();
        }
        StringBuilder tabs = new StringBuilder();
        IntStream.range(0, depth).forEach(i -> tabs.append("\t"));
        System.out.println(tabs.toString() + element.getName() + " " + element.getValue());
    }

    private void printElement2(final int depth, final JAXBElement element) {
        Object value = element.getValue();
        String localPart = element.getName().getLocalPart();
        Object val;
        switch (localPart) {
            case "trida":
                val = getEntity(value, TridaTyp.class);
                break;
            case "podtrida":
                val = getEntity(value, PodtridaTyp.class);
                break;
            case "identifikace":
                val = getEntity(value, IdentifikaceTyp.class);
                break;
            case "zaznam":
                val = getEntity(value, ZaznamTyp.class);
                break;
            case "preferovane_oznaceni":
                val = getEntity(value, OznaceniTyp.class);
                break;
            case "variantni_oznaceni":
                val = getEntity(value, OznaceniTyp.class);
                break;
            case "udalost":
                val = getEntity(value, UdalostTyp.class);
                break;
            case "pocatek_existence":
                val = getEntity(value, UdalostTyp.class);
                break;
            case "konec_existence":
                val = getEntity(value, UdalostTyp.class);
                break;
            case "zmena":
                val = getEntity(value, UdalostTyp.class);
                break;
            case "popis":
                val = getEntity(value, PopisTyp.class);
                break;
            case "souradnice":
                val = getEntity(value, SouradniceTyp.class);
                break;
            case "titul":
                val = getEntity(value, TitulTyp.class);
                break;
            case "kodovane_udaje":
                val = getEntity(value, KodovaneTyp.class);
                break;
            case "souvisejici_entita":
                val = getEntity(value, SouvisejiciTyp.class);
                break;
            case "hierarchicka_struktura":
                val = getEntity(value, StrukturaTyp.class);
                break;
            case "zarazeni":
                val = getEntity(value, ZarazeniTyp.class);
                break;
            case "vyobrazeni":
                val = getEntity(value, VyobrazeniTyp.class);
                break;
            case "zdroj_informaci":
                val = getEntity(value, ZdrojTyp.class);
                break;
            default:
                val = null;
        }

        System.out.println(ToStringBuilder.reflectionToString(val));
    }

    private <T> T getEntity(final Object value, final Class<T> cls) {
        return cls.cast(value);
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

        JAXBContext jaxbContext = JAXBContext.newInstance(cls);
        Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
        JAXBIntrospector jaxbIntrospector = jaxbContext.createJAXBIntrospector();

        Object unmarshal = unmarshaller.unmarshal(reader);


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
        factory.setAddress(url);

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
