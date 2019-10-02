package cz.tacr.elza.dao.common;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Stream;

import cz.tacr.elza.dao.DCStorageConfig;

public class PathResolver {

	private static final String YAML_EXTENSION = "yaml";
	private static final String XML_EXTENSION = "xml";
	private static final String PACKAGES_FOLDER_NAME = "packages";
	private static final String DESTRUCTION_REQUESTS_FOLDER_NAME = "destr-requests";
	private static final String TRANSFER_REQUESTS_FOLDER_NAME = "trans-requests";
	private static final String DIGITIZATION_REQUESTS_FOLDER_NAME = "digi-requests";
	private static final String DAO_CONFIG_FILE_NAME = "dao-config." + YAML_EXTENSION;
	private static final String PACKAGE_CONFIG_FILE_NAME = "package-config." + YAML_EXTENSION;
	private static final String EXTERNAL_SYSTEMS_CONFIG_FILE_NAME = "external-systems-config." + YAML_EXTENSION;
	private static final String DELETE_ENTRY_FILE_NAME = "deleted";
	private static final Path STORAGE_PATH;
	static {
		String basePath = DCStorageConfig.get().getBasePath();
		String repositoryIdentifier = DCStorageConfig.get().getRepositoryIdentifier();
		STORAGE_PATH = Paths.get(basePath, repositoryIdentifier);
	}

	public static void forEachDaoFilePath(String packageIdentifier, String daoIdentifier, Consumer<Path> action) {
		Path daoPath = resolveDaoPath(packageIdentifier, daoIdentifier);
		try {
            try (Stream<Path> fileStream = Files.walk(daoPath, 1)) {
                fileStream.filter(p -> !(p.equals(daoPath)
							|| p.endsWith(DAO_CONFIG_FILE_NAME)
							|| p.endsWith(DELETE_ENTRY_FILE_NAME)))
					.forEach(action);
            }
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	public static void forEachDaoPath(String packageIdentifier, Consumer<Path> action) {
		Path packagePath = resolvePackagePath(packageIdentifier);
		DaoVisitor daoVisitor = new DaoVisitor(packagePath, action);
		try {
			Files.walkFileTree(packagePath, daoVisitor);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	public static String getDigitizationRequestInfoFileName(String requestIdentifier) {
		return requestIdentifier + '.' + XML_EXTENSION;
	}

	public static String getDigitizationRequestInfoName(Path requestPath) {
		String fileName = requestPath.getFileName().toString();
		return fileName.substring(0, fileName.length() - XML_EXTENSION.length() - 1);
	}

	public static String getDaoRequestInfoFileName(String requestIdentifier) {
		return requestIdentifier + '.' + YAML_EXTENSION;
	}

	public static String getDaoRequestInfoName(Path requestPath) {
		String fileName = requestPath.getFileName().toString();
		return fileName.substring(0, fileName.length() - YAML_EXTENSION.length() - 1);
	}

	public static Path resolveDigitizationRequestInfoPath(String requestIdentifier) {
		return STORAGE_PATH.resolve(DIGITIZATION_REQUESTS_FOLDER_NAME).resolve(
				getDigitizationRequestInfoFileName(requestIdentifier));
	}

	public static Path resolveDaoRequestInfoPath(String requestIdentifier, boolean destrRequest) {
		return STORAGE_PATH.resolve(resolveDaoRequestInfoRelativePath(requestIdentifier, destrRequest));
	}

	public static Path resolveDaoRequestInfoRelativePath(String requestIdentifier, boolean destrRequest) {
		return Paths.get(destrRequest ? DESTRUCTION_REQUESTS_FOLDER_NAME : TRANSFER_REQUESTS_FOLDER_NAME,
				getDaoRequestInfoFileName(requestIdentifier));
	}

	public static Path resolveExternalSystemsConfigPath() {
		return STORAGE_PATH.resolve(EXTERNAL_SYSTEMS_CONFIG_FILE_NAME);
	}

	public static Path resolveDaoFilePath(String packageIdentifier, String daoIdentifier, String fileIdentifier) {
		return resolveDaoPath(packageIdentifier, daoIdentifier).resolve(fileIdentifier);
	}

	public static Path resolveDaoDeleteEntryPath(Path daoPath) {
		return daoPath.resolve(DELETE_ENTRY_FILE_NAME);
	}

	public static Path resolveDaoConfigPath(String packageIdentifier, String daoIdentifier) {
		return resolveDaoPath(packageIdentifier, daoIdentifier).resolve(DAO_CONFIG_FILE_NAME);
	}

	public static Path resolveDaoPath(String packageIdentifier, String daoIdentifier) {
		return resolvePackagePath(packageIdentifier).resolve(daoIdentifier);
	}

	public static Path resolvePackageConfigPath(String packageIdentifier) {
		return resolvePackagePath(packageIdentifier).resolve(PACKAGE_CONFIG_FILE_NAME);
	}

	public static Path resolvePackagePath(String packageIdentifier) {
		return STORAGE_PATH.resolve(PACKAGES_FOLDER_NAME).resolve(packageIdentifier);
	}

	public static Path createDaoRequestInfoPath(boolean destrRequest, int tryCount) throws FileAlreadyExistsException {
		Path parent = STORAGE_PATH.resolve(destrRequest ? DESTRUCTION_REQUESTS_FOLDER_NAME : TRANSFER_REQUESTS_FOLDER_NAME);
		return createUniqueTimestampPath(parent, YAML_EXTENSION, tryCount);
	}

	public static Path createDigitizationRequestInfoPath(int tryCount) throws FileAlreadyExistsException {
		Path parent = STORAGE_PATH.resolve(DIGITIZATION_REQUESTS_FOLDER_NAME);
		return createUniqueTimestampPath(parent, XML_EXTENSION, tryCount);
	}

	private static Path createUniqueTimestampPath(Path parent, String fileExtension, int tryCount)
			throws FileAlreadyExistsException {
		String timestamp = Long.toString(System.currentTimeMillis());
		String uniqueName = timestamp;
		for (int i = 1; i <= tryCount; i++) {
			if (fileExtension != null) {
				uniqueName += '.' + fileExtension;
			}
			Path tmpPath = parent.resolve(uniqueName);
			if (!Files.exists(tmpPath)) {
				return tmpPath;
			}
			uniqueName = timestamp + '-' + tryCount;
		}
		throw new FileAlreadyExistsException(parent.toString());
	}

	private static class DaoVisitor extends SimpleFileVisitor<Path> {

		private final Set<Path> deletedDaoPaths = new HashSet<>();

		private final Path packagePath;

		private final Consumer<Path> action;

		public DaoVisitor(Path packagePath, Consumer<Path> action) {
			this.packagePath = packagePath;
			this.action = action;
		}

		@Override
		public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
			if (isDaoDirectory(dir) || dir.equals(packagePath)) {
				return FileVisitResult.CONTINUE;
			} else {
				return FileVisitResult.SKIP_SUBTREE;
			}
		}

		@Override
		public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
			if (file.endsWith(DELETE_ENTRY_FILE_NAME) && isDaoDirectory(file.getParent())) {
				deletedDaoPaths.add(file.getParent());
				return FileVisitResult.SKIP_SIBLINGS;
			}
			return FileVisitResult.CONTINUE;
		}

		@Override
		public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
			if (exc != null) {
				throw exc;
			}
			if (isDaoDirectory(dir) && !deletedDaoPaths.contains(dir)) {
				action.accept(dir);
			}
			return FileVisitResult.CONTINUE;
		}

		private boolean isDaoDirectory(Path dir) {
			return dir.getParent().equals(packagePath);
		}
	}
}