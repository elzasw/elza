package cz.tacr.elza.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSession.Receiptable;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DatabindException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import cz.tacr.elza.controller.arrangement.UpdateItemResult;
import cz.tacr.elza.controller.vo.AddLevelParam;
import cz.tacr.elza.controller.vo.ArrFundVersionVO;
import cz.tacr.elza.controller.vo.ArrInhibitedItemVO;
import cz.tacr.elza.controller.vo.TreeData;
import cz.tacr.elza.controller.vo.TreeNodeVO;
import cz.tacr.elza.controller.vo.nodes.ArrNodeVO;
import cz.tacr.elza.controller.vo.nodes.RulDescItemTypeExtVO;
import cz.tacr.elza.controller.vo.nodes.descitems.ArrItemIntVO;
import cz.tacr.elza.controller.vo.nodes.descitems.ArrUpdateItemVO;
import cz.tacr.elza.controller.vo.nodes.descitems.UpdateOp;
import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.ArrInhibitedItem;
import cz.tacr.elza.domain.ArrLevel;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.service.FundLevelService;
import cz.tacr.elza.test.ApiException;
import cz.tacr.elza.test.controller.vo.Fund;

public class ArrangementWebsocketControllerTest extends AbstractControllerTest {

    private final String UPDATE_DESK_ITEMS_MSG_MAPPING = "/app/arrangement/descItems/{fundVersionId}/{nodeId}/{nodeVersion}/update/bulk";

    private final String ADD_LEVEL_MSG_MAPPING = "/app/arrangement/levels/add";

    enum ReceiptStatus {
        RCP_WAITING,
        RCP_RECEIVED,
        RCP_LOST,
        RCP_ERROR
    };

    private ArrFundVersionVO fundVersion;

    private TreeData treeData;

    @Test
    public void updateDescItemsTest() throws InterruptedException, ExecutionException, IllegalAccessException, ApiException, JsonParseException, JsonMappingException, IOException {
        final Map<String, Message<byte[]>> receiptStore = new HashMap<>();
        ObjectMapper mapper = new ObjectMapper();
        Message<byte[]> recepipt;
        ReceiptStatus status;

        MyStompSessionHandler sessionHandler = new MyStompSessionHandler();
        StompSession session = connectWebSocketStompClient(sessionHandler, receiptStore);
        session.setAutoReceipt(true);

        FieldUtils.writeField(StompCommand.RECEIPT, "body", true, true);

        initFundVersionAndTreeData();

        // Musí existovat root node
        assertNotNull(treeData.getNodes());

        // Musí existovat pouze root node
        assertTrue(treeData.getNodes().size() == 1);

        // Příprava objektu s daty pro odeslání
        Integer fundVersionId = fundVersion.getId();
        Integer nodeId = treeData.getNodes().iterator().next().getId();
        Integer nodeVersion = treeData.getNodes().iterator().next().getVersion();

        // vytvoření nové ArrItem
        ArrItemIntVO newItem = new ArrItemIntVO();
        newItem.setItemTypeId(findItemTypeId("SRD_NAD"));
        newItem.setValue(1);
        ArrUpdateItemVO[] createItems = { new ArrUpdateItemVO(UpdateOp.CREATE, newItem) };

        Receiptable receiptCreate = session
                .send(createDestination(UPDATE_DESK_ITEMS_MSG_MAPPING, fundVersionId, nodeId, nodeVersion), createItems);
        status = waitingForReceipt(receiptCreate, sessionHandler);
        assertEquals(ReceiptStatus.RCP_RECEIVED, status);

        recepipt = receiptStore.get(receiptCreate.getReceiptId());
        assertNotNull(recepipt);

        List<UpdateItemResult> addResults = mapper.readValue(recepipt.getPayload(), new TypeReference<List<UpdateItemResult>>(){});
        assertTrue(addResults.size() == 1);
        assertNotNull(addResults.get(0).getItem());
        assertTrue(addResults.get(0).getNode().getVersion() == 1);

        // změna existujícího ArrItem
        ArrItemIntVO updateItem = (ArrItemIntVO) addResults.get(0).getItem();
        updateItem.setValue(2);
        ArrUpdateItemVO[] updateItems = { new ArrUpdateItemVO(UpdateOp.UPDATE, updateItem) };

        Receiptable receiptUpdate = session
                .send(createDestination(UPDATE_DESK_ITEMS_MSG_MAPPING, fundVersionId, nodeId, ++nodeVersion), updateItems);
        status = waitingForReceipt(receiptUpdate, sessionHandler);
        assertEquals(ReceiptStatus.RCP_RECEIVED, status);

        recepipt = receiptStore.get(receiptUpdate.getReceiptId());
        assertNotNull(recepipt);

        List<UpdateItemResult> updResults = mapper.readValue(recepipt.getPayload(), new TypeReference<List<UpdateItemResult>>(){});
        assertTrue(updResults.size() == 1);
        assertNotNull(updResults.get(0).getItem());
        assertTrue(updResults.get(0).getNode().getVersion() == 2);

        // mazání existujícího ArrItem
        ArrItemIntVO deleleItem = (ArrItemIntVO) updResults.get(0).getItem();
        ArrUpdateItemVO[] deleteItems = { new ArrUpdateItemVO(UpdateOp.DELETE, deleleItem) };

        Receiptable receiptDelete = session
                .send(createDestination(UPDATE_DESK_ITEMS_MSG_MAPPING, fundVersionId, nodeId, ++nodeVersion), deleteItems);
        status = waitingForReceipt(receiptDelete, sessionHandler);
        assertEquals(ReceiptStatus.RCP_RECEIVED, status);

        recepipt = receiptStore.get(receiptDelete.getReceiptId());
        assertNotNull(recepipt);

        List<UpdateItemResult> delResults = mapper.readValue(recepipt.getPayload(), new TypeReference<List<UpdateItemResult>>(){});
        assertTrue(delResults.size() == 1);
        assertNotNull(delResults.get(0).getItem());
        assertTrue(delResults.get(0).getNode().getVersion() == 3);

        session.disconnect();
    }

