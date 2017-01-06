package cz.tacr.elza.dao.bo.resource;

import java.nio.file.Path;

import cz.tacr.elza.dao.PathResolver;

public class DaoPackageConfigResource extends YamlResource<DaoPackageConfig> {

	private final Path resourcePath;

	public DaoPackageConfigResource(String packageIdentifier) {
		super(DaoPackageConfig.class);
		resourcePath = PathResolver.resolvePackageConfigPath(packageIdentifier);
	}

	@Override
	protected Path getResourcePath() {
		return resourcePath;
	}
}