
/**
 * Please modify this class to meet your needs
 * This class is not complete
 */

package cz.tacr.elza.ws.core.v1;

import java.util.stream.Collectors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import cz.tacr.elza.domain.ArrRequest;
import cz.tacr.elza.repository.RequestRepository;
import cz.tacr.elza.service.IEventNotificationService;
import cz.tacr.elza.service.RequestService;
import cz.tacr.elza.service.eventnotification.events.EventIdNodeIdInVersion;
import cz.tacr.elza.service.eventnotification.events.EventType;
import cz.tacr.elza.ws.types.v1.DigitizationRequestResult;
import cz.tacr.elza.ws.types.v1.RequestRevoked;


@Component
@jakarta.jws.WebService(
                      serviceName = "CoreService",
                      portName = "DaoDigitizationService",
                      targetNamespace = "http://elza.tacr.cz/ws/core/v1",
                      endpointInterface = "cz.tacr.elza.ws.core.v1.DaoDigitizationService")

public class DaoDigitizationServiceImpl implements DaoDigitizationService {

    private Log logger = LogFactory.getLog(this.getClass());

    @Autowired
    private RequestService requestService;

    @Autowired
    private RequestRepository requestRepository;

    @Autowired
    private DaoCoreServiceImpl daoCoreService;

    @Autowired
    private IEventNotificationService eventNotificationService;

    /*
     * @see cz.tacr.elza.ws.core.v1.DaoDigitizationService#digitizationRequestFinished(cz.tacr.elza.ws.types.v1.DigitizationRequestResult digitizationRequestResult)*
     */
    @Override
	@Transactional
    public void digitizationRequestFinished(final DigitizationRequestResult digitizationRequestResult) throws CoreServiceException   {
        try {
            logger.info("Executing operation digitizationRequestFinished for code=" + digitizationRequestResult.getIdentifier());

            ArrRequest request = requestRepository.findOneByCode(digitizationRequestResult.getIdentifier());
            requestService.setRequestState(request, ArrRequest.State.SENT, ArrRequest.State.PROCESSED);

            // import
             daoCoreService._import(digitizationRequestResult.getDaoImport());

            sendNotification(request);

            logger.info("Finished operation digitizationRequestFinished");
        } catch (Exception e) {
            logger.error("Fail operation digitizationRequestFinished", e);
            throw new CoreServiceException(e.getMessage(), e);
        }
    }

    /*
     * @see cz.tacr.elza.ws.core.v1.DaoDigitizationService#digitizationRequestRevoked(cz.tacr.elza.ws.types.v1.RequestRevoked requestRevoked)*
     */
    @Override
	@Transactional
    public void digitizationRequestRevoked(final RequestRevoked requestRevoked) throws CoreServiceException   {
        try {
            logger.info("Executing operation digitizationRequestRevoked for code=" + requestRevoked.getIdentifier());

            ArrRequest request = requestRepository.findOneByCode(requestRevoked.getIdentifier());
            if (request != null) {
                requestService.setRequestState(request, ArrRequest.State.SENT, ArrRequest.State.REJECTED);
                request.setRejectReason(requestRevoked.getDescription());
                requestRepository.save(request);

                sendNotification(request);

                logger.info("Finished operation digitizationRequestRevoked.");
            } else {
                logger.warn("Request for code=" + requestRevoked.getIdentifier() + " was not found." );
            }
        } catch (Exception e) {
            logger.error("Fail operation digitizationRequestRevoked", e);
            throw new CoreServiceException(e.getMessage(), e);
        }
    }

    private void sendNotification(ArrRequest request) {
        Integer version = request.getFund().getVersions().stream().filter(i -> i.getLockChange() == null).collect(Collectors.toList()).get(0).getFundVersionId();
        EventIdNodeIdInVersion event = new EventIdNodeIdInVersion(EventType.REQUEST_CHANGE, version, request.getRequestId(), null);
        eventNotificationService.publishEvent(event);
    }

}
