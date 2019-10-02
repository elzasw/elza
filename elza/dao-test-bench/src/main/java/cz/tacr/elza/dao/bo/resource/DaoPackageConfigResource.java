package cz.tacr.elza.dao.bo.resource;

import java.nio.file.Path;

import cz.tacr.elza.dao.common.PathResolver;

public class DaoPackageConfigResource extends YamlResource<DaoPackageConfig> {

	private final Path resourcePath;

	public DaoPackageConfigResource(String packageIdentifier) {
		super(DaoPackageConfig.class);
		resourcePath = PathResolver.resolvePackageConfigPath(packageIdentifier);
	}

	@Override
	public Path getResourcePath() {
		return resourcePath;
	}

	@Override
	protected DaoPackageConfig createEmptyResource() {
		return new DaoPackageConfig();
	}
}