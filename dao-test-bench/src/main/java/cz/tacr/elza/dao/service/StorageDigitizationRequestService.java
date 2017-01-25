package cz.tacr.elza.dao.service;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import cz.tacr.elza.dao.DCStorageConfig;
import cz.tacr.elza.dao.bo.resource.DigitizationRequestInfo;
import cz.tacr.elza.dao.bo.resource.DigitizationRequestInfo.Status;
import cz.tacr.elza.dao.bo.resource.DigitizationRequestInfoResource;
import cz.tacr.elza.dao.common.GlobalLock;
import cz.tacr.elza.dao.exception.DaoComponentException;
import cz.tacr.elza.ws.digitization.v1.DigitizationServiceException;
import cz.tacr.elza.ws.types.v1.DigitizationRequest;

@Service
public class StorageDigitizationRequestService {

	private static final Logger LOG = LoggerFactory.getLogger(StorageDigitizationRequestService.class);

	@Autowired
	private DCStorageConfig storageConfig;

	public String createRequest(DigitizationRequest digitizationRequest) {
		return GlobalLock.runAtomicFunction(() -> {
			try {
				return DigitizationRequestInfoResource.create(digitizationRequest).getIdentifier();
			} catch (IOException e) {
				throw new DaoComponentException("digitization request creation failed", e);
			}
		});
	}

	public DigitizationRequestInfo confirmRequest(String requestIdentifier) {
		return GlobalLock.runAtomicFunction(() -> {
			DigitizationRequestInfoResource resource = new DigitizationRequestInfoResource(requestIdentifier);
			try {
				DigitizationRequestInfo requestInfo = resource.getOrInit();
				if (requestInfo.getStatus() == Status.FINISHED) {
					LOG.info("digitization request already finished, requestIdentifier:" + requestIdentifier);
				} else if (requestInfo.getStatus() == Status.REVOKED) {
					throw new IllegalStateException("digitization request is revoked, requestIdentifier:" + requestIdentifier);
				} else {
					requestInfo.setStatus(Status.FINISHED);
					resource.save();
				}
				return requestInfo;
			} catch (Exception e) {
				throw new DaoComponentException("digitization request confirmation failed", e);
			}
		});
	}

	public DigitizationRequestInfo rejectRequest(String requestIdentifier) {
		return GlobalLock.runAtomicFunction(() -> {
			DigitizationRequestInfoResource resource = new DigitizationRequestInfoResource(requestIdentifier);
			try {
				DigitizationRequestInfo requestInfo = resource.getOrInit();
				if (requestInfo.getStatus() == Status.REVOKED) {
					LOG.info("digitization request already revoked, requestIdentifier:" + requestIdentifier);
				} else if (requestInfo.getStatus() == Status.FINISHED) {
					throw new IllegalStateException("digitization request is finished, requestIdentifier:" + requestIdentifier);
				} else {
					requestInfo.setStatus(Status.REVOKED);
					resource.save();
				}
				return requestInfo;
			} catch (Exception e) {
				throw new DaoComponentException("digitization request rejection failed", e);
			}
		});
	}

	public void checkRejectMode() throws DigitizationServiceException {
		if (storageConfig.isRejectMode()) {
			throw new DigitizationServiceException("reject mode enabled");
		}
	}
}
