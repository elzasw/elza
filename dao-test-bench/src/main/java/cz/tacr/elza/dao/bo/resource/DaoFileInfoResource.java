package cz.tacr.elza.dao.bo.resource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Date;

import cz.tacr.elza.dao.common.PathResolver;

public class DaoFileInfoResource extends AbstractStorageResource<DaoFileInfo> {

	private final Path resourcePath;

	public DaoFileInfoResource(String packageIdentifier, String daoIdentifier, String fileIdentifier) {
		resourcePath = PathResolver.resolveDaoFilePath(packageIdentifier, daoIdentifier, fileIdentifier);
	}

	@Override
	protected DaoFileInfo loadResource() throws IOException {
		DaoFileInfo info = new DaoFileInfo();
		info.setFilePath(resourcePath);
		info.setMimeType(Files.probeContentType(resourcePath));
		BasicFileAttributes attrs = Files.readAttributes(resourcePath, BasicFileAttributes.class);
		info.setCreated(new Date(attrs.creationTime().toMillis()));
		info.setSize(attrs.size());
		return info;
	}
}