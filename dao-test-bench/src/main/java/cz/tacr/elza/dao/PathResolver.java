package cz.tacr.elza.dao;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;

public class PathResolver implements EnvironmentAware {

	private static final String PACKAGES_FOLDER_NAME = "packages";
	private static final String DESTRUCTION_REQUEST_FOLDER_NAME = "destr-requests";
	private static final String TRANSFER_REQUEST_FOLDER_NAME = "trans-requests";
	private static final String DAO_CONFIG_FILE_NAME = "dao-config.yaml";
	private static final String PACKAGE_CONFIG_FILE_NAME = "package-config.yaml";
	private static final String REQUEST_INFO_FILE_NAME = "request-info.yaml";
	private static final String DELETED_FILE_NAME = "deleted";

	private static Environment environment;

	@Override
	public void setEnvironment(Environment environment) {
		PathResolver.environment = environment;
	}

	public static void forEachDaoFilePath(String packageIdentifier, String daoIdentifier, Consumer<Path> listener) {
		Path daoPath = resolveDaoPath(packageIdentifier, daoIdentifier);
		try {
			Files.walk(daoPath, 1)
					.filter(p -> !(p.equals(daoPath)
							|| p.endsWith(DAO_CONFIG_FILE_NAME)
							|| p.endsWith(DELETED_FILE_NAME)))
					.forEach(listener);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	public static void forEachDaoPath(String packageIdentifier, DaoPathListener listener) {
		Path packagePath = resolvePackagePath(packageIdentifier);
		DaoVisitor daoVisitor = new DaoVisitor(packagePath, listener);
		try {
			Files.walkFileTree(packagePath, daoVisitor);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	public static Path resolveDaoFilePath(String packageIdentifier, String daoIdentifier, String fileIdentifier) {
		return resolveDaoPath(packageIdentifier, daoIdentifier).resolve(fileIdentifier);
	}

	public static Path resolveRequestInfoPath(String requestIdentifier, boolean destrRequest) {
		String requestFolder = destrRequest ? DESTRUCTION_REQUEST_FOLDER_NAME : TRANSFER_REQUEST_FOLDER_NAME;
		return getStoragePath().resolve(requestFolder).resolve(requestIdentifier).resolve(REQUEST_INFO_FILE_NAME);
	}

	public static Path resolvePackageConfigPath(String packageIdentifier) {
		return resolvePackagePath(packageIdentifier).resolve(PACKAGE_CONFIG_FILE_NAME);
	}

	public static Path resolveDaoConfigPath(String packageIdentifier, String daoIdentifier) {
		return resolveDaoPath(resolvePackagePath(packageIdentifier), daoIdentifier).resolve(DAO_CONFIG_FILE_NAME);
	}

	public static Path resolveDaoPath(String packageIdentifier, String daoIdentifier) {
		return resolveDaoPath(resolvePackagePath(packageIdentifier), daoIdentifier);
	}

	public static Path resolvePackagePath(String packageIdentifier) {
		return getStoragePath().resolve(PACKAGES_FOLDER_NAME).resolve(packageIdentifier);
	}

	private static Path resolveDaoPath(Path packagePath, String daoIdentifier) {
		return packagePath.resolve(daoIdentifier);
	}

	private static Path getStoragePath() {
		String basePath = environment.getProperty(DCStorageApp.BASE_PATH_PARAM_NAME);
		String repositoryIdentifier = environment.getProperty(DCStorageApp.REPOSITORY_IDENTIFIER_PARAM_NAME);
		return Paths.get(basePath, repositoryIdentifier);
	}

	public interface DaoPathListener {

		void process(Path path, boolean active);
	}

	private static class DaoVisitor extends SimpleFileVisitor<Path> {

		private final Set<Path> deletedDaoPaths = new HashSet<>();

		private final Path packagePath;

		private final DaoPathListener listener;

		public DaoVisitor(Path packagePath, DaoPathListener listener) {
			this.packagePath = packagePath;
			this.listener = listener;
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
			if (file.endsWith(DELETED_FILE_NAME) && isDaoDirectory(file.getParent())) {
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
			if (isDaoDirectory(dir)) {
				listener.process(dir, !deletedDaoPaths.contains(dir));
			}
			return FileVisitResult.CONTINUE;
		}

		private boolean isDaoDirectory(Path dir) {
			return dir.getParent().equals(packagePath);
		}
	}
}
