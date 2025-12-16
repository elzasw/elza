package cz.tacr.elza.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;

import org.junit.Test;

import cz.tacr.elza.controller.vo.ApAccessPointVO;
import cz.tacr.elza.controller.vo.ArrFundVersionVO;
import cz.tacr.elza.controller.vo.nodes.ArrNodeVO;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.test.controller.vo.DataType;
import cz.tacr.elza.test.controller.vo.DescItemField;
import cz.tacr.elza.test.controller.vo.FieldValueFilter;
import cz.tacr.elza.test.controller.vo.Fund;
import cz.tacr.elza.test.controller.vo.MultimatchContainsFilter;
import cz.tacr.elza.test.controller.vo.NodeData;
import cz.tacr.elza.test.controller.vo.NodeDataParam;
import cz.tacr.elza.test.controller.vo.NodeField;
import cz.tacr.elza.test.controller.vo.NodeFieldName;
import cz.tacr.elza.test.controller.vo.NodeItem;
import cz.tacr.elza.test.controller.vo.NodeSearchResult;
import cz.tacr.elza.test.controller.vo.NodeTreeData;
import cz.tacr.elza.test.controller.vo.OperationCompareType;
import cz.tacr.elza.test.controller.vo.SearchParams;

public class NodeControllerTest extends AbstractControllerTest {

    @Test
    public void nodeSearchTest() {
        Fund fund = createFund("fund1", "internalCode");
        assertNotNull(fund);

        // create levels (nodes)
        ArrFundVersionVO fundVersion = getOpenVersion(fund);
        List<ArrNodeVO> nodes = createLevels(fundVersion);
        ArrNodeVO node = nodes.get(0);

        // create item by SRD_TITLE
        NodeItem nodeItem = buildNodeItem(SRD_TITLE, null, DataType.TEXT, "value", node);
        descitemsApi.descItemCreateDescItem(fundVersion.getId(), nodeItem);

        // create item by SRD_SERIAL_NUMBER
        nodeItem = buildNodeItem(SRD_SERIAL_NUMBER, null, DataType.INT, 1, node);
        descitemsApi.descItemCreateDescItem(fundVersion.getId(), nodeItem);

        // create item by SRD_UNIT_DATE
        nodeItem = buildNodeItem(SRD_UNIT_DATE, null, DataType.UNITDATE, "15.5.2025", node);
        descitemsApi.descItemCreateDescItem(fundVersion.getId(), nodeItem);

        // create item by SRD_OTHER_ID
        nodeItem = buildNodeItem(SRD_OTHER_ID, SRD_OTHERID_CJ, DataType.STRING, "13", node);
        descitemsApi.descItemCreateDescItem(fundVersion.getId(), nodeItem);

        // create item by SRD_LANGUAGE
        nodeItem = buildNodeItem(SRD_LANGUAGE, SRD_LANGUAGE_1, DataType.ENUM, null, node);
        descitemsApi.descItemCreateDescItem(fundVersion.getId(), nodeItem);

        // prepare to create item by SRD_ENTITY_ROLE
        List<ApAccessPointVO> accessPoints = findRecord(null, null, null, null, null);
        ApAccessPointVO accessPoint = accessPoints.get(0);
        // create item by SRD_ENTITY_ROLE
        nodeItem = buildNodeItem(SRD_ENTITY_ROLE, SRD_ENTITY_ROLE_1, DataType.RECORD_REF, accessPoint, node);
        descitemsApi.descItemCreateDescItem(fundVersion.getId(), nodeItem);

        // create MultimatchContainsFilter filter
        MultimatchContainsFilter containsFilter = new MultimatchContainsFilter();
        containsFilter.setValue("value");

        // create search params
        SearchParams params = new SearchParams();
        params.addFiltersItem(containsFilter);

        // waiting for reindexing to get result
        NodeSearchResult fundResult = null;
        int counter = 0;
        try {
            do {
                Thread.sleep(100);
                fundResult = nodeApi.nodeSearch(params);
                counter++;
            } while (fundResult.getFonds().size() == 0 && counter < 1000);
        } catch (Exception e) {
            fail("Exception while waiting on result: " + e);
        }
        assertEquals(1, fundResult.getFonds().size());

    	List<NodeTreeData> nodeResult = nodeApi.nodeGetSearchResult(fund.getId());
    	assertEquals(1, nodeResult.size());

        // create FieldValueFilter filter
        FieldValueFilter valueFilter = new FieldValueFilter();
        valueFilter.setField(new DescItemField().typeCode(SRD_TITLE));
        valueFilter.setValue("alu");
        valueFilter.setOperation(OperationCompareType.CONTAINS);

        // set FieldValueFilter in params
        params.filters(List.of(valueFilter));

        // try to search text using FieldValueFilter
        fundResult = nodeApi.nodeSearch(params);
        assertEquals(1, fundResult.getFonds().size());

        // change filter by SRD_SERIAL_NUMBER
        valueFilter.setField(new DescItemField().typeCode(SRD_SERIAL_NUMBER));
        valueFilter.setValue("1");
        valueFilter.setOperation(OperationCompareType.EQ);

        // try to search serial number using FieldValueFilter
        fundResult = nodeApi.nodeSearch(params);
        assertEquals(1, fundResult.getFonds().size());

        // change filter by SRD_UNIT_DATE
        valueFilter.setField(new DescItemField().typeCode(SRD_UNIT_DATE));
        valueFilter.setValue("15.5.2025");
        valueFilter.setOperation(OperationCompareType.EQ);

        // try to search unitdate using FieldValueFilter
        fundResult = nodeApi.nodeSearch(params);
        assertEquals(1, fundResult.getFonds().size());

        // change filter by SRD_OTHER_ID
        valueFilter.setField(new DescItemField().typeCode(SRD_OTHER_ID).specCode(SRD_OTHERID_CJ));
        valueFilter.setValue("13");
        valueFilter.setOperation(OperationCompareType.CONTAINS);

        // try to search otherId using FieldValueFilter
        fundResult = nodeApi.nodeSearch(params);
        assertEquals(1, fundResult.getFonds().size());

        // change filter by SRD_LANGUAGE
        valueFilter.setField(new DescItemField().typeCode(SRD_LANGUAGE).specCode(SRD_LANGUAGE_1));
        valueFilter.setValue(null);
        valueFilter.setOperation(OperationCompareType.EQ);

        // try to search language using FieldValueFilter
        fundResult = nodeApi.nodeSearch(params);
        assertEquals(1, fundResult.getFonds().size());

        // change filter by RECORD_REF search by id
        valueFilter.setField(new DescItemField().typeCode(SRD_ENTITY_ROLE).specCode(SRD_ENTITY_ROLE_1));
        valueFilter.setValue(accessPoint.getId().toString());
        valueFilter.setOperation(OperationCompareType.EQ);

        // try to search by AP name using FieldValueFilter
        fundResult = nodeApi.nodeSearch(params);
        assertEquals(1, fundResult.getFonds().size());

        // change filter by RECORD_REF search by apId
        valueFilter.setValue(accessPoint.getId().toString());
        valueFilter.setOperation(OperationCompareType.EQ);

        // try to search by AP id using FieldValueFilter
        fundResult = nodeApi.nodeSearch(params);
        assertEquals(1, fundResult.getFonds().size());

        // get ArrNode for uuid
        ArrNode arrNode = nodeRepository.findById(node.getId()).get();

        // try to search by NODE_FIELD field type
        valueFilter.setField(new NodeField().fieldName(NodeFieldName.UUID));
        valueFilter.setValue(arrNode.getUuid());
        valueFilter.setOperation(OperationCompareType.EQ);

        // try to search by UUID id using FieldValueFilter
        fundResult = nodeApi.nodeSearch(params);
        assertEquals(1, fundResult.getFonds().size());
    }

