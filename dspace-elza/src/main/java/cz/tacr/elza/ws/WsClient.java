package cz.tacr.elza.ws;

import cz.tacr.elza.ws.core.v1.DaoRequestsService;
import cz.tacr.elza.ws.dao_service.v1.DaoServiceException;
import cz.tacr.elza.ws.types.v1.RequestRevoked;
import org.apache.commons.lang.StringUtils;
import org.apache.cxf.feature.FastInfosetFeature;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import javax.annotation.Nullable;
import javax.ws.rs.ProcessingException;
import javax.xml.bind.helpers.DefaultValidationEventHandler;
import java.util.HashMap;

/**
 * Created by Marbes Consulting
 * ludek.cacha@marbes.cz / 02.05.2019.
 */
@Service
public class WsClient {

    private static final Logger logger = LoggerFactory.getLogger(WsClient.class);
    private ConfigurationService configurationService = DSpaceServicesFactory.getInstance().getConfigurationService();

    /**
     * Vytvoří JaxWs klienta webové služby a vrátí proxy rozhraní.
     *
     * @param wsInterface třída rozhraní webové služby
     * @param url         adresa webové služby (bez wsdl)
     * @param loginName   Nepovinné uživatelské jméno pro přihlášení k webové službě
     * @param password    Nepovinné heslo pro přihlášení k webové službě
     * @return proxy rozhraní webové služby
     */
    private static <T> T getJaxWsRemoteInterface(Class<T> wsInterface, String url, @Nullable String loginName, @Nullable String password) {
        Assert.notNull(wsInterface, "Nebyla zadána třída webové služby.");
        Assert.hasText(url, "Nebylo zadáno url pro volání webové služby.");

        try {
            JaxWsProxyFactoryBean factory = new JaxWsProxyFactoryBean();
            factory.setServiceClass(wsInterface);
            factory.setAddress(url);
            factory.getFeatures().add(new FastInfosetFeature());
            //factory.setBindingId(SOAPBinding.SOAP12HTTP_MTOM_BINDING);

            HashMap<String, Object> properties = new HashMap<>();
            properties.put("jaxb-validation-event-handler", new DefaultValidationEventHandler());
            factory.setProperties(properties);

            //prihlaseni ke vzdalene sluzbe
            if (StringUtils.isNotBlank(loginName)) {
                factory.setUsername(loginName);
            }
            if (StringUtils.isNotBlank(password)) {
                factory.setPassword(password);
            }

            Object service = factory.create();


            return (T) service;

        } catch (Exception e) {
            throw new ProcessingException(e.getMessage(), e);
        }
    }

    public void destructionRequestRevoked(final RequestRevoked requestRevoked) {
        final DaoRequestsService remoteInterface = getDaoRequests();
        try {
            remoteInterface.destructionRequestRevoked(requestRevoked);
        } catch (DaoServiceException e) {
            logger.error("Fail in call remote webservice.", e);
            throw new ProcessingException(e);
        }
    }

    public void destructionRequestFinished(final String requestIdentifier) {
        final DaoRequestsService remoteInterface = getDaoRequests();
        try {
            remoteInterface.destructionRequestFinished(requestIdentifier);
        } catch (DaoServiceException e) {
            logger.error("Fail in call remote webservice.", e);
            throw new ProcessingException(e);
        }
    }

    public void transferRequestRevoked(final RequestRevoked requestRevoked) {
        final DaoRequestsService remoteInterface = getDaoRequests();
        try {
            remoteInterface.transferRequestRevoked(requestRevoked);
        } catch (DaoServiceException e) {
            logger.error("Fail in call remote webservice.", e);
            throw new ProcessingException(e);
        }
    }

    public void transferRequestFinished(final String requestIdentifier) {
        final DaoRequestsService remoteInterface = getDaoRequests();
        try {
            remoteInterface.transferRequestFinished(requestIdentifier);
        } catch (DaoServiceException e) {
            logger.error("Fail in call remote webservice.", e);
            throw new ProcessingException(e);
        }
    }

    private DaoRequestsService getDaoRequests() {
        final String url = configurationService.getProperty("elza.base.url") + "DaoRequestsService";
        final String username = configurationService.getProperty("elza.base.username");
        final String password = configurationService.getProperty("elza.base.password");
        return getJaxWsRemoteInterface(DaoRequestsService.class, url, username, password);
    }


}
