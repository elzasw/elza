package cz.tacr.elza.print;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import cz.tacr.elza.print.format.Helper;

public class HelperTest {

    @Test
    public void testGetParagraph() {
        String r = Helper.getParagraph("a", null, null);
        assertEquals("a", r);
        r = Helper.getParagraph("\na\n", null, null);
        assertEquals("\na\n", r);
        r = Helper.getParagraph("a", 0, 0);
        assertEquals("", r);
        r = Helper.getParagraph("\na\n", 0, 1);
        assertEquals("a", r);
        r = Helper.getParagraph("a\n", 0, 1);
        assertEquals("a", r);
        r = Helper.getParagraph("a\nb\nc\n", 0, 2);
        assertEquals("a\nb", r);
        r = Helper.getParagraph("a\nb\nc\n", 1, null);
        assertEquals("b\nc\n", r);
    }

}
