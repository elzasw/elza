package cz.tacr.elza.controller;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

import cz.tacr.elza.controller.vo.ArrFundVersionVO;
import cz.tacr.elza.controller.vo.nodes.ArrNodeVO;
import cz.tacr.elza.test.controller.vo.DataText;
import cz.tacr.elza.test.controller.vo.DataType;
import cz.tacr.elza.test.controller.vo.Fund;
import cz.tacr.elza.test.controller.vo.ItemDataResult;
import cz.tacr.elza.test.controller.vo.NodeItem;

public class DescItemControllerTest extends AbstractControllerTest {

	@Test
	public void createUpdateDeleteDescItemTest() {
		Fund fund = createFund("Test fund name", "internalCode");
		assertNotNull(fund);

        ArrFundVersionVO fundVersion = getOpenVersion(fund);
        List<ArrNodeVO> nodes = createLevels(fundVersion);
        ArrNodeVO rootNode = nodes.get(0);

        // vytvoření hodnoty
        NodeItem nodeItem = buildNodeItem("SRD_SCALE", null, DataType.TEXT, "value", rootNode, null);
        ItemDataResult itemDataResult = descitemsApi.descItemCreateDescItem(fundVersion.getId(), nodeItem);
        NodeItem nodeItemCreated = itemDataResult.getItem();

        assertNotNull(((DataText) nodeItem.getData()).getTextValue().equals(((DataText) nodeItemCreated.getData()).getTextValue()));
        assertNotNull(nodeItemCreated.getPosition());
        assertNotNull(nodeItemCreated.getItemObjectId());

        // aktualizace hodnoty
        ((DataText) nodeItemCreated.getData()).setTextValue("update value");
        itemDataResult = descitemsApi.descItemUpdateDescItem(fundVersion.getId(), true, nodeItemCreated);
        NodeItem nodeItemUpdated = itemDataResult.getItem();

        assertTrue(!nodeItemUpdated.getId().equals(nodeItemCreated.getId()));
        assertTrue(nodeItemUpdated.getItemObjectId().equals(nodeItemCreated.getItemObjectId()));
        assertTrue(nodeItemUpdated.getPosition().equals(nodeItemCreated.getPosition()));
        assertTrue(((DataText) nodeItemUpdated.getData()).getTextValue().equals(((DataText) nodeItemCreated.getData()).getTextValue()));

        // odstranění hodnoty
        itemDataResult = descitemsApi.descItemDeleteDescItem(fundVersion.getId(), nodeItemUpdated);
        NodeItem nodeItemDeleted = itemDataResult.getItem();

        assertTrue(nodeItemDeleted.getId().equals(nodeItemUpdated.getId()));
        assertTrue(nodeItemDeleted.getItemObjectId().equals(nodeItemUpdated.getItemObjectId()));
        assertTrue(nodeItemDeleted.getPosition().equals(nodeItemUpdated.getPosition()));
        assertTrue(((DataText) nodeItemDeleted.getData()).getTextValue().equals(((DataText) nodeItemUpdated.getData()).getTextValue()));
	}

}
