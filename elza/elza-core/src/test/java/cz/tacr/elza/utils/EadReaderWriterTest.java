package cz.tacr.elza.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.archivists.ead3.schema.Ead;
import org.archivists.ead3.schema.Unitid;
import org.junit.jupiter.api.Test;

import com.lightcomp.kads.common.XmlContentException;

/**
 * Unit tests of the EAD reader: a document that is not an EAD has to be rejected with a
 * description of what it actually is.
 */
public class EadReaderWriterTest {

    private static final String EAD = """
            <?xml version="1.0" encoding="UTF-8"?>
            <ead xmlns="http://ead3.archivists.org/schema/">
                <control>
                    <recordid>ccaa937d</recordid>
                </control>
                <archdesc level="otherlevel" otherlevel="balicek">
                    <did>
                        <unitid>ccaa937d</unitid>
                    </did>
                </archdesc>
            </ead>
            """;

    /**
     * The descriptive metadata of a package the archive did not transform: the original METS
     * of the originating system, carrying the EAD inside instead of being one.
     */
    private static final String METS_WITH_EMBEDDED_EAD = """
            <?xml version="1.0" encoding="UTF-8"?>
            <mets xmlns="http://www.loc.gov/METS/" xmlns:ns3="http://ead3.archivists.org/schema/">
                <amdSec ID="source">
                    <sourceMD ID="AIS_SOURCE">
                        <mdWrap MDTYPE="EAD">
                            <xmlData>
                                <ns3:ead>
                                    <ns3:archdesc level="otherlevel">
                                        <ns3:did><ns3:unitid>ccaa937d</ns3:unitid></ns3:did>
                                    </ns3:archdesc>
                                </ns3:ead>
                            </xmlData>
                        </mdWrap>
                    </sourceMD>
                </amdSec>
            </mets>
            """;

    @Test
    void readsAnEad() throws Exception {
        Ead ead = EadReaderWriter.unmarshal(stream(EAD));

        assertNotNull(ead.getArchdesc());
        assertEquals("otherlevel", ead.getArchdesc().getLevel());
        assertTrue(ead.getArchdesc().getDid().getMDid().stream().anyMatch(o -> o instanceof Unitid));
    }

    @Test
    void rejectsAMetsCarryingAnEadInside() {
        XmlContentException e = assertThrows(XmlContentException.class,
                () -> EadReaderWriter.unmarshal(stream(METS_WITH_EMBEDDED_EAD)));

        // the message has to name both standards, the document is not obviously wrong to read
        assertTrue(e.getMessage().contains("ead"), e.getMessage());
        assertTrue(e.getMessage().contains("mets"), e.getMessage());
        assertTrue(e.getMessage().contains("http://www.loc.gov/METS/"), e.getMessage());
    }

    @Test
    void rejectsAnEadOfAnotherNamespace() {
        XmlContentException e = assertThrows(XmlContentException.class,
                () -> EadReaderWriter.unmarshal(stream("<ead><archdesc/></ead>")));

        assertTrue(e.getMessage().contains("bez jmenného prostoru"), e.getMessage());
    }

    @Test
    void rejectsADocumentThatIsNotXml() {
        assertThrows(XmlContentException.class, () -> EadReaderWriter.unmarshal(stream("nic")));
    }

    @Test
    void rejectsAnEmptyDocument() {
        assertThrows(XmlContentException.class, () -> EadReaderWriter.unmarshal(stream("")));
    }

    private static InputStream stream(String xml) {
        return new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));
    }
}
