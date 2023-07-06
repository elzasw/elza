package cz.tacr.elza.service.dao;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Consumer;
import java.util.stream.Stream;

import cz.tacr.elza.domain.ArrDigitalRepository;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.codes.BaseCode;

/**
 * Image of file system
 * 
 *
 */
public class FileSystemImage {

    final String repoPath;
    private Path rootPath;

    /*!
     * ID of external system definition
     */
    private Integer digiRepId;

    public FileSystemImage(final String repoPath, final ArrDigitalRepository digiRep) {
        this.digiRepId = digiRep.getExternalSystemId();
        this.rootPath = Paths.get(repoPath).toAbsolutePath();
        this.repoPath = rootPath.toString();
        if (!Files.isDirectory(rootPath)) {
            throw new BusinessException("Incorrect path, path: repoPath", BaseCode.INVALID_STATE);
        }
    }

    public Integer getDigiRepId() {
        return digiRepId;
    }

    public String getRepoPath() {
        return this.repoPath;
    }

    public void walk(String filePath, Consumer<? super Path> consumer) throws IOException {
        Path p = Paths.get(repoPath, filePath);
        if (Files.isDirectory(p)) {
            try (Stream<Path> stream = Files.walk(p)) {
                stream.forEach(consumer);
            }
        } else {
            consumer.accept(p);
        }
    }

    public String getRelatPath(Path itemPath) {
        return rootPath.relativize(itemPath).toString();
    }

    public InputStream getInputStream(String filePath) throws IOException {
        Path p = rootPath.resolve(filePath);
        return Files.newInputStream(p);
    }

}
