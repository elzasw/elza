package cz.tacr.elza.print;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.domain.ArrStructuredObject;
import cz.tacr.elza.domain.RulItemType;
import cz.tacr.elza.print.format.Formatter;
import cz.tacr.elza.print.item.Item;
import cz.tacr.elza.print.item.ItemInteger;
import cz.tacr.elza.print.item.ItemStructuredRef;
import cz.tacr.elza.print.item.ItemType;

public class FormatterTest {

    @Test
    public void testFormatter1() {
        Formatter formatter = new Formatter();
        formatter.setBlockSeparators("/","")
                .setItemSeparator(", ")
                .addValue("ZP2015_STORAGE_ID")
                .beginBlock().addValue("ZP2015_ITEM_ORDER").endBlock();

        List<Item> items = new ArrayList<>();

        ArrStructuredObject structObjEntity = new ArrStructuredObject();
        structObjEntity.setValue("k100");
        structObjEntity.setSortValue("k000100");
        Structured sobj = Structured.newInstance(structObjEntity, null);

        RulItemType rulType1 = new RulItemType();
        rulType1.setCode("ZP2015_STORAGE_ID");
        ItemType type1 = new ItemType(rulType1, DataType.STRUCTURED);
        ItemStructuredRef sobjRef = new ItemStructuredRef(sobj);
        sobjRef.setType(type1);
        items.add(sobjRef);

        RulItemType rulType2 = new RulItemType();
        rulType2.setCode("ZP2015_ITEM_ORDER");
        ItemType type2 = new ItemType(rulType2, DataType.INT);
        ItemInteger itemInt = new ItemInteger(5);
        itemInt.setType(type2);
        items.add(itemInt);

        String s = formatter.format(items);
        assertEquals("k100/5", s);
    }
}
