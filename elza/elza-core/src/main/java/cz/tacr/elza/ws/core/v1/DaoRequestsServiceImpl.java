
/**
 * Please modify this class to meet your needs
 * This class is not complete
 */

package cz.tacr.elza.ws.core.v1;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import cz.tacr.elza.domain.ArrDao;
import cz.tacr.elza.domain.ArrDaoPackage;
import cz.tacr.elza.domain.ArrDaoRequest;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.ArrRequest;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.codes.DigitizationCode;
import cz.tacr.elza.repository.DaoRequestDaoRepository;
import cz.tacr.elza.repository.DaoRequestRepository;
import cz.tacr.elza.service.ArrangementService;
import cz.tacr.elza.service.RequestService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;


@Component
@jakarta.jws.WebService(
                      serviceName = "CoreService",
                      portName = "DaoRequestsService",
                      targetNamespace = "http://elza.tacr.cz/ws/core/v1",
//                      wsdlLocation = "file:elza-core-v1.wsdl",
                      endpointInterface = "cz.tacr.elza.ws.core.v1.DaoRequestsService")
public class DaoRequestsServiceImpl implements DaoRequestsService {

    private Log logger = LogFactory.getLog(this.getClass());

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private DaoRequestRepository daoRequestRepository;

    @Autowired
    private cz.tacr.elza.service.DaoService daoService;

    @Autowired
    private DaoRequestDaoRepository daoRequestDaoRepository;

    @Autowired
    private ArrangementService arrangementService;

    @Autowired
    private RequestService requestService;

    /* (non-Javadoc)
     * @see cz.tacr.elza.ws.core.v1.DaoRequestsService#destructionRequestRevoked(cz.tacr.elza.ws.types.v1.RequestRevoked requestRevoked)*
     */
    @Override
    @Transactional
    public void destructionRequestRevoked(final cz.tacr.elza.ws.types.v1.RequestRevoked requestRevoked) throws CoreServiceException {
        try {
            logger.info("Executing operation destructionRequestRevoked");

            final List<ArrDaoRequest> daoLinkRequestList = daoRequestRepository.findByCode(requestRevoked.getIdentifier());
            for (ArrDaoRequest arrDaoLinkRequest : daoLinkRequestList) {
                requestService.setRequestState(arrDaoLinkRequest, arrDaoLinkRequest.getState(), ArrRequest.State.REJECTED);
                entityManager.refresh(arrDaoLinkRequest);
                arrDaoLinkRequest.setRejectReason(requestRevoked.getDescription());
                daoRequestRepository.save(arrDaoLinkRequest);
            }

            logger.info("Ending operation destructionRequestRevoked");
        } catch (Exception e) {
            logger.error("Fail operation destructionRequestRevoked", e);
            throw new CoreServiceException(e.getMessage(), e);
        }
    }

    /* (non-Javadoc)
     * @see cz.tacr.elza.ws.core.v1.DaoRequestsService#transferRequestFinished(java.lang.String requestIdentifier)*
     */
    @Override
    @Transactional
    public void transferRequestFinished(final java.lang.String requestIdentifier) throws CoreServiceException {
        try {
            logger.info("Executing operation transferRequestFinished");

            final List<ArrDaoRequest> daoRequestList = daoRequestRepository.findByCode(requestIdentifier);
            for (ArrDaoRequest arrDaoRequest : daoRequestList) {
                if (ArrDaoRequest.Type.TRANSFER.equals(arrDaoRequest.getType())) {
                    finishDaoRequest(arrDaoRequest);
                } else {
                    throw new BusinessException("DAO Request je neočekávaného typu", DigitizationCode.UNWANTED_REQUEST_TYPE);
                }
            }

            logger.info("Ending operation transferRequestFinished");
        } catch (Exception e) {
            logger.error("Fail operation transferRequestFinished", e);
            throw new CoreServiceException(e.getMessage(), e);
        }
    }

    /* (non-Javadoc)
     * @see cz.tacr.elza.ws.core.v1.DaoRequestsService#transferRequestRevoked(cz.tacr.elza.ws.types.v1.RequestRevoked requestRevoked)*
     */
    @Override
    @Transactional
    public void transferRequestRevoked(final cz.tacr.elza.ws.types.v1.RequestRevoked requestRevoked) throws CoreServiceException {
        try {
            logger.info("Executing operation transferRequestRevoked");

            final List<ArrDaoRequest> daoLinkRequestList = daoRequestRepository.findByCode(requestRevoked.getIdentifier());

            for (ArrDaoRequest arrDaoLinkRequest : daoLinkRequestList) {
                if (arrDaoLinkRequest.getType().equals(ArrDaoRequest.Type.TRANSFER)) {
                    requestService.setRequestState(arrDaoLinkRequest, arrDaoLinkRequest.getState(), ArrRequest.State.REJECTED);
                    entityManager.refresh(arrDaoLinkRequest);
                    arrDaoLinkRequest.setRejectReason(requestRevoked.getDescription());
                    daoRequestRepository.save(arrDaoLinkRequest);
                } else {
                    throw new CoreServiceException("Operace transferRequestRevoked byla spučtěna nad requertem typu " + arrDaoLinkRequest.getType());
                }
            }

            logger.info("Ending operation transferRequestRevoked");
        } catch (Exception e) {
            logger.error("Fail operation transferRequestRevoked", e);
            throw new CoreServiceException(e.getMessage(), e);
        }
    }

    /*
     * @see cz.tacr.elza.ws.core.v1.DaoRequestsService#destructionRequestFinished(java.lang.String requestIdentifier)*
     */
    @Override
    @Transactional
    public void destructionRequestFinished(final java.lang.String requestIdentifier) throws CoreServiceException {
        try {
            logger.info("Executing operation destructionRequestFinished");

            final List<ArrDaoRequest> daoLinkRequestList = daoRequestRepository.findByCode(requestIdentifier);
            for (ArrDaoRequest arrDaoRequest : daoLinkRequestList) {
                if (ArrDaoRequest.Type.DESTRUCTION.equals(arrDaoRequest.getType())) {
                    finishDaoRequest(arrDaoRequest);
                } else {
                    throw new BusinessException("DAO Request je neočekávaného typu", DigitizationCode.UNWANTED_REQUEST_TYPE);
                }
            }


            logger.info("Ending operation destructionRequestFinished");
        } catch (Exception e) {
            logger.error("Fail operation destructionRequestFinished", e);
            throw new CoreServiceException(e.getMessage(), e);
        }
    }

    private void finishDaoRequest(final ArrDaoRequest arrDaoRequest) {
        requestService.setRequestState(arrDaoRequest, arrDaoRequest.getState(), ArrRequest.State.PROCESSED);

        ArrFundVersion fundVersion = arrangementService.getOpenVersionByFund(arrDaoRequest.getFund());

        List<ArrDao> daoByDaoRequest = daoRequestDaoRepository.findDaoByDaoRequest(arrDaoRequest);
        if (daoByDaoRequest.size() == 1) {
            ArrDao arrDao = daoByDaoRequest.get(0);
            ArrDaoPackage daoPackage = arrDao.getDaoPackage();
            daoService.deleteDaoPackageWithCascade(fundVersion, daoPackage);
        } else {
            // Označí všechny DAO z požadavku jako neplatné a ukončí jeho případné linky na JP bez notifikace
            daoService.deleteDaos(fundVersion, daoByDaoRequest, true);
        }
    }
}
