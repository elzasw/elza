package cz.tacr.elza.ws;

import java.util.HashMap;

import org.apache.commons.lang3.StringUtils;
import org.apache.cxf.feature.FastInfosetFeature;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import cz.tacr.elza.domain.ArrDigitalRepository;
import cz.tacr.elza.domain.ArrDigitizationFrontdesk;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.ws.dao_service.v1.DaoNotifications;
import cz.tacr.elza.ws.dao_service.v1.DaoRequests;
import cz.tacr.elza.ws.dao_service.v1.DaoServiceException;
import cz.tacr.elza.ws.digitization.v1.DigitizationFrontdesk;
import cz.tacr.elza.ws.digitization.v1.DigitizationServiceException;
import cz.tacr.elza.ws.types.v1.DaosSyncRequest;
import cz.tacr.elza.ws.types.v1.DaosSyncResponse;
import cz.tacr.elza.ws.types.v1.DestructionRequest;
import cz.tacr.elza.ws.types.v1.DigitizationRequest;
import cz.tacr.elza.ws.types.v1.OnDaoLinked;
import cz.tacr.elza.ws.types.v1.OnDaoUnlinked;
import cz.tacr.elza.ws.types.v1.TransferRequest;
import jakarta.annotation.Nullable;
import jakarta.xml.bind.helpers.DefaultValidationEventHandler;

/**
 * @author <a href="mailto:martin.lebeda@marbes.cz">Martin Lebeda</a>
 *         Date: 22.12.16
 */
@Service
public class WsClient {

    private static final Logger logger = LoggerFactory.getLogger(WsClient.class);

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

            // Proc to tu je?
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
            throw new SystemException(e.getMessage(), e);
        }
    }

    public String postDestructionRequest(final DestructionRequest destructionRequest,
                                         final ArrDigitalRepository digitalRepository) {
        final DaoRequests remoteInterface = getDaoRequests(digitalRepository);
        try {
            return remoteInterface.postDestructionRequest(destructionRequest);
        } catch (DaoServiceException e) {
            logger.error("Fail in call remote webservice.", e);
            throw new SystemException(e);
        }
    }

    public String postTransferRequest(final TransferRequest transferRequest,
                                      final ArrDigitalRepository digitalRepository) {
        final DaoRequests remoteInterface = getDaoRequests(digitalRepository);
        try {
            return remoteInterface.postTransferRequest(transferRequest);
        } catch (DaoServiceException e) {
            logger.error("Fail in call remote webservice.", e);
            throw new SystemException(e);
        }
    }

    public DaosSyncResponse syncDaos(final DaosSyncRequest daosSyncRequest,
                                     final ArrDigitalRepository digitalRepository) {
        final DaoRequests remoteInterface = getDaoRequests(digitalRepository);
        try {
            return remoteInterface.syncDaos(daosSyncRequest);
        } catch (DaoServiceException e) {
            logger.error("Fail in call remote webservice.", e);
            throw new SystemException(e);
        }
    }

    private static DaoRequests getDaoRequests(ArrDigitalRepository digitalRepository) {
        final String url = digitalRepository.getUrl() + "DaoRequests";
        final String username = digitalRepository.getUsername();
        final String password = digitalRepository.getPassword();
        return getJaxWsRemoteInterface(DaoRequests.class, url, username, password);
    }

    private static DaoNotifications getDaoNotifications(ArrDigitalRepository digitalRepository) {
        final String url = digitalRepository.getUrl() + "DaoNotifications";
        final String username = digitalRepository.getUsername();
        final String password = digitalRepository.getPassword();
        return getJaxWsRemoteInterface(DaoNotifications.class, url, username, password);
    }

    private static DigitizationFrontdesk getDigitizationFrontdesk(ArrDigitizationFrontdesk digitalRepository) {
        final String url = digitalRepository.getUrl() + "DigitizationFrontdesk";
        final String username = digitalRepository.getUsername();
        final String password = digitalRepository.getPassword();
        return getJaxWsRemoteInterface(DigitizationFrontdesk.class, url, username, password);
    }

    public String postRequest(final DigitizationRequest digitizationRequest,
                              final ArrDigitizationFrontdesk digitizationFrontdesk) {
        final DigitizationFrontdesk remoteInterface = getDigitizationFrontdesk(digitizationFrontdesk);
        try {
            return remoteInterface.postRequest(digitizationRequest);
        } catch (DigitizationServiceException e) {
            logger.error("Fail in call remote webservice.", e);
            throw new SystemException(e);
        }
    }

    public void onDaoLinked(final OnDaoLinked daoLinked,
                            final ArrDigitalRepository digitalRepository) {
        final DaoNotifications remoteInterface = getDaoNotifications(digitalRepository);
        try {
            remoteInterface.onDaoLinked(daoLinked);
        } catch (DaoServiceException e) {
            logger.error("Fail in call remote webservice.", e);
            throw new SystemException(e);
        }
    }

    public void onDaoUnlinked(final OnDaoUnlinked daoUnlinked,
                              final ArrDigitalRepository digitalRepository) {
        final DaoNotifications remoteInterface = getDaoNotifications(digitalRepository);
        try {
            remoteInterface.onDaoUnlinked(daoUnlinked);
        } catch (DaoServiceException e) {
            logger.error("Fail in call remote webservice.", e);
            throw new SystemException(e);
        }
    }
}