    @Test
    public void addLevelTest() throws InterruptedException, ExecutionException, IllegalAccessException, ApiException {
        final Map<String, Message<byte[]>> receiptStore = new HashMap<>();
        MyStompSessionHandler sessionHandler = new MyStompSessionHandler();
        
        StompSession session = connectWebSocketStompClient(sessionHandler, receiptStore);
        session.setAutoReceipt(true);

        FieldUtils.writeField(StompCommand.RECEIPT, "body", true, true);

        //session.subscribe("/topic/api/changes", sessionHandler); // funguje i bez tohoto řádku

        initFundVersionAndTreeData();

        // Musí existovat root node
        assertNotNull(treeData.getNodes());

        // Musí existovat pouze root node
        assertTrue(treeData.getNodes().size() == 1);

        TreeNodeVO rootTreeNodeClient = treeData.getNodes().iterator().next();
        ArrNodeVO rootNode = convertTreeNode(rootTreeNodeClient);

        // Příprava objektu s daty pro odeslání
        AddLevelParam addLevelParam = new AddLevelParam();
        addLevelParam.setVersionId(fundVersion.getId());
        addLevelParam.setStaticNode(rootNode);
        addLevelParam.setStaticNodeParent(rootNode);
        addLevelParam.setDirection(FundLevelService.AddLevelDirection.CHILD);
        addLevelParam.setCount(2); // přidat více než 1 úroveň

        Receiptable receipt = session.send(ADD_LEVEL_MSG_MAPPING, addLevelParam);
        ReceiptStatus status = waitingForReceipt(receipt, sessionHandler);
        assertEquals(ReceiptStatus.RCP_RECEIVED, status);

        // Monitorování výsledků dotazu
        List<ArrNode> nodes = nodeRepository.findAll();
        assertTrue(nodes.size() == 3);

        session.disconnect();
    }

