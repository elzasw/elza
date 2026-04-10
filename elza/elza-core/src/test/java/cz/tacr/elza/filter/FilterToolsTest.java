package cz.tacr.elza.filter;

import java.util.ArrayList;
import java.util.Arrays;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import cz.tacr.elza.FilterTools;


/**
 * 
 * @since 18.03.2016
 */
public class FilterToolsTest {


    @Test
    public void getSublistTest() {
        ArrayList<Integer> list = new ArrayList<>(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9));

        ArrayList<Integer> sublist = FilterTools.getSublist(0, 2, list);

        for (int i = 0; i < 3; i++) {
            sublist = FilterTools.getSublist(i, 2, list);
            Assertions.assertTrue(sublist.size() == 2);
            Assertions.assertEquals(sublist.get(0), new Integer(i * 2 + 1));
            Assertions.assertEquals(sublist.get(1), new Integer(i * 2 + 2));
        }

        sublist = FilterTools.getSublist(4, 2, list);
        Assertions.assertTrue(sublist.size() == 1);
        Assertions.assertEquals(sublist.get(0), new Integer(9));

        sublist = FilterTools.getSublist(5, 2, list);
        Assertions.assertTrue(sublist.size() == 0);
    }

}
