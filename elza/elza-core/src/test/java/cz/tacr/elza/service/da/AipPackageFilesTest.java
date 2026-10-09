package cz.tacr.elza.service.da;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.lightcomp.kads.mets.MetsReaderWriter;

import gov.loc.mets.v1_11.schema.MetsType;

/**
 * The files of a package are found where its root METS puts them, under any name - the rules
 * fix only the root METS.xml and PACKAGE-INFO.xml.
 */
public class AipPackageFilesTest {

    /** A package whose PREMIS file is named in lower case, as a partner's producer writes it. */
    private static final String METS = """
            <?xml version="1.0" encoding="UTF-8"?>
            <mets xmlns="http://www.loc.gov/METS/" xmlns:xlink="http://www.w3.org/1999/xlink" OBJID="uuid-1">
              <dmdSec ID="dmd-1">
                <mdRef LOCTYPE="URL" MDTYPE="EAD" xlink:type="simple" xlink:href="metadata/descriptive/EAD-INHERENT.xml"/>
              </dmdSec>
              <amdSec>
                <digiprovMD ID="amd-1">
                  <mdRef LOCTYPE="URL" MDTYPE="PREMIS" xlink:type="simple" xlink:href="metadata/preservation/PACKAGE-INFO.xml"/>
                </digiprovMD>
                <digiprovMD ID="amd-2">
                  <mdRef LOCTYPE="URL" MDTYPE="PREMIS" xlink:type="simple" xlink:href="metadata/preservation/premis.xml"/>
                </digiprovMD>
              </amdSec>
              <fileSec>
                <fileGrp ID="rep-1" USE="Representations/submission">
                  <fileGrp ID="rep-1-data">
                    <file ID="file-1">
                      <FLocat LOCTYPE="URL" xlink:type="simple" xlink:href="representations/submission/data/smlouva.pdf"/>
                    </file>
                  </fileGrp>
                </fileGrp>
              </fileSec>
              <structMap TYPE="LOGICAL"><div/></structMap>
            </mets>
            """;

    @TempDir
    Path tempDir;

