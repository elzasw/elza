package cz.tacr.elza.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.lang3.reflect.FieldUtils;
import org.assertj.core.util.Arrays;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.task.TaskSchedulerBuilder;
import org.springframework.http.HttpHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.converter.SimpleMessageConverter;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSession.Receiptable;
import org.springframework.messaging.simp.stomp.StompSessionHandler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import cz.tacr.elza.controller.arrangement.UpdateItemResult;
import cz.tacr.elza.controller.vo.AddLevelParam;
import cz.tacr.elza.controller.vo.ArrFundVersionVO;
import cz.tacr.elza.controller.vo.TreeData;
import cz.tacr.elza.controller.vo.TreeNodeVO;
import cz.tacr.elza.controller.vo.nodes.ArrNodeVO;
import cz.tacr.elza.controller.vo.nodes.RulDescItemTypeExtVO;
import cz.tacr.elza.controller.vo.nodes.descitems.ArrItemIntVO;
import cz.tacr.elza.controller.vo.nodes.descitems.ArrItemVO;
import cz.tacr.elza.controller.vo.nodes.descitems.ArrUpdateItemVO;
import cz.tacr.elza.controller.vo.nodes.descitems.UpdateOp;
import cz.tacr.elza.domain.ArrItem;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.repository.ItemRepository;
import cz.tacr.elza.service.FundLevelService;
import cz.tacr.elza.test.ApiException;
import cz.tacr.elza.test.controller.vo.Fund;
import cz.tacr.elza.websocket.WebSocketStompClientElza;

public class ArrangementWebsocketControllerTest extends AbstractControllerTest {

    static private Logger logger = LoggerFactory.getLogger(ArrangementWebsocketControllerTest.class);

    private final String UPDATE_DESK_ITEMS_MSG_MAPPING = "/app/arrangement/descItems/{fundVersionId}/{nodeId}/{nodeVersion}/update/bulk";

    private final String ADD_LEVEL_MSG_MAPPING = "/app/arrangement/levels/add";

    enum ReceiptStatus {
        RCP_WAITING,
        RCP_RECEIVED,
        RCP_LOST
    };

    @Autowired
    ItemRepository itemRepository;

    private ArrFundVersionVO fundVersion;

    private TreeData treeData;

    @Test
    public void updateDescItemsTest() throws InterruptedException, ExecutionException, IllegalAccessException, ApiException, JsonParseException, JsonMappingException, IOException {
        final Map<String, Message<byte[]>> receiptStore = new HashMap<>();
        ObjectMapper mapper = new ObjectMapper();
        Message<byte[]> recepipt;

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
        waitingForReceipt(receiptCreate, sessionHandler);
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
        waitingForReceipt(receiptUpdate, sessionHandler);
        recepipt = receiptStore.get(receiptUpdate.getReceiptId());

        List<UpdateItemResult> updResults = mapper.readValue(recepipt.getPayload(), new TypeReference<List<UpdateItemResult>>(){});
        assertTrue(updResults.size() == 1);
        assertNotNull(updResults.get(0).getItem());
        assertTrue(updResults.get(0).getNode().getVersion() == 2);

        // mazání existujícího ArrItem
        ArrItemIntVO deleleItem = (ArrItemIntVO) updResults.get(0).getItem();
        ArrUpdateItemVO[] deleteItems = { new ArrUpdateItemVO(UpdateOp.DELETE, deleleItem) };

        Receiptable receiptDelete = session
                .send(createDestination(UPDATE_DESK_ITEMS_MSG_MAPPING, fundVersionId, nodeId, ++nodeVersion), deleteItems);
        waitingForReceipt(receiptDelete, sessionHandler);
        recepipt = receiptStore.get(receiptDelete.getReceiptId());

        List<UpdateItemResult> delResults = mapper.readValue(recepipt.getPayload(), new TypeReference<List<UpdateItemResult>>(){});
        assertTrue(delResults.size() == 1);
        assertNotNull(delResults.get(0).getItem());
        assertTrue(delResults.get(0).getNode().getVersion() == 3);

        session.disconnect();
    }

