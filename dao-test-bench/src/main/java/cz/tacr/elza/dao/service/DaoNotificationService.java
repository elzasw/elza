package cz.tacr.elza.dao.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import cz.tacr.elza.dao.DCStorageApp;
import cz.tacr.elza.dao.GlobalLock;
import cz.tacr.elza.dao.bo.DaoBo;
import cz.tacr.elza.dao.bo.DaoPackageBo;
import cz.tacr.elza.ws.dao_service.v1.DaoServiceException;

@Service
public class DaoNotificationService {

	@Autowired
	private Environment env;

	public void linkDao(String daoUId, String didIdentifier) {
		String[] parsedUId = DaoBo.parseUId(daoUId);
		DaoPackageBo packageBo = new DaoPackageBo(parsedUId[0]);
		GlobalLock.runAtomicAction(() -> {
			DaoBo daoBo = packageBo.getDao(parsedUId[1]);
			daoBo.setDidIdentifier(didIdentifier);
			daoBo.saveDescriptor();
		});
	}

	public void unlinkDao(String daoUId) {
		linkDao(daoUId, null);
	}

	public void checkRejectMode() throws DaoServiceException {
		if (env.containsProperty(DCStorageApp.REJECT_MODE_PARAM_NAME)) {
			throw new DaoServiceException("reject mode enabled");
		}
	}
}
