package cz.tacr.elza.utils;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

/**
 * Podpora pro práci s temp adresářem - jeho založení, mazání při exit WM apod.
 *
 * @author Pavel Stánek [pavel.stanek@marbes.cz]
 * @since 14.11.2017
 */
public class TempDirectory {
    final Path path;

    public TempDirectory(final String prefix) {
        try {
            path = Files.createTempDirectory(prefix);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public Path getPath() {
        return path;
    }

    public void delete() {
        delete(path.toString());
    }

    public static void delete(String pathStr) {
        final Path path = Paths.get(pathStr);
        if (!Files.exists(path)) {
            return;
        }
        try {
            Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc)
                        throws IOException {
                    Files.deleteIfExists(dir);
                    return super.postVisitDirectory(dir, exc);
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                        throws IOException {
                    if (!Files.deleteIfExists(file)) {
                        file.toFile().deleteOnExit();
                    }
                    return super.visitFile(file, attrs);
                }
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