    @Test
    public void addLevelTest() throws InterruptedException, ExecutionException, IllegalAccessException, ApiException {
        final Map<String, Message<byte[]>> recepiptStore = new HashMap<>();
        MyStompSessionHandler sessionHandler = new MyStompSessionHandler();
        
        StompSession session = connectWebSocketStompClient(sessionHandler, recepiptStore);
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
        waitingForReceipt(receipt, sessionHandler);

        // Monitorování výsledků dotazu
        List<ArrNode> nodes = nodeRepository.findAll();
        assertTrue(nodes.size() == 3);

        session.disconnect();
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

    private void waitingForReceipt(Receiptable receipt, MyStompSessionHandler sessionHandler) throws InterruptedException {
        AtomicReference<ReceiptStatus> receiptStatus = new AtomicReference<ReceiptStatus>();
        receipt.addReceiptTask(() -> {
            logger.debug("Receipt received");
            receiptStatus.set(ReceiptStatus.RCP_RECEIVED);
        });
        receipt.addReceiptLostTask(() -> {
            logger.debug("Receipt lost");
            receiptStatus.set(ReceiptStatus.RCP_LOST);
        });
        while (receiptStatus.get() == null && !sessionHandler.hasError()) {
            logger.info("Waiting on receipt...");
            Thread.sleep(100);
        }
        if (sessionHandler.hasError()) {
            logger.debug("Receipt error ove WebSocket");            
            fail("Receipt error over WebSocket");
        }
        assertEquals(ReceiptStatus.RCP_RECEIVED, receiptStatus.get());
    }

    private void initFundVersionAndTreeData() throws ApiException {
        Fund fund = createFund("Jmeno", "kod");
        helperTestService.waitForWorkers();
        fundVersion = getOpenVersion(fund);

        ArrangementController.FaTreeParam input = new ArrangementController.FaTreeParam();
        input.setVersionId(fundVersion.getId());
        treeData = getFundTree(input);
    }

    private StompSession connectWebSocketStompClient(StompSessionHandler sessionHandler, 
                                                     final Map<String, Message<byte[]> > recepiptStore) throws InterruptedException, ExecutionException {
        WebSocketClient client = new StandardWebSocketClient();
        WebSocketStompClientElza stompClient = new WebSocketStompClientElza(client, (rcpId, msg) -> {
            recepiptStore.put(rcpId, msg);
        });

        ThreadPoolTaskScheduler taskScheduler = new TaskSchedulerBuilder().poolSize(1).build();
        taskScheduler.initialize();
        stompClient.setTaskScheduler(taskScheduler);

        stompClient.setMessageConverter(new MappingJackson2MessageConverter());

        // Dostáváme údaje k autorizaci
        WebSocketHttpHeaders handshakeHeaders = new WebSocketHttpHeaders();
        StringBuilder cookie = new StringBuilder();
        cookies.forEach((name, value) -> {
            if (cookie.length() > 0) {
                cookie.append(';');
            }
            cookie.append(String.format("%s=%s", name, value));
        });
        if (cookie.length() > 0) {
            handshakeHeaders.set(HttpHeaders.COOKIE, cookie.toString());
        }

        ListenableFuture<StompSession> futureSession = stompClient.connect("ws://localhost:" + port + "/stomp", 
                                                                           handshakeHeaders, sessionHandler);
        return futureSession.get();
    }

    class MyStompSessionHandler implements StompSessionHandler {

        private Logger logger = LoggerFactory.getLogger(this.getClass());
        
        int errorCount = 0;

        @Override
        public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
            logger.info("New session established : " + session.getSessionId());
        }

        @Override
        public Type getPayloadType(StompHeaders headers) {
            return String.class;
        }

        @Override
        public void handleFrame(StompHeaders headers, Object payload) {
            logger.info("Received: {}", payload);
        }

        @Override
        public void handleException(StompSession session, StompCommand command, StompHeaders headers, byte[] payload, Throwable exception) {
            logger.error("Got an exception: ", exception);
            errorCount++;
        }

        @Override
        public void handleTransportError(StompSession session, Throwable exception) {
            logger.error("Got an transport error: ", exception);
            errorCount++;
        }

        public boolean hasError() {
            return errorCount > 0;
        }
    }

}