    private static MetsType mets(String xml) throws Exception {
        try (InputStream in = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8))) {
            return MetsReaderWriter.unmarshal(in);
        }
    }

    private Path write(Path root, String relative, String content) throws IOException {
        Path file = root.resolve(relative);
        Files.createDirectories(file.getParent());
        Files.writeString(file, content);
        return file;
    }

    @Test
    void premisFilesAreTakenFromTheMetsWhateverTheirName() throws Exception {
        assertEquals(List.of("metadata/preservation/PACKAGE-INFO.xml", "metadata/preservation/premis.xml"),
                     AipPackageFiles.premisHrefs(mets(METS)));
    }

    /** The second PREMIS file of an AIS eARK Convert package is not called PREMIS.xml at all. */
    @Test
    void aProducerMayNameItsPremisFileDifferently() throws Exception {
        try (InputStream in = getClass().getResourceAsStream("/zp2015/da-volne/a/METS.xml")) {
            assertEquals(List.of("metadata/preservation/PACKAGE-INFO.xml", "metadata/preservation/RETENTION-METADATA.xml"),
                         AipPackageFiles.premisHrefs(MetsReaderWriter.unmarshal(in)));
        }
    }

    @Test
    void metadataHrefsCoverDescriptiveAndAdministrativeSections() throws Exception {
        assertEquals(List.of("metadata/descriptive/EAD-INHERENT.xml", "metadata/preservation/PACKAGE-INFO.xml",
                             "metadata/preservation/premis.xml"),
                     AipPackageFiles.metadataHrefs(mets(METS)));
    }

    @Test
    void hrefOfFindsFilesInNestedGroupsAndMetadataSections() throws Exception {
        MetsType mets = mets(METS);
        assertEquals("representations/submission/data/smlouva.pdf", AipPackageFiles.hrefOf(mets, "file-1"));
        assertEquals("metadata/preservation/premis.xml", AipPackageFiles.hrefOf(mets, "amd-2"));
        assertEquals("metadata/descriptive/EAD-INHERENT.xml", AipPackageFiles.hrefOf(mets, "dmd-1"));
        assertNull(AipPackageFiles.hrefOf(mets, "unknown"));
    }

    /**
     * The METS of the original SIP lies in a representation; a search by name could find it
     * before the root one, depending on the file system.
     */
    @Test
    void theRootMetsIsTheOneAtTheTopOfThePackageDirectory() throws Exception {
        Path root = tempDir.resolve("aip-code");
        write(root, "representations/submission/data/METS.xml", "<sip/>");
        Path rootMets = write(root, "METS.xml", METS);

        assertEquals(root, AipPackageFiles.packageRoot(tempDir));
        assertEquals(rootMets, AipPackageFiles.mets(AipPackageFiles.packageRoot(tempDir)));
    }

    @Test
    void aPackageWithoutRootMetsIsReported() throws Exception {
        Path root = tempDir.resolve("aip-code");
        write(root, "representations/submission/data/METS.xml", "<sip/>");

        AipProblemException e = assertThrows(AipProblemException.class,
                () -> AipPackageFiles.mets(AipPackageFiles.packageRoot(tempDir)));
        assertEquals("Balíček neobsahuje soubor METS.xml", e.getMessage());
    }

    @Test
    void packageInfoIsFoundInTheMetadataPackageAndAlone() throws Exception {
        Path metadataPackage = tempDir.resolve("a");
        Path inMetadata = write(metadataPackage, AipPackageFiles.PACKAGE_INFO, "<premis/>");
        Path packageInfoBatch = tempDir.resolve("b");
        Path alone = write(packageInfoBatch, "PACKAGE-INFO.xml", "<premis/>");

        assertEquals(inMetadata, AipPackageFiles.packageInfo(metadataPackage));
        assertEquals(alone, AipPackageFiles.packageInfo(packageInfoBatch));
    }

    @Test
    void aReferencedFileThePackageLacksIsReportedByItsPath() throws Exception {
        AipProblemException e = assertThrows(AipProblemException.class,
                () -> AipPackageFiles.referenced(tempDir, "metadata/preservation/premis.xml"));
        assertTrue(e.getMessage().contains("metadata/preservation/premis.xml"), e.getMessage());
    }

    /** A zip entry or a METS reference must not reach outside the directory of the package. */
    @Test
    void pathsOutsideThePackageAreRefused() {
        assertThrows(AipProblemException.class, () -> AipPackageFiles.resolveInside(tempDir, "../evil.xml"));
        assertThrows(AipProblemException.class, () -> AipPackageFiles.resolveInside(tempDir, "a/../../evil.xml"));
        assertEquals(tempDir.toAbsolutePath().normalize().resolve("a/b.xml"),
                     AipPackageFiles.resolveInside(tempDir, "a/./b.xml"));
    }

    /** The check a metadata package passes before it is stored asks for the files the METS names. */
    @Test
    void aMetadataPackageIsCompleteWithItsPremisInLowerCase() throws Exception {
        Path root = tempDir.resolve("aip-code");
        write(root, "METS.xml", METS);
        write(root, "metadata/descriptive/EAD-INHERENT.xml", "<ead/>");
        write(root, AipPackageFiles.PACKAGE_INFO, "<premis/>");
        write(root, "metadata/preservation/premis.xml", "<premis/>");

        DaService.checkMetadataFiles(root);

        Files.delete(root.resolve("metadata/preservation/premis.xml"));
        AipProblemException e = assertThrows(AipProblemException.class, () -> DaService.checkMetadataFiles(root));
        assertTrue(e.getMessage().contains("metadata/preservation/premis.xml"), e.getMessage());
    }

    @Test
    void theRootMetsEntryOfAStoredPackageIsRecognised() {
        assertTrue(DaService.isRootMets("aip-code/METS.xml"));
        assertTrue(DaService.isRootMets("METS.xml"));
        assertEquals(false, DaService.isRootMets("aip-code/representations/submission/data/METS.xml"));
        assertEquals(false, DaService.isRootMets("aip-code/SIP-METS.xml"));
    }
}
