package cz.tacr.elza.dao.service;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import cz.tacr.elza.dao.DCStorageConfig;
import cz.tacr.elza.dao.bo.resource.DaoRequestInfo;
import cz.tacr.elza.dao.bo.resource.DaoRequestInfo.Status;
import cz.tacr.elza.dao.bo.resource.DaoRequestInfoResource;
import cz.tacr.elza.dao.common.GlobalLock;
import cz.tacr.elza.dao.common.PathResolver;
import cz.tacr.elza.dao.exception.DaoComponentException;
import cz.tacr.elza.ws.dao_service.v1.DaoServiceException;

@Service
public class StorageDaoRequestService {

	private static final Logger LOG = LoggerFactory.getLogger(StorageDaoRequestService.class);

	@Autowired
	private DCStorageConfig storageConfig;

	@Autowired
	private StorageDaoService storageDaoService;

	/**
	 * @param destrRequest destruction request otherwise transfer request
	 */
	public String createRequest(DaoRequestInfo daoRequestInfo, boolean destrRequest) {
		return GlobalLock.runAtomicFunction(() -> {
			try {
				return DaoRequestInfoResource.create(daoRequestInfo, destrRequest).getIdentifier();
			} catch (IOException e) {
				throw new DaoComponentException("dao request creation failed", e);
			}
		});
	}

	/**
	 * @param destrRequest destruction request otherwise transfer request
	 * @return finished dao request info
	 */
	public DaoRequestInfo confirmRequest(String requestIdentifier, boolean destrRequest) {
		return GlobalLock.runAtomicFunction(() -> {
			DaoRequestInfoResource resource = new DaoRequestInfoResource(requestIdentifier, destrRequest);
			try {
				DaoRequestInfo requestInfo = resource.getOrInit();
				if (requestInfo.getStatus() == Status.FINISHED) {
					LOG.info("dao request already finished, requestIdentifier:" + requestIdentifier);
				} else if (requestInfo.getStatus() == Status.REVOKED) {
					throw new IllegalStateException("dao request is revoked, requestIdentifier:" + requestIdentifier);
				} else {
					List<String> daoIdentifiers = resource.getOrInit().getDaoIdentifiers();
					for (String daoUId : daoIdentifiers) {
						Path path = PathResolver.resolveDaoRequestInfoRelativePath(requestIdentifier, destrRequest);
						if (!storageDaoService.deleteDao(daoUId, path.toString())) {
							LOG.error("dao already deleted, uid:" + daoUId);
						}
					}
					requestInfo.setStatus(Status.FINISHED);
					resource.save();
				}
				return requestInfo;
			} catch (Exception e) {
				throw new DaoComponentException("dao request confirmation failed", e);
			}
		});
	}

	/**
	 * @param destrRequest destruction request otherwise transfer request
	 * @return finished dao request info
	 */
	public DaoRequestInfo rejectRequest(String requestIdentifier, boolean destrRequest) {
		return GlobalLock.runAtomicFunction(() -> {
			DaoRequestInfoResource resource = new DaoRequestInfoResource(requestIdentifier, destrRequest);
			try {
				DaoRequestInfo requestInfo = resource.getOrInit();
				if (requestInfo.getStatus() == Status.REVOKED) {
					LOG.info("dao request already revoked, requestIdentifier:" + requestIdentifier);
				} else if (requestInfo.getStatus() == Status.FINISHED) {
					throw new IllegalStateException("dao request is finished, requestIdentifier:" + requestIdentifier);
				} else {
					requestInfo.setStatus(Status.REVOKED);
					resource.save();
				}
				return requestInfo;
			} catch (Exception e) {
				throw new DaoComponentException("dao request rejection failed", e);
			}
		});
	}

	public void checkRejectMode() throws DaoServiceException {
		if (storageConfig.isRejectMode()) {
			throw new DaoServiceException("reject mode enabled");
		}
	}
}
