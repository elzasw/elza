package cz.tacr.elza.dao.bo.resource;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import cz.tacr.elza.dao.PathResolver;

public class DaoRequestInfoResource extends YamlResource<DaoRequestInfo> {

	private final Path resourcePath;

	private DaoRequestInfoResource(Path resourcePath) {
		super(DaoRequestInfo.class);
		this.resourcePath = resourcePath;
	}

	public DaoRequestInfoResource(String requestIdentifier, boolean destrRequest) {
		this(PathResolver.resolveRequestInfoPath(requestIdentifier, destrRequest));
	}

	public void delete() throws IOException {
		Files.delete(resourcePath.getParent());
		Files.delete(resourcePath);
		clearCached();
	}

	@Override
	protected Path getResourcePath() {
		return resourcePath;
	}

	public static DaoRequestInfoResource create(DaoRequestInfo daoRequestInfo, boolean destrRequest)
			throws IOException {
		String identifier = Long.toString(System.currentTimeMillis());
		String uniqueName = identifier;
		int tryCount = 0;
		while (tryCount++ < 10) {
			Path path = PathResolver.resolveRequestInfoPath(uniqueName, destrRequest);
			if (!Files.exists(path)) {
				Files.createDirectory(path.getParent());
				try (BufferedWriter bw = Files.newBufferedWriter(path, StandardOpenOption.WRITE,
						StandardOpenOption.CREATE_NEW)) {
					YAML_INSTANCE.dump(daoRequestInfo, bw);
				}
				daoRequestInfo.setIdentifier(uniqueName);
				return new DaoRequestInfoResource(path);
			}
			uniqueName = identifier + '-' + tryCount;
		}
		throw new FileAlreadyExistsException("dao request already exists, identifier:" + identifier);
	}
}