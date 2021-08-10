package cz.tacr.elza.print;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import cz.tacr.elza.domain.ArrDataUnitdate;
import cz.tacr.elza.domain.convertor.UnitDateConvertor;
import cz.tacr.elza.print.format.Helper;
import cz.tacr.elza.print.item.ItemUnitdate;

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

    @Test
    public void testConvertDate() {
        // jedno datum v různých formátech

        ItemUnitdate itemUD = createItemUnitdate("21.8.1968 8:23:31");
        assertEquals("21. 8. 1968 8:23:31", Helper.convertDate(itemUD));

        itemUD = createItemUnitdate("21.8.1968"); 
        assertEquals("21. 8. 1968", Helper.convertDate(itemUD));

        itemUD = createItemUnitdate("8.1968"); 
        assertEquals("srpen 1968", Helper.convertDate(itemUD));

        itemUD = createItemUnitdate("1968"); 
        assertEquals("1968", Helper.convertDate(itemUD));

        itemUD = createItemUnitdate("[1968]"); 
        assertEquals("[1968]", Helper.convertDate(itemUD));

        itemUD = createItemUnitdate("20st"); 
        assertEquals("20. století", Helper.convertDate(itemUD));

        // časový interval v různých formátech

        itemUD = createItemUnitdate("21.8.1968 0:00:00-21.8.1968 8:23:31"); 
        assertEquals("21. 8. 1968 0:00:00 – 21. 8. 1968 8:23:31", Helper.convertDate(itemUD));

        itemUD = createItemUnitdate("8.1968-1969"); 
        assertEquals("srpen 1968 – 1969", Helper.convertDate(itemUD));

        itemUD = createItemUnitdate("1968-1969"); 
        assertEquals("1968–1969", Helper.convertDate(itemUD));

        itemUD = createItemUnitdate("1968/1969"); 
        assertEquals("[1968]–[1969]", Helper.convertDate(itemUD));

        itemUD = createItemUnitdate("8.1968/1969"); 
        assertEquals("[srpen 1968] – [1969]", Helper.convertDate(itemUD));

        itemUD = createItemUnitdate("6.1968-8.1968"); 
        assertEquals("červen 1968 – srpen 1968", Helper.convertDate(itemUD));

        itemUD = createItemUnitdate("[8.1968]-1969"); 
        assertEquals("[srpen 1968] – 1969", Helper.convertDate(itemUD));

        itemUD = createItemUnitdate("6.1968-[8.1969]"); 
        assertEquals("červen 1968 – [srpen 1969]", Helper.convertDate(itemUD));
    }

    /**
     * Vytvoření objektu ItemUnitdate ze znakového řetězce
     * 
     * @param date
     * @return ItemUnitdate
     */
    private ItemUnitdate createItemUnitdate(String date) {
        UnitDate ud = UnitDateConvertor.convertToUnitDate(date, new UnitDate());
        return new ItemUnitdate(ud); 
    }

}