	@Test
    public void nodeGetNodeDataTest() {
    	Fund fund = createFund("fund1", "internalCode");
    	assertNotNull(fund);

    	// create levels (nodes)
        ArrFundVersionVO fundVersion = getOpenVersion(fund);
        List<ArrNodeVO> nodes = createLevels(fundVersion);
        ArrNodeVO node = nodes.get(0);

        // create item by SRD_TITLE
        NodeItem nodeItem = buildNodeItem(SRD_TITLE, null, DataType.TEXT, "value", node);
        descitemsApi.descItemCreateDescItem(fundVersion.getId(), nodeItem);

        // create NodeDataParam
        NodeDataParam param = new NodeDataParam();
        param.setFundVersionId(fundVersion.getId());
        param.setNodeId(nodes.get(0).getId());
        param.setSiblingsMaxCount(100);
        param.setFormData(true);
        param.setParents(true);
        param.setChildren(true);

        NodeData nodeData = nodeApi.nodeGetNodeData(param);
        assertNotNull(nodeData);
        assertNotNull(nodeData.getFormData());
        assertNotNull(nodeData.getNode());
        assertTrue(!nodeData.getChildren().isEmpty());
        assertTrue(nodeData.getParents().isEmpty());
        assertTrue(nodeData.getSiblings().isEmpty());
    }
}
