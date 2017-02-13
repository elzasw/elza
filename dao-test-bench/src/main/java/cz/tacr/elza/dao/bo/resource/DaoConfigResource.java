package cz.tacr.elza.dao.bo.resource;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import cz.tacr.elza.dao.common.PathResolver;

public class DaoConfigResource extends YamlResource<DaoConfig> {

	private final Path resourcePath;

	private final Path deleteEntryPath;

	public DaoConfigResource(String packageIdentifier, String daoIdentifier) {
		super(DaoConfig.class);
		resourcePath = PathResolver.resolveDaoConfigPath(packageIdentifier, daoIdentifier);
		deleteEntryPath = PathResolver.resolveDaoDeleteEntryPath(resourcePath.getParent());
	}

	public void delete(String deleteEntry) throws IOException {
		try (BufferedWriter bw = Files.newBufferedWriter(deleteEntryPath,
				StandardOpenOption.WRITE, StandardOpenOption.CREATE_NEW)) {
			bw.write(deleteEntry);
		}
		clearCached();
	}

	@Override
	public Path getResourcePath() {
		return resourcePath;
	}

	@Override
	protected DaoConfig createEmptyResource() {
		return new DaoConfig();
	}

	@Override
	protected DaoConfig loadResource() throws Exception {
		if (Files.exists(deleteEntryPath)) {
			throw new NoSuchFileException(resourcePath.toString());
		}
		return super.loadResource();
	}
}