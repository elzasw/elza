package cz.tacr.elza.other;

import java.util.Arrays;
import java.util.List;

import javax.net.ssl.TrustManager;
import javax.transaction.Transactional;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.cxf.configuration.jsse.TLSClientParameters;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.interceptor.LoggingInInterceptor;
import org.apache.cxf.interceptor.LoggingOutInterceptor;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.cxf.transports.http.configuration.HTTPClientPolicy;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.util.Assert;

import cz.tacr.elza.ElzaCoreTest;
import cz.tacr.elza.api.RegExternalSystemType;
import cz.tacr.elza.controller.AbstractControllerTest;
import cz.tacr.elza.controller.vo.RecordImportVO;
import cz.tacr.elza.controller.vo.RegRecordVO;
import cz.tacr.elza.domain.RegExternalSystem;
import cz.tacr.elza.domain.RegScope;
import cz.tacr.elza.interpi.service.InterpiService;
import cz.tacr.elza.interpi.service.pqf.AttributeType;
import cz.tacr.elza.interpi.service.pqf.ConditionType;
import cz.tacr.elza.interpi.service.vo.ConditionVO;
import cz.tacr.elza.interpi.service.vo.ExternalRecordVO;
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
import cz.tacr.elza.security.UserDetail;
import cz.tacr.elza.utils.NoCheckTrustManager;
import cz.tacr.elza.utils.XmlUtils;

/**
 * Testy na volání INTERPI.
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 23. 11. 2016
 */
@Ignore
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = ElzaCoreTest.class)
public class InterpiTest extends AbstractControllerTest {

    private String url = "https://195.113.132.114:443/csp/interpi/cust.interpi.ws.soap.cls";
//    private String url = "https://localhost:8088/mockwssoapSoap";
    private String username = "tacr";
    private String password = "tacrinterpi2015";
    private String from = "1";
    private String to = "10";

    @Autowired
    private InterpiService interpiService;
    @Autowired
    private RegExternalSystemRepository regExternalSystemRepository;

    private Integer systemId;

    @Override
    @Before
    @Transactional
    public void setUp() throws Exception {
        super.setUp();
        RegExternalSystem externalSystem = new RegExternalSystem();
        externalSystem.setCode("INTERPI");
        externalSystem.setName("INTERPI");
        externalSystem.setPassword(password);
        externalSystem.setType(RegExternalSystemType.INTERPI);
        externalSystem.setUrl(url);
        externalSystem.setUsername(username);

        systemId = regExternalSystemRepository.save(externalSystem).getExternalSystemId();
    }

    public void authenticate() {
        UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken("admin", "admin");
        token.setDetails(new UserDetail("admin"));
        SecurityContextHolder.getContext().setAuthentication(token);
    }

    @Test
    public void importByServiceTest() {
        RegScope scope = scopeRepository.findByCode("GLOBAL");

        RecordImportVO importVO = new RecordImportVO();
        importVO.setInterpiRecordId("0000216");
        importVO.setScopeId(scope.getScopeId());
        importVO.setSystemId(systemId);

        RegRecordVO regRecord = post(spec -> spec.body(importVO), "/api/registry/interpi/import").as(RegRecordVO.class);
        RegRecordVO record = getRecord(regRecord.getId());
        Assert.isTrue(regRecord.getRecord().equals(record.getRecord()));

        RegRecordVO regRecordUpdate = put(spec -> spec.pathParam("recordId", record.getId()).body(importVO), "/api/registry/interpi/import/{recordId}").as(RegRecordVO.class);
        Assert.isTrue(regRecordUpdate.getId().equals(record.getId()));
    }

    @Test
    public void findDataByServiceTest() {
        List<ConditionVO> conditions = Arrays.asList(new ConditionVO(ConditionType.AND, AttributeType.ALL_NAMES, "rod"),
                new ConditionVO(ConditionType.AND, AttributeType.PREFFERED_NAME, "jan"));
        List<ExternalRecordVO> data = interpiService.findRecords(true, conditions, 500, systemId);

        for (ExternalRecordVO externalRecordVO : data) {
            printRecord(externalRecordVO);
        }
    }

    private void printRecord(final ExternalRecordVO externalRecordVO) {
        System.out.println(externalRecordVO.getDetail());
        System.out.println(externalRecordVO.getName());
        System.out.println(externalRecordVO.getRecordId());
    }

    @Test
    public void findOneByServiceTest() {
        ExternalRecordVO externalRecordVO = interpiService.getRecordById("0000216", systemId);

        printRecord(externalRecordVO);
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

        SetTyp setTyp = XmlUtils.unmarshallDataWithIntrospector(data, SetTyp.class);
        SetTyp setTyp2 = XmlUtils.unmarshallDataWithIntrospector(data2, SetTyp.class);
        SetTyp setTyp3 = XmlUtils.unmarshallDataWithIntrospector(data3, SetTyp.class);
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

        SetTyp set = XmlUtils.unmarshallDataWithIntrospector(oneRecord, SetTyp.class);

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

        SetTyp set = XmlUtils.unmarshallDataWithIntrospector(data, SetTyp.class);
        List<EntitaTyp> entita = set.getEntita();
        for (EntitaTyp entitaTyp : entita) {
            entitaTyp.getContent().forEach(e -> {
                JAXBElement element = (JAXBElement) e;
                printElement2(0, element);
            });
        }
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
}
