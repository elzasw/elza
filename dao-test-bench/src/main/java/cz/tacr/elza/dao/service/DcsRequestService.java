package cz.tacr.elza.dao.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import cz.tacr.elza.dao.DCStorageApp;
import cz.tacr.elza.dao.FileUtils;
import cz.tacr.elza.dao.GlobalLock;
import cz.tacr.elza.dao.GlobalLock.StorageAction;
import cz.tacr.elza.dao.bo.DaoBo.DaoUniqueIdentifier;
import cz.tacr.elza.dao.descriptor.DaoRequestInfo;
import cz.tacr.elza.ws.dao_service.v1.DaoServiceException;

@Service
public class DcsRequestService {

	private static final Logger LOG = LoggerFactory.getLogger(DcsRequestService.class);

	@Autowired
	private Environment env;

	/**
	 * @param destrRequest destruction request otherwise transfer request
	 */
	public void createRequest(DaoRequestInfo requestInfo, boolean destrRequest) throws IOException {
		// TODO: check on disc
		requestInfo.setIdentifier(String.valueOf(System.currentTimeMillis()));
		GlobalLock.runAtomicAction(new StorageAction() {
			@Override
			public void execute() throws IOException {
				Path requestInfoPath = fileManager.resolveRequestInfoPath(requestInfo.getIdentifier(), destrRequest);
				Files.createDirectories(requestInfoPath);
				FileUtils.createYamlFile(requestInfoPath, requestInfo);
			}
			@Override
			public void exceptionInterceptor(IOException e) {
				LOG.error("request " + (destrRequest ? "destr" : "trans") + " failed", e);
			}
		});
	}

	/**
	 * @param destrRequest destruction request otherwise transfer request
	 */
	public void confirmRequest(String requestIdentifier, boolean destrRequest) throws IOException {
		Path requestInfoPath = fileManager.resolveRequestInfoPath(requestIdentifier, destrRequest);
		GlobalLock.runAtomicAction(() -> {
			DaoRequestInfo requestInfo = FileUtils.readYamlFile(requestInfoPath, DaoRequestInfo.class);
			for (String uniqueIdentifier : requestInfo.getDaoIdentifiers()) {
				DaoUniqueIdentifier identifier = DaoUniqueIdentifier.parse(uniqueIdentifier);
				try {
					fileManager.createDeleteEntry(identifier.getPackageIdentifier(), identifier.getDaoIdentifier(),
							requestIdentifier, destrRequest);
				} catch (IOException e) {
					LOG.error("create dao delete entry failed", e);
				}
			}
			deleteRequestFolder(requestInfoPath);
		});
	}

	/**
	 * @param destrRequest destruction request otherwise transfer request
	 */
	public void deleteRequest(String requestIdentifier, boolean destrRequest) throws IOException {
		GlobalLock.runAtomicAction(() -> {
			Path requestPath = fileManager.resolveRequestInfoPath(requestIdentifier, destrRequest);
			deleteRequestFolder(requestPath);
		});
	}

	public void checkRejectMode() throws DaoServiceException {
		if (env.containsProperty(DCStorageApp.REJECT_MODE_PARAM_NAME)) {
			throw new DaoServiceException("reject mode enabled");
		}
	}

	private static void deleteRequestFolder(Path requestInfoPath) throws IOException {
		Files.delete(requestInfoPath.getParent());
		Files.delete(requestInfoPath);
	}
}
