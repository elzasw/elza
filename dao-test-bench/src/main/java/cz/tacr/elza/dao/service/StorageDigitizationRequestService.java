package cz.tacr.elza.dao.service;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import cz.tacr.elza.dao.DCStorageConfig;
import cz.tacr.elza.dao.bo.resource.DigitizationRequestResource;
import cz.tacr.elza.dao.common.GlobalLock;
import cz.tacr.elza.dao.exception.DaoComponentException;
import cz.tacr.elza.ws.dao_service.v1.DaoServiceException;
import cz.tacr.elza.ws.types.v1.DigitizationRequest;

@Service
public class StorageDigitizationRequestService {

	@Autowired
	private ResourceService resourceService;

	@Autowired
	private DCStorageConfig storageConfig;

	public String getSystemIdentifier(String requestIdentifier) {
		return GlobalLock.runAtomicFunction(() -> {
			try {
				return new DigitizationRequestResource(requestIdentifier).getOrInit().getSystemIdentifier();
			} catch (Exception e) {
				throw new DaoComponentException("dao request initialization failed", e);
			}
		});
	}

	public String createRequest(DigitizationRequest digitizationRequest) {
		return GlobalLock.runAtomicFunction(() -> {
			try {
				return DigitizationRequestResource.create(digitizationRequest).getIdentifier();
			} catch (IOException e) {
				throw new DaoComponentException("digitization request creation failed", e);
			}
		});
	}

	public void deleteRequest(String requestIdentifier) {
		GlobalLock.runAtomicAction(() -> {
			try {
				new DigitizationRequestResource(requestIdentifier).delete();
			} catch (IOException e) {
				throw new DaoComponentException("digitization request deletion failed", e);
			}
		});
	}

	public void checkRejectMode() throws DaoServiceException {
		if (storageConfig.isRejectMode()) {
			throw new DaoServiceException("reject mode enabled");
		}
	}
}