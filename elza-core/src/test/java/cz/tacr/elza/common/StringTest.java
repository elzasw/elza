package cz.tacr.elza.common;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import cz.tacr.elza.common.string.PrepareForCompare;

public class StringTest {
    @Test
    public void prepareForCompareTest() {
        // Empty list
        List<String> list1 = new ArrayList<>();
        String result1 = PrepareForCompare.prepare(list1);
        assertEquals(result1, "");

        // Single item
        List<String> list2 = new ArrayList<>();
        list2.add("A");
        String result2 = PrepareForCompare.prepare(list2);
        assertEquals(result2, "a");

        // Single item + whitespace
        List<String> list3 = new ArrayList<>();
        list3.add("  A\t\t\r\n");
        String result3 = PrepareForCompare.prepare(list3);
        assertEquals(result3, "a");

        // Two items
        List<String> list4 = new ArrayList<>();
        list4.add("A b");
        String result4 = PrepareForCompare.prepare(list4);
        assertEquals(result4, "a b");

        // Multiple items
        List<String> list5 = new ArrayList<>();
        list5.add("A-b c,de, Fgh");
        String result5 = PrepareForCompare.prepare(list5);
        assertEquals(result5, "a b c de fgh");

        // Multiple items
        List<String> list6 = new ArrayList<>();
        list6.add("A-b c");
        list6.add("de");
        list6.add("Fgh");
        String result6 = PrepareForCompare.prepare(list6);
        assertEquals(result6, "a b c de fgh");
    }

}
