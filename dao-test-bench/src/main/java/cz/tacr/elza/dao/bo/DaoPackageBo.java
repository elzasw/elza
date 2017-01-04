package cz.tacr.elza.dao.bo;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.springframework.util.Assert;

import cz.tacr.elza.dao.FileUtils;
import cz.tacr.elza.dao.PathResolver;
import cz.tacr.elza.dao.descriptor.DaoPackageConfig;
import cz.tacr.elza.dao.exception.DaoComponentException;

public class DaoPackageBo implements DescriptorTarget<DaoPackageConfig> {

	private final String identifier;

	private Map<String, DaoBo> daoSet;

	private DaoPackageConfig descriptor;

	private boolean daoLazyInit = true;

	public DaoPackageBo(String identifier) {
		Assert.notNull(identifier);
		this.identifier = identifier;
	}

	public String getIdentifier() {
		return identifier;
	}

	public Collection<DaoBo> getAllDao() {
		return getDaoSet(true).values();
	}

	public DaoBo getDao(String daoIdentifier) {
		return getDaoSet(false).computeIfAbsent(daoIdentifier, k -> {
			DaoBo dao;
			if (!daoLazyInit || !(dao = new DaoBo(this, k)).isValidDescriptor()) {
				return null;
			}
			return dao;
		});
	}

	public String getFundIdentifier() {
		return getDescriptor().getFundIdentifier();
	}

	public String getBatchIdentifier() {
		return getDescriptor().getBatchIdentifier();
	}

	public String getBatchLabel() {
		return getDescriptor().getBatchLabel();
	}

	@Override
	public DaoPackageConfig getDescriptor() {
		if (descriptor == null) {
			Path path = PathResolver.resolvePackageConfigPath(identifier);
			try {
				descriptor = FileUtils.readYamlFile(path, DaoPackageConfig.class);
			} catch (IOException e) {
				throw new DaoComponentException("package descriptor not found", e);
			}
		}
		return descriptor;
	}

	@Override
	public void saveDescriptor() {
		throw new UnsupportedOperationException();
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

	private Map<String, DaoBo> getDaoSet(boolean forceInit) {
		if (daoLazyInit) {
			if (daoSet == null) {
				daoSet = new HashMap<>();
			}
			if (!forceInit) {
				return daoSet;
			} else {
				daoLazyInit = false;
			}
		} else {
			if (daoSet != null) {
				return daoSet;
			} else {
				daoSet = new HashMap<>();
			}
		}
		PathResolver.forEachDaoPath(identifier, (path, active) -> {
			if (active) {
				String daoIdentifier = path.getFileName().toString();
				daoSet.computeIfAbsent(daoIdentifier, k -> new DaoBo(this, k));
			}
		});
		return daoSet;
	}
}
