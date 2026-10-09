package cz.tacr.elza.service.da;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * A component is downloaded from the stored package by the path its root METS gives the file -
 * the stored name may be the original name from PREMIS, which need not be the name in the
 * package nor unique in it.
 */
public class DaComponentEntryTest {

    /** Two files of the same name in two representations, one more with a space in its name. */
    private static final String METS = """
            <?xml version="1.0" encoding="UTF-8"?>
            <mets xmlns="http://www.loc.gov/METS/" xmlns:xlink="http://www.w3.org/1999/xlink" OBJID="uuid-1">
              <amdSec>
                <digiprovMD ID="amd-1">
                  <mdRef LOCTYPE="URL" MDTYPE="PREMIS" xlink:type="simple" xlink:href="metadata/preservation/PACKAGE-INFO.xml"/>
                </digiprovMD>
              </amdSec>
              <fileSec>
                <fileGrp ID="rep-1" USE="Representations/submission">
                  <file ID="file-a"><FLocat LOCTYPE="URL" xlink:type="simple" xlink:href="representations/submission/data/smlouva.pdf"/></file>
                  <file ID="file-space"><FLocat LOCTYPE="URL" xlink:type="simple" xlink:href="representations/submission/data/moje%20smlouva.pdf"/></file>
                </fileGrp>
                <fileGrp ID="rep-2" USE="Representations/access">
                  <file ID="file-b"><FLocat LOCTYPE="URL" xlink:type="simple" xlink:href="./representations/access/data/smlouva.pdf"/></file>
                </fileGrp>
              </fileSec>
              <structMap TYPE="LOGICAL"><div/></structMap>
            </mets>
            """;

    @TempDir
    Path tempDir;

    private ZipFile storedPackage() throws IOException {
        Map<String, String> entries = new LinkedHashMap<>();
        entries.put("aip-1/representations/submission/data/smlouva.pdf", "submission");
        entries.put("aip-1/representations/submission/data/moje smlouva.pdf", "with space");
        entries.put("aip-1/representations/access/data/smlouva.pdf", "access");
        // the METS of the original SIP must not be taken for the root one
        entries.put("aip-1/representations/submission/data/METS.xml", "<mets/>");
        entries.put("aip-1/METS.xml", METS);
        entries.put("aip-1/metadata/preservation/PACKAGE-INFO.xml", "<premis/>");
        Path zip = tempDir.resolve("aip-1.zip");
        try (OutputStream os = Files.newOutputStream(zip); ZipOutputStream zos = new ZipOutputStream(os)) {
            for (Map.Entry<String, String> entry : entries.entrySet()) {
                zos.putNextEntry(new ZipEntry(entry.getKey()));
                zos.write(entry.getValue().getBytes(StandardCharsets.UTF_8));
                zos.closeEntry();
            }
        }
        return new ZipFile(zip.toFile());
    }

    private static String content(ZipFile zipFile, ZipEntry entry) throws IOException {
        return new String(zipFile.getInputStream(entry).readAllBytes(), StandardCharsets.UTF_8);
    }

    @Test
    void filesOfTheSameNameAreToldApartByTheirMetsId() throws IOException {
        try (ZipFile zipFile = storedPackage()) {
            assertEquals("submission", content(zipFile, DaService.componentEntry(zipFile, "file-a")));
            assertEquals("access", content(zipFile, DaService.componentEntry(zipFile, "file-b")));
        }
    }

    @Test
    void aPercentEncodedReferenceFindsTheFileUnderItsDecodedName() throws IOException {
        try (ZipFile zipFile = storedPackage()) {
            assertEquals("with space", content(zipFile, DaService.componentEntry(zipFile, "file-space")));
        }
    }

    @Test
    void aFileTheMetsDoesNotKnowIsReported() throws IOException {
        try (ZipFile zipFile = storedPackage()) {
            AipProblemException e = assertThrows(AipProblemException.class,
                    () -> DaService.componentEntry(zipFile, "file-unknown"));
            assertTrue(e.getMessage().contains("file-unknown"), e.getMessage());
        }
    }
}
