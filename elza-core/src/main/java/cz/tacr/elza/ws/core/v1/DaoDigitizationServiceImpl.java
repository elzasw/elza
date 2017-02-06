
/**
 * Please modify this class to meet your needs
 * This class is not complete
 */

package cz.tacr.elza.ws.core.v1;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import cz.tacr.elza.domain.ArrRequest;
import cz.tacr.elza.repository.RequestRepository;
import cz.tacr.elza.service.RequestService;
import cz.tacr.elza.ws.types.v1.DigitizationRequestResult;
import cz.tacr.elza.ws.types.v1.RequestRevoked;

/**
 * This class was generated by Apache CXF 3.1.8
 * 2016-12-09T13:06:28.052+01:00
 * Generated source version: 3.1.8
 *
 */

@Component
@javax.jws.WebService(
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
                if (StringUtils.isNotBlank(requestRevoked.getDescription())) {
                    request.setRejectReason(requestRevoked.getDescription());
                    requestRepository.save(request);
                }

                logger.info("Finished operation digitizationRequestRevoked.");
            } else {
                logger.warn("Request for code=" + requestRevoked.getIdentifier() + " was not found." );
            }
        } catch (Exception e) {
            logger.error("Fail operation digitizationRequestRevoked", e);
            throw new CoreServiceException(e.getMessage(), e);
        }
    }

}
