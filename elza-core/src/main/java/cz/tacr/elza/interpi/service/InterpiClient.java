package cz.tacr.elza.interpi.service;

import java.util.List;

import javax.net.ssl.TrustManager;

import cz.tacr.elza.domain.ApExternalSystem;
import org.apache.cxf.configuration.jsse.TLSClientParameters;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.interceptor.LoggingInInterceptor;
import org.apache.cxf.interceptor.LoggingOutInterceptor;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.cxf.transports.http.configuration.HTTPClientPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import cz.tacr.elza.common.XmlUtils;
import cz.tacr.elza.common.security.NoCheckTrustManager;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.ExternalCode;
import cz.tacr.elza.interpi.ws.WssoapSoap;
import cz.tacr.elza.interpi.ws.wo.EntitaTyp;
import cz.tacr.elza.interpi.ws.wo.SetTyp;

/**
 * Klient pro WS INTERPI.
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 20. 12. 2016
 */
@Service
public class InterpiClient {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    /** Stránkování. */
    private static final String FROM = "1";
    private static final String TO = "500";

    /** Prefix pro chybovou hlášku. */
    private static final String ERROR_PREFIX = "ERR";

    public List<EntitaTyp> findRecords(final String query, final Integer count, final ApExternalSystem apExternalSystem) {
        WssoapSoap client = createClient(apExternalSystem);

        Integer maxCount = count == null ? Integer.valueOf(TO) : count;

        logger.info("Vyhledávání v interpi: " + query);
        long start = System.currentTimeMillis();
        String searchResult = client.findData(query, null, FROM, maxCount.toString(), apExternalSystem.getUsername(), apExternalSystem.getPassword());
        long end = System.currentTimeMillis();
        logger.debug("Dotaz do INTERPI trval " + (end - start) + " ms.");

        SetTyp setTyp = unmarshall(searchResult);

        List<EntitaTyp> records = setTyp.getEntita();
        int searchResultCount = records.size() > maxCount ? maxCount : records.size();

        return records.subList(0, searchResultCount); // INTERPI neumí stránkovat
    }

    public EntitaTyp findOneRecord(final String id, final ApExternalSystem apExternalSystem) {
        WssoapSoap client = createClient(apExternalSystem);

        logger.info("Načítání záznamu s identifikátorem " + id + " z interpi.");
        long start = System.currentTimeMillis();
        String oneRecord = client.getOneRecord(id, apExternalSystem.getUsername(), apExternalSystem.getPassword());
        long end = System.currentTimeMillis();
        logger.debug("Dotaz do INTERPI trval " + (end - start) + " ms.");

        if (oneRecord.startsWith(ERROR_PREFIX)) {
             throw new SystemException("Nastala chyba " + oneRecord + " při hledání záznamu s identifikátorem " + id + " v systému " + apExternalSystem, ExternalCode.EXTERNAL_SYSTEM_ERROR);
        }
        SetTyp setTyp = unmarshall(oneRecord);

        if (setTyp.getEntita().isEmpty()) {
            throw new SystemException("Záznam s identifikátorem " + id + " nebyl nalezen v systému " + apExternalSystem);
        }

        return setTyp.getEntita().iterator().next();
    }

    private WssoapSoap createClient(final ApExternalSystem apExternalSystem) {
        return createClient(apExternalSystem.getUrl(), WssoapSoap.class);
    }

    private SetTyp unmarshall(final String oneRecord) {
        return XmlUtils.unmarshall(oneRecord, SetTyp.class);
    }

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
