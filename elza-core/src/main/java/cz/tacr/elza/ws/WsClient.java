package cz.tacr.elza.ws;

import cz.tacr.elza.domain.ArrDao;
import cz.tacr.elza.domain.ArrDaoLinkRequest;
import cz.tacr.elza.domain.ArrDaoRequest;
import cz.tacr.elza.domain.ArrDaoRequestDao;
import cz.tacr.elza.domain.ArrDigitalRepository;
import cz.tacr.elza.domain.ArrDigitizationFrontdesk;
import cz.tacr.elza.domain.ArrDigitizationRequest;
import cz.tacr.elza.domain.ArrDigitizationRequestNode;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.repository.DaoDigitizationRequestNodeRepository;
import cz.tacr.elza.repository.DaoRequestDaoRepository;
import cz.tacr.elza.ws.dao_service.v1.DaoNotifications;
import cz.tacr.elza.ws.dao_service.v1.DaoRequests;
import cz.tacr.elza.ws.dao_service.v1.DaoServiceException;
import cz.tacr.elza.ws.digitization.v1.DigitizationFrontdesk;
import cz.tacr.elza.ws.digitization.v1.DigitizationServiceException;
import cz.tacr.elza.ws.types.v1.DaoIdentifiers;
import cz.tacr.elza.ws.types.v1.DestructionRequest;
import cz.tacr.elza.ws.types.v1.Did;
import cz.tacr.elza.ws.types.v1.DigitizationRequest;
import cz.tacr.elza.ws.types.v1.Materials;
import cz.tacr.elza.ws.types.v1.OnDaoLinked;
import cz.tacr.elza.ws.types.v1.OnDaoUnlinked;
import cz.tacr.elza.ws.types.v1.TransferRequest;
import org.apache.cxf.feature.FastInfosetFeature;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.xml.bind.helpers.DefaultValidationEventHandler;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author <a href="mailto:martin.lebeda@marbes.cz">Martin Lebeda</a>
 *         Date: 22.12.16
 */
@Service
public class WsClient {

    @Autowired
    private DaoRequestDaoRepository daoRequestDaoRepository;

    @Autowired
    private DaoDigitizationRequestNodeRepository daoDigitizationRequestNodeRepository;

