package cz.tacr.elza.dao.bo;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.util.Assert;

import cz.tacr.elza.dao.DCStorageConfig;
import cz.tacr.elza.dao.bo.resource.DaoConfigResource;
import cz.tacr.elza.dao.bo.resource.DaoPackageConfig;
import cz.tacr.elza.dao.bo.resource.DaoPackageConfigResource;
import cz.tacr.elza.dao.common.PathResolver;
import cz.tacr.elza.dao.exception.DaoComponentException;
import cz.tacr.elza.ws.types.v1.DaoBatchInfo;
import cz.tacr.elza.ws.types.v1.DaoLink;
import cz.tacr.elza.ws.types.v1.DaoPackage;
import cz.tacr.elza.ws.types.v1.Daoset;

public class DaoPackageBo {

	private final String identifier;

	private final DaoPackageConfigResource configResource;

	private final Map<String, DaoBo> daoSet = new HashMap<>();

	private boolean allDaoInitialized;

	public DaoPackageBo(String identifier) {
		Assert.notNull(identifier);
		this.identifier = identifier;
		configResource = new DaoPackageConfigResource(identifier);
	}

	public String getIdentifier() {
		return identifier;
	}

	public DaoPackageConfig getConfig() {
		try {
			return configResource.getOrInit();
		} catch (Exception e) {
			throw new DaoComponentException("package config not found", e);
		}
	}

	public Collection<DaoBo> getAllDao() {
		if (!allDaoInitialized) {
			PathResolver.forEachDaoPath(identifier, path -> {
				String daoIdentifier = path.getFileName().toString();
				daoSet.computeIfAbsent(daoIdentifier, k -> new DaoBo(this, k, false));
			});
			allDaoInitialized = true;
		}
		return daoSet.values();
	}

	public DaoBo getDao(String daoIdentifier) {
		return daoSet.computeIfAbsent(daoIdentifier, k -> {
			if (allDaoInitialized) {
				throw new DaoComponentException("dao not found, identifier:" + k);
			}
			return new DaoBo(this, k, true);
		});
	}

	public boolean deleteDao(String daoIdentifier, String deleteEntry) {
		try {
			new DaoConfigResource(identifier, daoIdentifier).delete(deleteEntry);
		} catch (FileAlreadyExistsException e) {
			return false;
		} catch (IOException e) {
			throw new DaoComponentException(e);
		}
		daoSet.remove(daoIdentifier);
		return true;
	}

	public DaoPackage export() {
		DaoPackage daoPackage = new DaoPackage();
		daoPackage.setIdentifier(identifier);
		DaoPackageConfig config = getConfig();
		daoPackage.setFundIdentifier(config.getFundIdentifier());
		daoPackage.setRepositoryIdentifier(DCStorageConfig.get().getRepositoryIdentifier());
		if (config.getBatchIdentifier() != null) {
			DaoBatchInfo batchInfo = new DaoBatchInfo();
			batchInfo.setIdentifier(config.getBatchIdentifier());
			batchInfo.setLabel(config.getBatchLabel());
			daoPackage.setDaoBatchInfo(batchInfo);
		}
		Daoset daoSet = new Daoset();
		for (DaoBo dao : getAllDao()) {
			daoSet.getDao().add(dao.export());
		}
		daoPackage.setDaoset(daoSet);
		return daoPackage;
	}

	public List<DaoLink> exportLinks() {
		List<DaoLink> links = new ArrayList<>();
		for (DaoBo dao : getAllDao()) {
			DaoLink link = dao.exportLink();
			if (link != null) {
				links.add(link);
			}
		}
		return links;
	}

	@Override
	public String toString() {
		return "DaoPackageBo [identifier=" + identifier + "]";
	}

	@Override
	public int hashCode() {
		return identifier.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}
		if (obj == this) {
			return true;
		}
		if (obj instanceof DaoPackageBo) {
			return identifier.equals(((DaoPackageBo) obj).identifier);
		}
		return false;
	}
}