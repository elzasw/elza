package cz.tacr.elza.service.da;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.archivists.ead3.schema.Container;
import org.archivists.ead3.schema.Dao;
import org.archivists.ead3.schema.Daterange;
import org.archivists.ead3.schema.Unitdatestructured;
import org.archivists.ead3.schema.Unittitle;
import org.junit.jupiter.api.Test;

public class DidElementConvertersTest {

    @Test
    void inherited_isRecognizedByAltrender() {
        Unitdatestructured inherited = unitdate(null);
        inherited.setAltrender("inherited");

        assertTrue(DidElementConverters.isInherited(inherited));
        assertFalse(DidElementConverters.isInherited(unitdate(null)));
    }

    @Test
    void localTypeOfDate_isTheLocalTypeOfItsRange() {
        assertEquals("CONTENT", DidElementConverters.localType(unitdate("CONTENT")));
        assertNull(DidElementConverters.localType(unitdate(null)));
        assertNull(DidElementConverters.localType(unitdate(" ")));
        assertNull(DidElementConverters.localType(new Unitdatestructured()));
    }

    @Test
    void localTypeOfTitle() {
        Unittitle title = new Unittitle();
        title.setLocaltype("FORMAL_TITLE");

        assertEquals("FORMAL_TITLE", DidElementConverters.localType(title));
    }

    @Test
    void unsupportedElement() {
        Dao dao = new Dao();

        assertFalse(DidElementConverters.isSupported(dao));
        assertFalse(DidElementConverters.isInherited(dao));
        assertNull(DidElementConverters.localType(dao));
        assertNull(DidElementConverters.text(dao));
    }

    @Test
    void containerText_isNormalized() {
        Container container = new Container();
        container.getContent().add("\n   samostatně\n   1 ");

        assertTrue(DidElementConverters.isSupported(container));
        assertEquals("samostatně 1", DidElementConverters.text(container));
        assertNull(DidElementConverters.text(new Unitdatestructured()), "a structured date has no text");
    }

    private static Unitdatestructured unitdate(String localType) {
        Daterange range = new Daterange();
        range.setLocaltype(localType);
        Unitdatestructured unitdate = new Unitdatestructured();
        unitdate.setDaterange(range);
        return unitdate;
    }
}
