package cz.tacr.elza.interpi.service;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.TrustManager;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.JAXBIntrospector;
import javax.xml.bind.Unmarshaller;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.cxf.configuration.jsse.TLSClientParameters;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.interceptor.LoggingInInterceptor;
import org.apache.cxf.interceptor.LoggingOutInterceptor;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.cxf.transports.http.configuration.HTTPClientPolicy;
import org.junit.Assert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import cz.tacr.elza.domain.RegExternalSystem;
import cz.tacr.elza.domain.RegRegisterType;
import cz.tacr.elza.interpi.ws.WssoapSoap;
import cz.tacr.elza.interpi.ws.wo.SetTyp;
import cz.tacr.elza.repository.RegExternalSystemRepository;
import cz.tacr.elza.utils.NoCheckTrustManager;

/**
 *
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 27. 11. 2016
 */
@Service
public class InterpiService {

    /** Stránkování. */
    private static final String FROM = "1";
    private static final String TO = "500";

    /** Atributy používané při hledání. */
    private static final String TYPE_ATT_ID = " @attr 1=2051 ";
    private static final String PREFFERED_NAME_ATT_ID = " @attr 1=2054 ";
    private static final String NAME_ATT_ID = " @attr 1=2055 ";

    private static final String AND = " @and ";
    private static final String OR = " @or ";

    /** Typy entit v INTERPI. */
    private static final String PERSON = "'o'";
    private static final String DYNASTY = "'r'";
    private static final String PARTY_GROUP = "'k'";
    private static final String GEO = "'g'";
    private static final String EVENT = "'u'";
    private static final String ARTWORK = "'d'";
    private static final String TERM = "'p'";

    @Autowired
    private RegExternalSystemRepository regExternalSystemRepository;

    /** Hledání podle preferovaného jména. */
    public Object findByPrefferedName(final String text, final List<RegRegisterType> types, final Integer systemId) {
        return find(text, types, systemId, PREFFERED_NAME_ATT_ID);
    }

    /** Hledání podle všech typů jmen. */
    public Object findByName(final String text, final List<RegRegisterType> types, final Integer systemId) {
        return find(text, types, systemId, NAME_ATT_ID);
    }

    /** Načtení konkrétního záznamu podle id. */
    public Object getOne(final String id, final Integer systemId) {
        Assert.assertNotNull(id);
        Assert.assertNotNull(systemId);

        RegExternalSystem interpiSystem = regExternalSystemRepository.findOne(systemId);
        WssoapSoap client = createClient(interpiSystem.getUrl());

        String oneRecord = client.getOneRecord(id, interpiSystem.getUsername(), interpiSystem.getPassword());
        SetTyp setTyp = unmarshallData(oneRecord);

        if (setTyp.getEntita().isEmpty()) {
            throw new IllegalStateException("Záznam s identifikátorem " + id + " nebyl nalezen v systému " + interpiSystem);
        }

        return setTyp.getEntita().iterator().next();
    }

    private Object find(final String text, final List<RegRegisterType> types, final Integer systemId, final String attId) {
        Assert.assertNotNull(text);
        Assert.assertNotNull(systemId);

        RegExternalSystem interpiSystem = regExternalSystemRepository.findOne(systemId);
        WssoapSoap client = createClient(interpiSystem.getUrl());

        String typeQuery = createTypeQuery(types);
        String query = AND + typeQuery + attId + "'" + text + "'";
        String searchResult = client.findData(query, null, FROM, TO, interpiSystem.getUsername(), interpiSystem.getPassword());

        SetTyp setTyp = unmarshallData(searchResult);

        return setTyp.getEntita();
    }

    private String createTypeQuery(final List<RegRegisterType> types) {
        if (CollectionUtils.isEmpty(types)) {
            return StringUtils.EMPTY;
        }

        List<String> conditions = convertTypes(types);

        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < conditions.size() - 1; i++) {
            sb.append(OR);
        }

        for (String condition : conditions) {
            sb.append(condition);
            sb.append(" ");
        }
        return TYPE_ATT_ID + sb.toString();
    }

    private List<String> convertTypes(final List<RegRegisterType> types) {
        List<String> conditions = new ArrayList<>(types.size());conditions.add(DYNASTY);
        for (RegRegisterType registerType : types) {
            RegRegisterType parent = getParent(registerType);
            switch (parent.getCode()) {
                case "PARTY_GROUP":
                    if (!conditions.contains(PARTY_GROUP)) {
                        conditions.add(PARTY_GROUP);
                    }
                    break;
                case "PERSON":
                    if (!conditions.contains(PERSON)) {
                        conditions.add(PERSON);
                    }
                    break;
                case "FAMILY":
                    if (!conditions.contains(DYNASTY)) {
                        conditions.add(DYNASTY);
                    }
                    break;
                case "GEO":
                    if (!conditions.contains(GEO)) {
                        conditions.add(GEO);
                    }
                    break;
                case "EVENT":
                    if (!conditions.contains(EVENT)) {
                        conditions.add(EVENT);
                    }
                    break;
                case "ARTWORK":
                    if (!conditions.contains(ARTWORK)) {
                        conditions.add(ARTWORK);
                    }
                    break;
                case "TERM":
                    if (!conditions.contains(TERM)) {
                        conditions.add(TERM);
                    }
                    break;
                default:
                    throw new IllegalStateException("Neznámý typ rejstříku " + parent.getCode());
            }
        }

        return conditions;
    }

    private RegRegisterType getParent(final RegRegisterType registerType) {
        RegRegisterType parent = registerType;
        while (parent.getParentRegisterType() != null) {
            parent = parent.getParentRegisterType();
        }
        return parent;
    }

    /**
     * Převede data z xml do objektu.
     */
    public SetTyp unmarshallData(final String searchResult) {
        if (StringUtils.isBlank(searchResult)) {
            return null;
        }

        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(SetTyp.class);
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            JAXBIntrospector jaxbIntrospector = jaxbContext.createJAXBIntrospector();

            StringReader reader = new StringReader(searchResult);
            Object unmarshal = unmarshaller.unmarshal(reader);

            return (SetTyp) jaxbIntrospector.getValue(unmarshal);
        } catch (JAXBException e) {
            throw new IllegalStateException("Chyba při převodu dat z xml.", e); // TODO [vanek] udělat podle systému výjimek
        }
    }

    private WssoapSoap createClient(final String url) {
        JaxWsProxyFactoryBean factory = new JaxWsProxyFactoryBean();
        factory.setServiceClass(WssoapSoap.class);
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

        WssoapSoap client = (WssoapSoap) factory.create();
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
