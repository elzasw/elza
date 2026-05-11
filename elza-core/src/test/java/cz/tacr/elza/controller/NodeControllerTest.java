package cz.tacr.elza.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.List;

import org.junit.jupiter.api.Test;

import cz.tacr.elza.controller.ArrangementController.DescFormDataNewVO;
import cz.tacr.elza.controller.vo.ApAccessPointVO;
import cz.tacr.elza.controller.vo.ArrFundVersionVO;
import cz.tacr.elza.controller.vo.nodes.ArrNodeVO;
import cz.tacr.elza.controller.vo.nodes.RulDescItemTypeExtVO;
import cz.tacr.elza.controller.vo.nodes.descitems.ArrItemTextVO;
import cz.tacr.elza.controller.vo.nodes.descitems.ArrItemVO;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.test.controller.vo.DataText;
import cz.tacr.elza.test.controller.vo.DataType;
import cz.tacr.elza.test.controller.vo.DescItemField;
import cz.tacr.elza.test.controller.vo.FieldType;
import cz.tacr.elza.test.controller.vo.FieldValueFilter;
import cz.tacr.elza.test.controller.vo.FondsField;
import cz.tacr.elza.test.controller.vo.FondsFieldName;
import cz.tacr.elza.test.controller.vo.Fund;
import cz.tacr.elza.test.controller.vo.ItemDataResult;
import cz.tacr.elza.test.controller.vo.LogicalFilter;
import cz.tacr.elza.test.controller.vo.MultimatchContainsFilter;
import cz.tacr.elza.test.controller.vo.NodeData;
import cz.tacr.elza.test.controller.vo.NodeDataParam;
import cz.tacr.elza.test.controller.vo.NodeField;
import cz.tacr.elza.test.controller.vo.NodeFieldName;
import cz.tacr.elza.test.controller.vo.NodeItem;
import cz.tacr.elza.test.controller.vo.NodeSearchResult;
import cz.tacr.elza.test.controller.vo.NodeTreeData;
import cz.tacr.elza.test.controller.vo.OperationCompareType;
import cz.tacr.elza.test.controller.vo.OperationLogicalType;
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
        NodeItem nodeItem = buildNodeItem(SRD_TITLE, null, DataType.TEXT, "value", node, null);
        descitemsApi.descItemCreateDescItem(fundVersion.getId(), nodeItem);

        // create item by SRD_SERIAL_NUMBER
        nodeItem = buildNodeItem(SRD_SERIAL_NUMBER, null, DataType.INT, 1, node, null);
        descitemsApi.descItemCreateDescItem(fundVersion.getId(), nodeItem);

        // create item by SRD_UNIT_DATE
        nodeItem = buildNodeItem(SRD_UNIT_DATE, null, DataType.UNITDATE, "15.5.2025", node, null);
        descitemsApi.descItemCreateDescItem(fundVersion.getId(), nodeItem);

        // create item by SRD_OTHER_ID
        nodeItem = buildNodeItem(SRD_OTHER_ID, SRD_OTHERID_CJ, DataType.STRING, "13", node, null);
        descitemsApi.descItemCreateDescItem(fundVersion.getId(), nodeItem);

        // create item by SRD_LANGUAGE
        nodeItem = buildNodeItem(SRD_LANGUAGE, SRD_LANGUAGE_1, DataType.ENUM, null, node, null);
        descitemsApi.descItemCreateDescItem(fundVersion.getId(), nodeItem);

        // prepare to create item by SRD_ENTITY_ROLE
        List<ApAccessPointVO> accessPoints = findRecord(null, null, null, null, null);
        ApAccessPointVO accessPoint = accessPoints.get(0);
        // create item by SRD_ENTITY_ROLE
        nodeItem = buildNodeItem(SRD_ENTITY_ROLE, SRD_ENTITY_ROLE_1, DataType.RECORD_REF, accessPoint, node, null);
        descitemsApi.descItemCreateDescItem(fundVersion.getId(), nodeItem);

        //
        // create MultimatchContainsFilter filter
        //
        MultimatchContainsFilter containsFilter = new MultimatchContainsFilter();
        containsFilter.setValue("value");

        // create search params
        SearchParams params = new SearchParams();
        params.addFiltersItem(containsFilter);

        // waiting for reindexing to get result
        NodeSearchResult searchResult = nodeSearch(params);
        assertEquals(1, searchResult.getFonds().size());

    	List<NodeTreeData> nodeResult = nodeApi.nodeGetSearchResult(fund.getId());
    	assertEquals(1, nodeResult.size());

    	//
        // create FieldValueFilter filter
    	//
        FieldValueFilter valueFilter = new FieldValueFilter();
        valueFilter.setField(new DescItemField().typeCode(SRD_TITLE));
        valueFilter.setValue("alu");
        valueFilter.setOperation(OperationCompareType.CONTAINS);

        // set FieldValueFilter in params
        params.filters(List.of(valueFilter));

        // try to search text using FieldValueFilter
        searchResult = nodeSearch(params);
        assertEquals(1, searchResult.getFonds().size());

        // change filter by SRD_SERIAL_NUMBER
        valueFilter.setField(new DescItemField().typeCode(SRD_SERIAL_NUMBER));
        valueFilter.setValue("1");
        valueFilter.setOperation(OperationCompareType.EQ);

        // try to search serial number using FieldValueFilter
        searchResult = nodeSearch(params);
        assertEquals(1, searchResult.getFonds().size());

        // change filter by SRD_UNIT_DATE
        valueFilter.setField(new DescItemField().typeCode(SRD_UNIT_DATE));
        valueFilter.setValue("15.5.2025");
        valueFilter.setOperation(OperationCompareType.EQ);

        // try to search unitdate using FieldValueFilter
        searchResult = nodeSearch(params);
        assertEquals(1, searchResult.getFonds().size());

        // change filter by SRD_OTHER_ID
        valueFilter.setField(new DescItemField().typeCode(SRD_OTHER_ID).specCode(SRD_OTHERID_CJ));
        valueFilter.setValue("13");
        valueFilter.setOperation(OperationCompareType.CONTAINS);

        // try to search otherId using FieldValueFilter
        searchResult = nodeSearch(params);
        assertEquals(1, searchResult.getFonds().size());

        // change filter by SRD_LANGUAGE
        valueFilter.setField(new DescItemField().typeCode(SRD_LANGUAGE).specCode(SRD_LANGUAGE_1));
        valueFilter.setValue(null);
        valueFilter.setOperation(OperationCompareType.EQ);

        // try to search language using FieldValueFilter
        searchResult = nodeSearch(params);
        assertEquals(1, searchResult.getFonds().size());

        // change filter by RECORD_REF search by id
        valueFilter.setField(new DescItemField().typeCode(SRD_ENTITY_ROLE).specCode(SRD_ENTITY_ROLE_1));
        valueFilter.setValue(accessPoint.getId().toString());
        valueFilter.setOperation(OperationCompareType.EQ);

        // try to search by AP name using FieldValueFilter
        searchResult = nodeSearch(params);
        assertEquals(1, searchResult.getFonds().size());

        // change filter by RECORD_REF search by apId
        valueFilter.setValue(accessPoint.getId().toString());
        valueFilter.setOperation(OperationCompareType.EQ);

        // try to search by AP id using FieldValueFilter
        searchResult = nodeSearch(params);
        assertEquals(1, searchResult.getFonds().size());

        // get ArrNode for uuid
        ArrNode arrNode = nodeRepository.findById(node.getId()).get();

        // try to search by NODE_FIELD field type
        valueFilter.setField(new NodeField().fieldName(NodeFieldName.UUID));
        valueFilter.setValue(arrNode.getUuid());
        valueFilter.setOperation(OperationCompareType.EQ);

        // try to search by UUID id using FieldValueFilter
        searchResult = nodeSearch(params);
        assertEquals(1, searchResult.getFonds().size());

        //
        // testy LogicalFilter s využitím dvou fondů a filtru FONDS_ID
        //

        // vytvoření druhého fondu s uzlem a položkami
        Fund fund2 = createFund("fund2", "internalCode2");
        assertNotNull(fund2);

        ArrFundVersionVO fundVersion2 = getOpenVersion(fund2);
        List<ArrNodeVO> nodes2 = createLevels(fundVersion2);
        ArrNodeVO node2 = nodes2.get(0);

        // přidat položku SRD_TITLE do uzlu fund2 (stejná hodnota jako v fund1)
        NodeItem nodeItem2 = buildNodeItem(SRD_TITLE, null, DataType.TEXT, "value", node2, null);
        descitemsApi.descItemCreateDescItem(fundVersion2.getId(), nodeItem2);

        // příprava filtru na fondsId pro fund1
        FieldValueFilter fund1IdFilter = new FieldValueFilter();
        fund1IdFilter.setField(new FondsField().fieldType(FieldType.FONDS_FIELD).fieldName(FondsFieldName.FONDS_ID));
        fund1IdFilter.setValue(fund.getId().toString());
        fund1IdFilter.setOperation(OperationCompareType.EQ);

        // příprava filtru na fondsId pro fund2
        FieldValueFilter fund2IdFilter = new FieldValueFilter();
        fund2IdFilter.setField(new FondsField().fieldType(FieldType.FONDS_FIELD).fieldName(FondsFieldName.FONDS_ID));
        fund2IdFilter.setValue(fund2.getId().toString());
        fund2IdFilter.setOperation(OperationCompareType.EQ);

        // OR: fond1 nebo fond2 → oba fondy mají uzel s hodnotou "value", očekáváme 2 fondy
        LogicalFilter orFondsFilter = new LogicalFilter();
        orFondsFilter.setOperation(OperationLogicalType.OR);
        orFondsFilter.setFilters(List.of(fund1IdFilter, fund2IdFilter));

        params.filters(List.of(orFondsFilter));
        // čekáme, až bude fond2 zaindexován
        NodeSearchResult searchResult2Fonds = null;
        int counter2 = 0;
        try {
            do {
                Thread.sleep(100);
                searchResult2Fonds = nodeApi.nodeSearch(params);
                counter2++;
            } while (searchResult2Fonds.getFonds().size() < 2 && counter2 < 1000);
        } catch (Exception e) {
            fail("Exception while waiting on result: " + e);
        }
        assertEquals(2, searchResult2Fonds.getFonds().size());

        // AND: fondsId=fund1 AND title CONTAINS "value" → pouze fond1
        FieldValueFilter titleFilter2 = new FieldValueFilter();
        titleFilter2.setField(new DescItemField().typeCode(SRD_TITLE));
        titleFilter2.setValue("value");
        titleFilter2.setOperation(OperationCompareType.CONTAINS);

        LogicalFilter andFund1TitleFilter = new LogicalFilter();
        andFund1TitleFilter.setOperation(OperationLogicalType.AND);
        andFund1TitleFilter.setFilters(List.of(fund1IdFilter, titleFilter2));

        params.filters(List.of(andFund1TitleFilter));
        searchResult = nodeSearch(params);
        assertEquals(1, searchResult.getFonds().size());
        assertEquals(fund.getId(), searchResult.getFonds().get(0).getId());

        // AND: fondsId=fund2 AND title CONTAINS "xyz" → žádný výsledek
        FieldValueFilter titleNoMatchFilter = new FieldValueFilter();
        titleNoMatchFilter.setField(new DescItemField().typeCode(SRD_TITLE));
        titleNoMatchFilter.setValue("xyz");
        titleNoMatchFilter.setOperation(OperationCompareType.CONTAINS);

        LogicalFilter andFund2NoMatchFilter = new LogicalFilter();
        andFund2NoMatchFilter.setOperation(OperationLogicalType.AND);
        andFund2NoMatchFilter.setFilters(List.of(fund2IdFilter, titleNoMatchFilter));

        params.filters(List.of(andFund2NoMatchFilter));
        searchResult = nodeApi.nodeSearch(params); // přímé volání — žádný výsledek se neočekává
        assertEquals(0, searchResult.getFonds().size());        
    }

    private NodeSearchResult nodeSearch(SearchParams params) {
        NodeSearchResult searchResult = null;
        int counter = 0;
        try {
            do {
                Thread.sleep(100);
                searchResult = nodeApi.nodeSearch(params);
                counter++;
            } while (searchResult.getFonds().size() == 0 && counter < 1000);
        } catch (Exception e) {
            fail("Exception while waiting on result: " + e);
        }
        return searchResult;
    }

    @Test
    public void nodeGetNodeDataTest() {
        Fund fund = createFund("fund1", "internalCode");
        ArrFundVersionVO fundVersion = getOpenVersion(fund);
        List<ArrNodeVO> nodes = createLevels(fundVersion);
        ArrNodeVO focusNode = nodes.get(0);

        // attach a desc item so the form has content to return
        descitemsApi.descItemCreateDescItem(fundVersion.getId(),
                buildNodeItem(SRD_TITLE, null, DataType.TEXT, "value", focusNode, null));

        // nodeStatus=true → response.node populated with id + version
        NodeData withStatus = fetchNodeData(fundVersion, focusNode, true);
        assertNotNull(withStatus.getFormData());
        assertNotNull(withStatus.getNode());
        assertEquals(focusNode.getId(), withStatus.getNode().getId());
        assertNotNull(withStatus.getNode().getVersion());
        assertTrue(!withStatus.getChildren().isEmpty());
        assertTrue(withStatus.getParents().isEmpty());
        assertTrue(withStatus.getSiblings().isEmpty());

        // nodeStatus=false → focus-node block is skipped, response.node is null
        NodeData withoutStatus = fetchNodeData(fundVersion, focusNode, false);
        assertNull(withoutStatus.getNode());
        // toggling the flag must not affect the rest of the payload
        assertNotNull(withoutStatus.getFormData());
    }

    /**
     * Fetch node data with a common parameter set, varying only the {@code nodeStatus} flag.
     */
    private NodeData fetchNodeData(ArrFundVersionVO fundVersion, ArrNodeVO node, boolean nodeStatus) {
        NodeDataParam param = new NodeDataParam();
        param.setFundVersionId(fundVersion.getId());
        param.setNodeId(node.getId());
        param.setSiblingsMaxCount(100);
        param.setFormData(true);
        param.setParents(true);
        param.setChildren(true);
        param.setNodeStatus(nodeStatus);

        NodeData data = nodeApi.nodeGetNodeData(param);
        assertNotNull(data);
        return data;
    }

	@Test
	public void nodeCopyOlderSiblingAttributeTest() {
	    Fund fund = createFund("fund1", "internalCode");
	    ArrFundVersionVO fundVersion = getOpenVersion(fund);

	    List<ArrNodeVO> nodes = createLevels(fundVersion);
	    ArrNodeVO node1 = nodes.get(1);
	    ArrNodeVO node2 = nodes.get(2);

	    // vytvoření hodnoty na starším sourozenci
	    RulDescItemTypeExtVO type = findDescItemTypeByCode(SRD_TITLE);
	    NodeItem nodeItem = buildNodeItem(type.getCode(), null, DataType.TEXT, "value", node1, null);
	    ItemDataResult itemDataResult = descitemsApi.descItemCreateDescItem(fundVersion.getId(), nodeItem);
	    NodeItem nodeItemCreated = itemDataResult.getItem();

	    assertNotNull(((DataText) nodeItem.getData()).getTextValue().equals(((DataText) nodeItemCreated.getData()).getTextValue()));
	    assertNotNull(nodeItemCreated.getPosition());
	    assertNotNull(nodeItemCreated.getItemObjectId());

	    // zkopírování hodnoty ze staršího sourozence
	    nodeApi.nodeCopyOlderSiblingAttribute(fundVersion.getId(), type.getId(), convertArrNode(node2));

	    // ověření, že hodnota byla zkopírována
	    DescFormDataNewVO formData = getNodeFormData(node2.getId(), fundVersion.getId());
	    List<ArrItemVO> items = formData.getDescItems();
	    assertTrue(items.size() == 1);
	    ArrItemVO result = items.get(0);
	    ArrItemTextVO textVo = (ArrItemTextVO) result;
	    assertTrue(textVo.getValue().equals("value"));
	}
}
