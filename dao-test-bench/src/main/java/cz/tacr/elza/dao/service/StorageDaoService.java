package cz.tacr.elza.dao.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import cz.tacr.elza.dao.DCStorageConfig;
import cz.tacr.elza.dao.bo.DaoBo;
import cz.tacr.elza.dao.bo.DaoPackageBo;
import cz.tacr.elza.dao.common.GlobalLock;
import cz.tacr.elza.ws.dao_service.v1.DaoServiceException;

@Service
public class StorageDaoService {

	@Autowired
	private DCStorageConfig storageConfig;

	public boolean deleteDao(String daoUId, String deleteEntry) {
		String[] parsedUId = DaoBo.parseUId(daoUId);
		DaoPackageBo packageBo = new DaoPackageBo(parsedUId[0]);
		return GlobalLock.runAtomicFunction(() -> packageBo.deleteDao(parsedUId[1], deleteEntry));
	}

	public void linkDao(String daoUId, String didIdentifier) {
		String[] parsedUId = DaoBo.parseUId(daoUId);
		DaoPackageBo packageBo = new DaoPackageBo(parsedUId[0]);
		GlobalLock.runAtomicAction(() -> {
			DaoBo dao = packageBo.getDao(parsedUId[1]);
			dao.getConfig().setDidIdentifier(didIdentifier);
			dao.saveConfig();
		});
	}

	public void unlinkDao(String daoUId) {
		linkDao(daoUId, null);
	}

	public void checkRejectMode() throws DaoServiceException {
		if (storageConfig.isRejectMode()) {
			throw new DaoServiceException("reject mode enabled");
		}
	}
}