    @Test
    public void inhibitedItemTest() throws InterruptedException, ExecutionException, IllegalAccessException, ApiException, StreamReadException, DatabindException, IOException {
    	final Map<String, Message<byte[]>> receiptStore = new HashMap<>();
        MyStompSessionHandler sessionHandler = new MyStompSessionHandler();
        ObjectMapper mapper = new ObjectMapper();
        Message<byte[]> recepipt;
        ReceiptStatus status;

        StompSession session = connectWebSocketStompClient(sessionHandler, receiptStore);
        session.setAutoReceipt(true);

        FieldUtils.writeField(StompCommand.RECEIPT, "body", true, true);

        initFundVersionAndTreeData();

        // Musí existovat root node
        assertNotNull(treeData.getNodes());

        // Musí existovat pouze root node
        assertTrue(treeData.getNodes().size() == 1);

        // Musí existovat pouze jeden descItem
        List<ArrDescItem> descItems = descItemRepository.findAll();
        assertTrue(descItems.size() == 1);

        List<ArrLevel> levels = levelRepository.findAll();

        // Přidání nové úrovně
        TreeNodeVO treeNodeVO = treeData.getNodes().iterator().next();
        ArrNodeVO rootNode = convertTreeNode(treeNodeVO);

        // Přidání JP na úroveň
        ArrItemIntVO itemInt = new ArrItemIntVO();
        itemInt.setItemTypeId(findItemTypeId("SRD_NAD"));
        itemInt.setValue(12);

        AddLevelParam addLevelParam = new AddLevelParam();
        addLevelParam.setVersionId(fundVersion.getId());
        addLevelParam.setStaticNode(rootNode);
        addLevelParam.setStaticNodeParent(rootNode);
        addLevelParam.setDirection(FundLevelService.AddLevelDirection.CHILD);
        addLevelParam.setCreateItems(List.of(itemInt));
        addLevelParam.setCount(1); // přidat jen 1 úroveň

        Receiptable receiptable = session.send(ADD_LEVEL_MSG_MAPPING, addLevelParam);
        status = waitingForReceipt(receiptable, sessionHandler);
        assertEquals(ReceiptStatus.RCP_RECEIVED, status);

        recepipt = receiptStore.get(receiptable.getReceiptId());
        assertNotNull(recepipt);

        ArrangementController.NodesWithParent nodesWithParent = mapper.readValue(recepipt.getPayload(), ArrangementController.NodesWithParent.class);
        assertNotNull(nodesWithParent);

        descItems = descItemRepository.findAll();
        assertTrue(descItems.size() == 2);
        levels = levelRepository.findAll();
        assertNotNull(levels);
        assertTrue(levels.size() == 2);

        // Přidání jednoho záznamu ArrInhibitedItem:
        //   node1 -> descItem1
        //   node2 -> descItem2
        // arrInhibitedItem <- node2,descItem1
        ArrInhibitedItemVO arrInhibitedItem = new ArrInhibitedItemVO(); 
        arrInhibitedItem.setNodeId(levels.get(levels.size() - 1).getNodeId());
        arrInhibitedItem.setItemId(descItems.iterator().next().getItemId());

        receiptable = session.send(INHIBIT_DESC_ITEM, arrInhibitedItem);
        status = waitingForReceipt(receiptable, sessionHandler);
        assertEquals(ReceiptStatus.RCP_RECEIVED, status);

        recepipt = receiptStore.get(receiptable.getReceiptId());
        assertNotNull(recepipt);

        Integer inhibitItemId = mapper.readValue(recepipt.getPayload(), Integer.class);
        assertNotNull(inhibitItemId);

        // Označení přidané položky jako smazané
        receiptable = session.send(ALLOW_DESC_ITEM, inhibitItemId);
        status = waitingForReceipt(receiptable, sessionHandler);
        assertEquals(ReceiptStatus.RCP_RECEIVED, status);

        recepipt = receiptStore.get(receiptable.getReceiptId());
        assertNotNull(recepipt);

        Integer deleteInhibitItemId = mapper.readValue(recepipt.getPayload(), Integer.class);
        assertNotNull(deleteInhibitItemId);
        assertEquals(inhibitItemId, deleteInhibitItemId);

        // Kontrola, zda je záznam označen jako smazaný
        ArrInhibitedItem inhibitedItem = inhibitedItemRepository.findById(inhibitItemId).orElseThrow();
        assertNotNull(inhibitedItem);
        assertNotNull(inhibitedItem.getDeleteChange());

        // Přidání jednoho záznamu ArrInhibitedItem s chybou
        // arrInhibitedItem <- node1,descItem1
        arrInhibitedItem.setNodeId(levels.iterator().next().getNodeId());
        arrInhibitedItem.setItemId(descItems.iterator().next().getItemId());
        receiptable = session.send(INHIBIT_DESC_ITEM, arrInhibitedItem);
        status = waitingForReceipt(receiptable, sessionHandler);
        assertEquals(ReceiptStatus.RCP_ERROR, status);

        // if the operation is completed with an error, the session is closed
        //session.disconnect();
    }

    private Integer findItemTypeId(String code) {
        List<RulDescItemTypeExtVO> itemTypes = getDescItemTypes();
        for (RulDescItemTypeExtVO item : itemTypes) {
            if (item.getCode().equals(code)) {
                return item.getId();
            }
        }
        return null;
    }

    private String createDestination(String pattern, Integer fundVersionId, Integer nodeId, Integer nodeVersion) {
        return pattern
                .replace("{fundVersionId}", fundVersionId.toString())
                .replace("{nodeId}", nodeId.toString())
                .replace("{nodeVersion}", nodeVersion.toString());
    }

    private void initFundVersionAndTreeData() throws ApiException {
        Fund fund = createFund("Jmeno", "kod");
        helperTestService.waitForWorkers();
        fundVersion = getOpenVersion(fund);

        ArrangementController.FaTreeParam input = new ArrangementController.FaTreeParam();
        input.setVersionId(fundVersion.getId());
        treeData = getFundTree(input);
    }
}
