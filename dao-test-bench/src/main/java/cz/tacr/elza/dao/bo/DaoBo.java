package cz.tacr.elza.dao.bo;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.util.Assert;

import cz.tacr.elza.dao.DCStorageConfig;
import cz.tacr.elza.dao.bo.resource.DaoConfig;
import cz.tacr.elza.dao.bo.resource.DaoConfigResource;
import cz.tacr.elza.dao.bo.resource.ItemConfig;
import cz.tacr.elza.dao.common.PathResolver;
import cz.tacr.elza.dao.exception.DaoComponentException;
import cz.tacr.elza.ws.types.v1.Dao;
import cz.tacr.elza.ws.types.v1.DaoLink;
import cz.tacr.elza.ws.types.v1.DaoType;
import cz.tacr.elza.ws.types.v1.FileGroup;
import cz.tacr.elza.ws.types.v1.Items;

public class DaoBo {

	private final static char UID_SEPARATOR = '/';

	private final DaoPackageBo daoPackage;

	private final String identifier;

	private final DaoConfigResource configResource;

	private final Map<String, DaoFileBo> daoFiles = new HashMap<>();

	private boolean allFileInitialized;

	public DaoBo(DaoPackageBo daoPackage, String identifier, boolean eagerInit) {
        Assert.notNull(daoPackage);
		Assert.notNull(identifier);
		this.daoPackage = daoPackage;
		this.identifier = identifier;
		configResource = new DaoConfigResource(daoPackage.getIdentifier(), identifier);
		if (eagerInit) {
			initConfigResource();
		}
	}

	public DaoPackageBo getDaoPackage() {
		return daoPackage;
	}

	public String getIdentifier() {
		return identifier;
	}

	public String getUId() {
		return daoPackage.getIdentifier() + UID_SEPARATOR + identifier;
	}

	public DaoConfig getConfig() {
		return initConfigResource();
	}

	public void saveConfig() {
		try {
			configResource.save();
		} catch (IOException e) {
			throw new DaoComponentException("cannot save dao config", e);
		}
	}

	public Collection<DaoFileBo> getAllDaoFiles() {
		if (!allFileInitialized) {
			PathResolver.forEachDaoFilePath(daoPackage.getIdentifier(), identifier, path -> {
				String fileIdentifier = path.getFileName().toString();
				daoFiles.computeIfAbsent(fileIdentifier, k -> new DaoFileBo(this, k, false));
			});
			allFileInitialized = true;
		}
		return daoFiles.values();
	}

	public DaoFileBo getDaoFile(String fileIdentifier) {
		return daoFiles.computeIfAbsent(fileIdentifier, k -> {
			if (allFileInitialized) {
				throw new DaoComponentException("dao file not found, identifier:" + k);
			}
			return new DaoFileBo(this, k, true);
		});
	}

	public Dao export() {
		Dao dao = new Dao();
		dao.setIdentifier(getUId());
		dao.setLabel(getConfig().getLabel());
        DaoType daoType = getConfig().getDaoType();
        if (daoType == null) {
            daoType = DaoType.ATTACHMENT;
        }
        dao.setDaoType(daoType);

        List<ItemConfig> configItems = getConfig().getItems();
        if (configItems != null && configItems.size() > 0) {
            Items items = new Items();
            for (ItemConfig configItem : configItems) {
                Object item = configItem.getItem();
                items.getStrOrLongOrEnm().add(item);
            }

            dao.setItems(items);
        }
		FileGroup fileGroup = new FileGroup();
		for (DaoFileBo daoFile : getAllDaoFiles()) {
			fileGroup.getFile().add(daoFile.export());
		}
        dao.setFiles(fileGroup);
		return dao;
	}

	public DaoLink exportLink() {
		String didIdentifier = getConfig().getDidIdentifier();
		if (didIdentifier == null) {
			return null;
		}
		DaoLink link = new DaoLink();
		link.setDaoIdentifier(getUId());
		link.setDidIdentifier(didIdentifier);
		link.setRepositoryIdentifier(DCStorageConfig.get().getRepositoryIdentifier());
		return link;
	}

	@Override
	public String toString() {
		return "DaoBo [packageIdentifier=" + daoPackage.getIdentifier() + ", identifier=" + identifier + "]";
	}

	@Override
	public int hashCode() {
		return Objects.hash(daoPackage, identifier);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}
		if (obj == this) {
			return true;
		}
		if (obj instanceof DaoBo) {
			DaoBo o = (DaoBo) obj;
			return daoPackage.equals(o.daoPackage) && identifier.equals(o.identifier);
		}
		return false;
	}

	private DaoConfig initConfigResource() {
		try {
			return configResource.getOrInit();
		} catch (Exception e) {
			throw new DaoComponentException("cannot init dao config: " + configResource.getResourcePath(), e);
		}
	}

	public static String[] parseUId(String value) {
		int index;
		if (value == null
				|| (value = value.trim()).length() == 0
				|| (index = value.indexOf(UID_SEPARATOR)) < 1
				|| index == value.length() - 1) {
			throw new DaoComponentException("invalid dao uid: " + value);
		}
		return new String[] { value.substring(0, index), value.substring(index + 1) };
	}
}