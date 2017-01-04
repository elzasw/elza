package cz.tacr.elza.dao.bo;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.util.Assert;

import cz.tacr.elza.dao.FileUtils;
import cz.tacr.elza.dao.PathResolver;
import cz.tacr.elza.dao.descriptor.DaoConfig;
import cz.tacr.elza.dao.exception.DaoComponentException;

public class DaoBo implements DescriptorTarget<DaoConfig> {

	private final static char UID_SEPARATOR = '/';

	private final DaoPackageBo daoPackage;

	private final String identifier;

	private Map<String, DaoFileBo> daoFiles;

	private DaoConfig descriptor;

	private boolean daoFileLazyInit = true;

	public DaoBo(DaoPackageBo daoPackage, String identifier) {
		Assert.notNull(daoPackage);
		Assert.notNull(identifier);
		this.daoPackage = daoPackage;
		this.identifier = identifier;
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

	public Collection<DaoFileBo> getAllDaoFiles() {
		return getDaoFiles(true).values();
	}

	public DaoFileBo getDaoFile(String fileIdentifier) {
		return getDaoFiles(false).computeIfAbsent(fileIdentifier, k -> {
			DaoFileBo daoFile;
			if (!daoFileLazyInit || !(daoFile = new DaoFileBo(this, k)).isValidDescriptor()) {
				return null;
			}
			return daoFile;
		});
	}

	public List<String> getRelatedDaoIdentifiers() {
		return getDescriptor().getRelatedDaoIdentifiers();
	}

	public String getDidIdentifier() {
		return getDescriptor().getDidIdentifier();
	}

	public void setDidIdentifier(String didIdentifier) {
		getDescriptor().setDidIdentifier(didIdentifier);
	}

	public String getLabel() {
		return getDescriptor().getLabel();
	}

	@Override
	public DaoConfig getDescriptor() {
		if (descriptor == null) {
			Path path = PathResolver.resolveDaoConfigPath(daoPackage.getIdentifier(), identifier);
			try {
				descriptor = FileUtils.readYamlFile(path, DaoConfig.class);
			} catch (IOException e) {
				throw new DaoComponentException("dao descriptor not found", e);
			}
		}
		return descriptor;
	}

	@Override
	public void saveDescriptor() {
		if (descriptor != null && descriptor.isDirty()) {
			Path path = PathResolver.resolveDaoConfigPath(daoPackage.getIdentifier(), identifier);
			try {
				FileUtils.saveYamlFile(path, descriptor);
			} catch (IOException e) {
				throw new DaoComponentException("cannot save dao descriptor", e);
			}
		}
	}

	@Override
	public String toString() {
		return "DaoBo [packageIdentifier:" + daoPackage.getIdentifier() + ", identifier:" + identifier + "]";
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

	private Map<String, DaoFileBo> getDaoFiles(boolean forceInit) {
		if (daoFileLazyInit) {
			if (daoFiles == null) {
				daoFiles = new HashMap<>();
			}
			if (!forceInit) {
				return daoFiles;
			} else {
				daoFileLazyInit = false;
			}
		} else {
			if (daoFiles != null) {
				return daoFiles;
			} else {
				daoFiles = new HashMap<>();
			}
		}
		PathResolver.forEachDaoFilePath(daoPackage.getIdentifier(), identifier, path -> {
			String fileIdentifier = path.getFileName().toString();
			daoFiles.computeIfAbsent(fileIdentifier, k -> new DaoFileBo(this, k));
		});
		return daoFiles;
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
