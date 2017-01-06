package cz.tacr.elza.dao.api.impl;

import javax.jws.WebService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import cz.tacr.elza.dao.bo.resource.DaoRequestInfo;
import cz.tacr.elza.dao.exception.DaoComponentException;
import cz.tacr.elza.dao.service.DcsDaoRequestService;
import cz.tacr.elza.ws.dao_service.v1.DaoRequests;
import cz.tacr.elza.ws.dao_service.v1.DaoServiceException;
import cz.tacr.elza.ws.types.v1.DestructionRequest;
import cz.tacr.elza.ws.types.v1.TransferRequest;

@Service
@WebService(endpointInterface = "cz.tacr.elza.ws.dao_service.v1.DaoRequests")
public class DaoRequestsImpl implements DaoRequests {

	@Autowired
	private DcsDaoRequestService requestService;

	@Override
	public String postDestructionRequest(DestructionRequest destructionRequest) throws DaoServiceException {
		requestService.checkRejectMode();
		DaoRequestInfo requestInfo = new DaoRequestInfo();
		requestInfo.setDaoIdentifiers(destructionRequest.getDaoIdentifiers().getIdentifier());
		requestInfo.setRequestIdentifier(destructionRequest.getIdentifier());
		requestInfo.setSystemIdentifier(destructionRequest.getSystemIdentifier());
		requestInfo.setDescription(destructionRequest.getDescription());
		try {
			requestService.createRequest(requestInfo, true);
		} catch (DaoComponentException e) {
			throw new DaoServiceException(e.getMessage(), e.getCause());
		}
		return requestInfo.getIdentifier();
	}

	@Override
	public String postTransferRequest(TransferRequest transferRequest) throws DaoServiceException {
		requestService.checkRejectMode();
		DaoRequestInfo requestInfo = new DaoRequestInfo();
		requestInfo.setDaoIdentifiers(transferRequest.getDaoIdentifiers().getIdentifier());
		requestInfo.setRequestIdentifier(transferRequest.getIdentifier());
		requestInfo.setSystemIdentifier(transferRequest.getSystemIdentifier());
		requestInfo.setDescription(transferRequest.getDescription());
		requestInfo.setTargetFund(transferRequest.getTargetFund());
		try {
			requestService.createRequest(requestInfo, false);
		} catch (DaoComponentException e) {
			throw new DaoServiceException(e.getMessage(), e.getCause());
		}
		return requestInfo.getIdentifier();
	}
}