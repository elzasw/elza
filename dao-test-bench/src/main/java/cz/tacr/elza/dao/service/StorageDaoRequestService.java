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
	public String getSystemIdentifier(String requestIdentifier, boolean destrRequest) {
		return GlobalLock.runAtomicFunction(() -> {
			try {
				return new DaoRequestInfoResource(requestIdentifier, destrRequest).getOrInit().getSystemIdentifier();
			} catch (Exception e) {
				throw new DaoComponentException("dao request initialization failed", e);
			}
		});
	}

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
	 */
	public void confirmRequest(String requestIdentifier, boolean destrRequest) {
		GlobalLock.runAtomicAction(() -> {
			DaoRequestInfoResource resource = new DaoRequestInfoResource(requestIdentifier, destrRequest);
			try {
				List<String> daoIdentifiers = resource.getOrInit().getDaoIdentifiers();
				for (String daoUId : daoIdentifiers) {
					Path rrp = PathResolver.resolveRelativeDaoRequestPath(requestIdentifier, destrRequest);
					if (!storageDaoService.deleteDao(daoUId, rrp.toString())) {
						LOG.error("dao already deleted, uid:" + daoUId);
					}
				}
				resource.delete();
			} catch (Exception e) {
				throw new DaoComponentException("dao request confirmation failed", e);
			}
		});
	}

	/**
	 * @param destrRequest destruction request otherwise transfer request
	 */
	public void deleteRequest(String requestIdentifier, boolean destrRequest) {
		GlobalLock.runAtomicAction(() -> {
			try {
				new DaoRequestInfoResource(requestIdentifier, destrRequest).delete();
			} catch (IOException e) {
				throw new DaoComponentException("dao request deletion failed", e);
			}
		});
	}

	public void checkRejectMode() throws DaoServiceException {
		if (storageConfig.isRejectMode()) {
			throw new DaoServiceException("reject mode enabled");
		}
	}
}