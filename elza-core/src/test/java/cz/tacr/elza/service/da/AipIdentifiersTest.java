package cz.tacr.elza.service.da;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;

import org.archivists.ead3.schema.Archdesc;
import org.archivists.ead3.schema.C;
import org.archivists.ead3.schema.Did;
import org.archivists.ead3.schema.Dsc;
import org.archivists.ead3.schema.Ead;
import org.archivists.ead3.schema.Unitid;
import org.junit.jupiter.api.Test;

public class AipIdentifiersTest {

    private static Did did(String... unitids) {
        Did did = new Did();
        for (String value : unitids) {
            Unitid unitid = new Unitid();
            unitid.getContent().add(value);
            did.getMDid().add(unitid);
        }
        return did;
    }

    private static C c(Did did, C... children) {
        C c = new C();
        c.setDid(did);
        for (C child : children) {
            c.getTheadAndC().add(child);
        }
        return c;
    }

    private static Ead ead(Did archdescDid, C... topLevel) {
        Archdesc archdesc = new Archdesc();
        archdesc.setDid(archdescDid);
        if (topLevel.length > 0) {
            Dsc dsc = new Dsc();
            dsc.getC().addAll(List.of(topLevel));
            archdesc.getAccessrestrictOrAccrualsOrAcqinfo().add(dsc);
        }
        Ead ead = new Ead();
        ead.setArchdesc(archdesc);
        return ead;
    }

    @Test
    void fromEad_leafUnitsIdentifyTheAip() {
        Ead ead = ead(did("aip-uuid"),
                c(did("eSSL:GROUP", "A"),
                        c(did("eSSL:SUBGROUP"),
                                c(did(" eSSL:DOC-1 ", "UKRUK/1/2019")),
                                c(did("eSSL:DOC-2")))));

        Set<String> ids = AipIdentifiers.fromEad(ead);

        assertEquals(List.of("eSSL:DOC-1", "UKRUK/1/2019", "eSSL:DOC-2"), List.copyOf(ids));
    }

    @Test
    void fromEad_withoutUnitsUsesArchdesc() {
        Ead ead = ead(did("aip-uuid", ""));

        assertEquals(Set.of("aip-uuid"), AipIdentifiers.fromEad(ead));
    }

    @Test
    void fromEad_handlesMissingParts() {
        assertTrue(AipIdentifiers.fromEad(null).isEmpty());
        assertTrue(AipIdentifiers.fromEad(new Ead()).isEmpty());
        assertTrue(AipIdentifiers.fromEad(ead(null)).isEmpty());
        assertTrue(AipIdentifiers.fromEad(ead(did("x"), c(null))).isEmpty());
    }
}