    /**
     * Vytvoří JaxWs klienta webové služby a vrátí proxy rozhraní.
     *
     * @param wsInterface třída rozhraní webové služby
     * @param url         adresa webové služby (bez wsdl)
     * @return proxy rozhraní webové služby
     */
    private static <T> T getJaxWsRemoteInterface(Class<T> wsInterface, String url, String loginName, String password) {

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
            factory.setUsername(loginName);
            factory.setPassword(password);

            Object service = factory.create();


            return (T) service;

        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    public String postDestructionRequest(ArrDaoRequest arrDaoRequest) {
        final ArrDigitalRepository digitalRepository = arrDaoRequest.getDigitalRepository();
        final DaoRequests remoteInterface = getDaoRequests(digitalRepository);

        try {
            final DestructionRequest destructionRequest = new DestructionRequest();
            destructionRequest.setIdentifier(arrDaoRequest.getCode());
            destructionRequest.setDescription(arrDaoRequest.getDescription());
            destructionRequest.setSystemIdentifier(arrDaoRequest.getRequestId().toString()); // TODO Lebeda - co nasetovat jako system identifier??

            final DaoIdentifiers daoIdentifiers = new DaoIdentifiers();
            final List<ArrDaoRequestDao> daos = daoRequestDaoRepository.findByDaoRequest(arrDaoRequest);
            daoIdentifiers.getIdentifier().addAll(daos.stream().map(ArrDaoRequestDao::getDao).map(ArrDao::getCode).collect(Collectors.toList()));
            destructionRequest.setDaoIdentifiers(daoIdentifiers);

            return remoteInterface.postDestructionRequest(destructionRequest);
        } catch (DaoServiceException e) {
            throw new SystemException(e);
        }
    }

    public String postTransferRequest(ArrDaoRequest arrDaoRequest) {
        final ArrDigitalRepository digitalRepository = arrDaoRequest.getDigitalRepository();
        final DaoRequests remoteInterface = getDaoRequests(digitalRepository);

        try {
            final TransferRequest transferRequest = new TransferRequest();
            transferRequest.setIdentifier(arrDaoRequest.getCode());
            transferRequest.setDescription(arrDaoRequest.getDescription());
            transferRequest.setSystemIdentifier(arrDaoRequest.getRequestId().toString());

            final DaoIdentifiers daoIdentifiers = new DaoIdentifiers();
            final List<ArrDaoRequestDao> daos = daoRequestDaoRepository.findByDaoRequest(arrDaoRequest);
            daoIdentifiers.getIdentifier().addAll(daos.stream().map(ArrDaoRequestDao::getDao).map(ArrDao::getCode).collect(Collectors.toList()));
            transferRequest.setDaoIdentifiers(daoIdentifiers);

            return remoteInterface.postTransferRequest(transferRequest);
        } catch (DaoServiceException e) {
            throw new SystemException(e);
        }
    }

    private static DaoRequests getDaoRequests(ArrDigitalRepository digitalRepository) {
        final String url = digitalRepository.getUrl();
        final String username = digitalRepository.getUsername();
        final String password = digitalRepository.getPassword();
        return getJaxWsRemoteInterface(DaoRequests.class, url, username, password);
    }

    private static DaoNotifications getDaoNotifications(ArrDigitalRepository digitalRepository) {
        final String url = digitalRepository.getUrl();
        final String username = digitalRepository.getUsername();
        final String password = digitalRepository.getPassword();
        return getJaxWsRemoteInterface(DaoNotifications.class, url, username, password);
    }

    private static DigitizationFrontdesk getDigitizationFrontdesk(ArrDigitizationFrontdesk digitalRepository) {
        final String url = digitalRepository.getUrl();
        final String username = digitalRepository.getUsername();
        final String password = digitalRepository.getPassword();
        return getJaxWsRemoteInterface(DigitizationFrontdesk.class, url, username, password);
    }

    public String postRequest(ArrDigitizationRequest arrDigitizationRequest) {
        final ArrDigitizationFrontdesk digitalRepository = arrDigitizationRequest.getDigitizationFrontdesk();
        final DigitizationFrontdesk remoteInterface = getDigitizationFrontdesk(digitalRepository);

        try {
            final DigitizationRequest digitizationRequest = new DigitizationRequest();
            digitizationRequest.setIdentifier(arrDigitizationRequest.getCode());
            digitizationRequest.setDescription(arrDigitizationRequest.getDescription());
            digitizationRequest.setSystemIdentifier(arrDigitizationRequest.getRequestId().toString());

            final Materials materials = new Materials();
            final List<ArrDigitizationRequestNode> digitizationRequestNodes = daoDigitizationRequestNodeRepository.findByDigitizationRequest(arrDigitizationRequest);
            for (ArrDigitizationRequestNode arrDigitizationRequestNode : digitizationRequestNodes) {
                final Did did = new Did(); // TODO Lebeda - jak se dostanu ke správné hodnotě
                materials.getDid().add(did);
            }
            digitizationRequest.setMaterials(materials);

            return remoteInterface.postRequest(digitizationRequest);
        } catch (DigitizationServiceException e) {
            throw new SystemException(e);
        }
    }

    public void onDaoLinked(ArrDaoLinkRequest arrDaoLinkRequest) {
        final ArrDigitalRepository digitalRepository = arrDaoLinkRequest.getDigitalRepository();
        final DaoNotifications remoteInterface = getDaoNotifications(digitalRepository);

        try {
            final OnDaoLinked daoLinked = new OnDaoLinked();
            daoLinked.setDaoIdentifier(arrDaoLinkRequest.getDao().getCode());
            daoLinked.setSystemIdentifier(arrDaoLinkRequest.getRequestId().toString());
            final Did did = new Did();  // TODO Lebeda - jak se dostanu ke správné hodnotě
//                    did.setIdentifier();
//                    did.setAbstract();
//                    did.setUnitdatestructured();
            daoLinked.setDid(did);

            remoteInterface.onDaoLinked(daoLinked);
        } catch (DaoServiceException e) {
            throw new SystemException(e);
        }
    }

    public void onDaoUnlinked(ArrDaoLinkRequest arrDaoLinkRequest) {
        final ArrDigitalRepository digitalRepository = arrDaoLinkRequest.getDigitalRepository();
        final DaoNotifications remoteInterface = getDaoNotifications(digitalRepository);

        try {
            final OnDaoUnlinked daoUnlinked = new OnDaoUnlinked();
            daoUnlinked.setDaoIdentifier(arrDaoLinkRequest.getDao().getCode());
            daoUnlinked.setSystemIdentifier(arrDaoLinkRequest.getRequestId().toString());
            remoteInterface.onDaoUnlinked(daoUnlinked);
        } catch (DaoServiceException e) {
            throw new SystemException(e);
        }
    }
}
