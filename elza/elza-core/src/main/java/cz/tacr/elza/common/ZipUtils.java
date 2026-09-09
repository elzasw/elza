package cz.tacr.elza.common;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.springframework.web.multipart.MultipartFile;

import cz.tacr.elza.exception.SystemException;

/**
 * Helper class for Zip/Unzip file operations.
 */
public class ZipUtils {

    /**
     * Recognize file type (zip/noZip) by its extension
     * 
     * @param srcFile
     * @return boolean
     */
    public static boolean isZipFile(final MultipartFile srcFile) {
        return srcFile.getOriginalFilename().endsWith(".zip");
    }

    /**
     * The extracted file together with the original entry name it had inside the archive
     * (the temp file on disk has a generated name and cannot carry it).
     */
    public record UnzippedFile(File file, String originalName) {}

    /**
     * Unzipping first file (https://www.baeldung.com/java-compress-and-uncompress)
     *
     * @param srcFile as MultipartFile
     * @return the extracted file and its original entry name, or {@code null} when the input
     *         is not a zip archive
     */
    public static UnzippedFile unzipFirstFile(final MultipartFile srcFile) {
        if (!isZipFile(srcFile)) {
            return null;
        }
        File zipFile = null;
        File unzipFile = null;
        String entryName = null;
        try {
            zipFile = File.createTempFile(srcFile.getName(), ".zip");
            try (BufferedOutputStream outputStream = new BufferedOutputStream(new FileOutputStream(zipFile, false))) {
                IOUtils.copy(srcFile.getInputStream(), outputStream);
            }
            byte[] buffer = new byte[1024];
            try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile))) {
                ZipEntry zipEntry = zis.getNextEntry();
                while (zipEntry != null && unzipFile == null) {
                    entryName = zipEntry.getName();
                    unzipFile = File.createTempFile("elza-unzip-", ".tmp");
                    try (FileOutputStream fos = new FileOutputStream(unzipFile)) {
                        int len;
                        while ((len = zis.read(buffer)) > 0) {
                            fos.write(buffer, 0, len);
                        }
                    }
                    zipEntry = zis.getNextEntry();
                }
                zis.closeEntry();
            }
        } catch (IOException e) {
            throw new SystemException("Failed to create a temporary file to unzip", e);
        } finally {
            if (zipFile != null) {
                zipFile.deleteOnExit();
            }
            if (unzipFile != null) {
                unzipFile.deleteOnExit();
            }
        }

        return unzipFile == null ? null : new UnzippedFile(unzipFile, entryName);
    }
}
