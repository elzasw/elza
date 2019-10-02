package cz.tacr.elza.common;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class AutoDeletingTempFile extends CloseablePathResource {

    /**
     * Flag if resource was released
     */
    boolean released = false;

    public AutoDeletingTempFile(Path resourceFile) {
        super(resourceFile);
    }

    @Override
    public void close() throws IOException {
        if (!released) {
            Files.delete(getPath());
        }
    }

    public static AutoDeletingTempFile createTempFile(String prefix, String postfix) throws IOException {
        File tempFile = File.createTempFile(prefix, postfix);

        return new AutoDeletingTempFile(tempFile.toPath());
    }

    /**
     * Release this resource
     * 
     * @return
     */
    public Path release() {
        released = true;
        return getPath();
    }
}
