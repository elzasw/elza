package cz.tacr.elza.common;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cz.tacr.elza.exception.SystemException;

/**
 * TempFileProvider instance represents folder with temporary files on specified path.
 * Implementation is not thread safe.
 */
public class TempFileProvider {

    private final static Logger logger = LoggerFactory.getLogger(TempFileProvider.class);

    private final static Path SYSTEM_TEMP_DIR_PATH;

    private final Path tmpDirPath;

    static {
        String systemTempDir = System.getProperty("java.io.tmpdir");
        Validate.notEmpty(systemTempDir);
        SYSTEM_TEMP_DIR_PATH = Paths.get(systemTempDir);
        logger.info("System temp directory:{}", systemTempDir);
    }

    public TempFileProvider(String folderPrefix, Path parentPath) {
        this.tmpDirPath = createTempDir(parentPath, folderPrefix);
    }

    public TempFileProvider(String folderPrefix) {
        this(folderPrefix, SYSTEM_TEMP_DIR_PATH);
    }

    public Path createTempFile() {
        try {
            return Files.createTempFile(tmpDirPath, null, null);
        } catch (IOException e) {
            throw new SystemException("Failed to create temporary file", e);
        }
    }

    /**
     * Close deletes all temporary resources created by provider.
     */
    public void close() {
        try {
            boolean emptyDir = deleteTempDirContent();
            if (emptyDir) {
                Files.delete(tmpDirPath);
            }
        } catch (IOException e) {
            if (Files.exists(tmpDirPath)) {
                logger.error("Failed to delete temporary directory, path:{}, detail:{}", tmpDirPath, e.getMessage());
            }
        }
    }

    private boolean deleteTempDirContent() throws IOException {
        try (Stream<Path> deleteStream = Files.list(tmpDirPath)) {
            return deleteStream.map(tmpFilePath -> {
            try {
                Files.delete(tmpFilePath);
                return Boolean.TRUE;
            } catch (IOException e) {
                logger.error("Failed to delete temporary file, path:{}, detail:{}", tmpFilePath, e.getMessage());
                return Boolean.FALSE;
            }
        }).reduce(Boolean.TRUE, Boolean::logicalAnd);
        }
    }

    private static Path createTempDir(Path parentPath, String prefix) {
        try {
            return Files.createTempDirectory(parentPath, prefix);
        } catch (IOException e) {
            throw new SystemException("Failed to create temporary directory", e);
        }
    }
}
