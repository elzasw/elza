package cz.tacr.elza.dao.service;

import java.util.Collection;

import org.springframework.stereotype.Service;

import cz.tacr.elza.dao.bo.DaoFileBo;
import cz.tacr.elza.dao.bo.DaoPackageBo;
import cz.tacr.elza.dao.common.GlobalLock;
import cz.tacr.elza.ws.types.v1.DaoImport;
import cz.tacr.elza.ws.types.v1.DaoLinks;
import cz.tacr.elza.ws.types.v1.DaoPackages;

@Service
public class ResourceService {

	public DaoFileBo getDaoFile(String packageIdentifier, String daoIdentifier, String fileIdentifier) {
		return GlobalLock.runAtomicFunction(
				() -> new DaoPackageBo(packageIdentifier).getDao(daoIdentifier).getDaoFile(fileIdentifier));
	}

	public Collection<DaoFileBo> getDaoFiles(String packageIdentifier, String daoIdentifier) {
		return GlobalLock.runAtomicFunction(
				() -> new DaoPackageBo(packageIdentifier).getDao(daoIdentifier).getAllDaoFiles());
	}

	public DaoImport getDaoImport(String[] packageIdentifiers) {
		return GlobalLock.runAtomicFunction(() -> getDaoImportInternal(packageIdentifiers));
	}

	private DaoImport getDaoImportInternal(String[] packageIdentifiers) {
		DaoImport daoImport = new DaoImport();
		DaoPackages daoPackages = new DaoPackages();
		DaoLinks daoLinks = new DaoLinks();
		for (String identifier : packageIdentifiers) {
			DaoPackageBo daoPackageBo = new DaoPackageBo(identifier);
			daoPackages.getDaoPackage().add(daoPackageBo.export());
			daoLinks.getDaoLink().addAll(daoPackageBo.exportLinks());
		}
		daoImport.setDaoPackages(daoPackages);
		daoImport.setDaoLinks(daoLinks);
		return daoImport;
	}
}