package cz.tacr.elza.dao.bo.resource;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import cz.tacr.elza.dao.common.PathResolver;
import cz.tacr.elza.dao.exception.DaoComponentException;

public class DaoRequestInfoResource extends YamlResource<DaoRequestInfo> {

	private final Path resourcePath;

	private DaoRequestInfoResource(Path resourcePath) {
		super(DaoRequestInfo.class);
		this.resourcePath = resourcePath;
	}

	public DaoRequestInfoResource(String requestIdentifier, boolean destrRequest) {
		this(PathResolver.resolveDaoRequestInfoPath(requestIdentifier, destrRequest));
	}

	public String getIdentifier() {
		return PathResolver.getDaoRequestInfoName(resourcePath);
	}

	@Override
	public Path getResourcePath() {
		return resourcePath;
	}

	@Override
	protected DaoRequestInfo createEmptyResource() {
		throw new DaoComponentException("empty dao request info: " + resourcePath);
	}

	public static DaoRequestInfoResource create(DaoRequestInfo daoRequestInfo, boolean destrRequest)
			throws IOException {
		Path filePath = PathResolver.createDaoRequestInfoPath(destrRequest, 10);
		Path dirPath = filePath.getParent();
		Files.createDirectories(dirPath);
		try (BufferedWriter bw = Files.newBufferedWriter(filePath, StandardOpenOption.WRITE,
				StandardOpenOption.CREATE_NEW)) {
			YAML_INSTANCE.dump(daoRequestInfo, bw);
		}
		return new DaoRequestInfoResource(filePath);
	}
}
