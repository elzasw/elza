package cz.tacr.elza.dao.api.ws;

import javax.jws.WebService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import cz.tacr.elza.dao.bo.resource.DaoRequestInfo;
import cz.tacr.elza.dao.exception.DaoComponentException;
import cz.tacr.elza.dao.service.StorageDaoRequestService;
import cz.tacr.elza.ws.dao_service.v1.DaoRequests;
import cz.tacr.elza.ws.dao_service.v1.DaoServiceException;
import cz.tacr.elza.ws.types.v1.DaosSyncRequest;
import cz.tacr.elza.ws.types.v1.DaosSyncResponse;
import cz.tacr.elza.ws.types.v1.DestructionRequest;
import cz.tacr.elza.ws.types.v1.TransferRequest;

@Service
@WebService(name = DaoRequestsImpl.NAME,
		portName = DaoRequestsImpl.NAME,
		serviceName = DaoRequestsImpl.NAME,
		targetNamespace = "http://elza.tacr.cz/ws/dao-service/v1",
		endpointInterface = "cz.tacr.elza.ws.dao_service.v1.DaoRequests")
public class DaoRequestsImpl implements DaoRequests {

	public static final String NAME = "DaoRequests";

	@Autowired
	private StorageDaoRequestService storageDaoRequestService;

	@Override
	public String postDestructionRequest(DestructionRequest destructionRequest) throws DaoServiceException {
		storageDaoRequestService.checkRejectMode();
		DaoRequestInfo requestInfo = new DaoRequestInfo();
		requestInfo.setDaoIdentifiers(destructionRequest.getDaoIdentifiers().getIdentifier());
		requestInfo.setRequestIdentifier(destructionRequest.getIdentifier());
		requestInfo.setSystemIdentifier(destructionRequest.getSystemIdentifier());
		requestInfo.setDescription(destructionRequest.getDescription());
		requestInfo.setUsername(destructionRequest.getUsername());
		try {
			return storageDaoRequestService.createRequest(requestInfo, true);
		} catch (DaoComponentException e) {
			throw new DaoServiceException(e.getMessage(), e.getCause());
		}
	}

	@Override
	public String postTransferRequest(TransferRequest transferRequest) throws DaoServiceException {
		storageDaoRequestService.checkRejectMode();
		DaoRequestInfo requestInfo = new DaoRequestInfo();
		requestInfo.setDaoIdentifiers(transferRequest.getDaoIdentifiers().getIdentifier());
		requestInfo.setRequestIdentifier(transferRequest.getIdentifier());
		requestInfo.setSystemIdentifier(transferRequest.getSystemIdentifier());
		requestInfo.setDescription(transferRequest.getDescription());
		requestInfo.setUsername(transferRequest.getUsername());
		requestInfo.setTargetFund(transferRequest.getTargetFund());
		try {
			return storageDaoRequestService.createRequest(requestInfo, false);
		} catch (DaoComponentException e) {
			throw new DaoServiceException(e.getMessage(), e.getCause());
		}
	}

	@Override
	public DaosSyncResponse syncDaos(DaosSyncRequest daosSyncRequest) throws DaoServiceException {
		// todo[MCV-56215]
		return null;
	}
}
