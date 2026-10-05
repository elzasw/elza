package cz.tacr.elza.service.da;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import gov.loc.mets.v1_11.schema.MetsType;

public class AipPackageTypeTest {

    private static final String PROFILE = "https://stands.nacr.cz/da/2023/aip.xml";

    @Test
    void other_isReplacedByTheOtherContentType() {
        AipPackageType type = AipPackageType.of(mets("OTHER", "NSESSS", PROFILE));

        assertEquals("NSESSS", type.contentType());
        assertEquals(PROFILE, type.profile());
    }

    @Test
    void vocabularyValue_isKept_andOtherContentTypeIgnored() {
        assertEquals("citssiard", AipPackageType.of(mets("citssiard", "NSESSS", null)).contentType());
    }

    @Test
    void otherWithoutOtherContentType_staysOther() {
        assertEquals("OTHER", AipPackageType.of(mets("OTHER", null, null)).contentType());
    }

    @Test
    void nothingDeclared() {
        AipPackageType type = AipPackageType.of(mets(null, "NSESSS", " "));

        assertNull(type.contentType());
        assertNull(type.profile());
    }

    @Test
    void tooLongValue_isAProblemOfThePackage() {
        assertThrows(AipProblemException.class, () -> AipPackageType.of(mets("x".repeat(251), null, null)));
    }

    private static MetsType mets(String contentType, String otherContentType, String profile) {
        MetsType mets = new MetsType();
        if (contentType != null) {
            mets.getOtherAttributes().put(AipPackageType.CONTENT_INFORMATION_TYPE, contentType);
        }
        if (otherContentType != null) {
            mets.getOtherAttributes().put(AipPackageType.OTHER_CONTENT_INFORMATION_TYPE, otherContentType);
        }
        mets.setPROFILE(profile);
        return mets;
    }
}
