package cz.tacr.elza.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Type;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;

import javax.transaction.Transactional;

import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.task.TaskSchedulerBuilder;
import org.springframework.http.HttpHeaders;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
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

import cz.tacr.elza.controller.vo.AddLevelParam;
import cz.tacr.elza.controller.vo.ArrFundVersionVO;
import cz.tacr.elza.controller.vo.TreeData;
import cz.tacr.elza.controller.vo.TreeNodeVO;
import cz.tacr.elza.controller.vo.nodes.ArrNodeVO;
import cz.tacr.elza.controller.vo.nodes.RulDescItemTypeExtVO;
import cz.tacr.elza.controller.vo.nodes.descitems.ArrItemIntVO;
import cz.tacr.elza.controller.vo.nodes.descitems.ArrUpdateItemVO;
import cz.tacr.elza.controller.vo.nodes.descitems.UpdateOp;
import cz.tacr.elza.domain.ArrItem;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.repository.ItemRepository;
import cz.tacr.elza.service.FundLevelService;
import cz.tacr.elza.test.ApiException;
import cz.tacr.elza.test.controller.vo.Fund;

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
    @Transactional
    public void updateDescItemsTest() throws InterruptedException, ExecutionException, IllegalAccessException, ApiException {
        MyStompSessionHandler sessionHandler = new MyStompSessionHandler();
        StompSession session = connectWebSocketStompClient(sessionHandler);
        session.setAutoReceipt(true);

        FieldUtils.writeField(StompCommand.RECEIPT, "body", true, true);

        initFundVersionAndTreeData();

        // Musí existovat root node
        assertNotNull(treeData.getNodes());

        // Musí existovat pouze root node
        assertTrue(treeData.getNodes().size() == 1);

        List<ArrItem> items = itemRepository.findAll();
        assertTrue(items.size() == 1); // SRD_LEVEL_TYPE // ENUM

        // Příprava objektu s daty pro odeslání
        String destination = UPDATE_DESK_ITEMS_MSG_MAPPING
                .replace("{fundVersionId}", fundVersion.getId().toString())
                .replace("{nodeId}", treeData.getNodes().iterator().next().getId().toString())
                .replace("{nodeVersion}", treeData.getNodes().iterator().next().getVersion().toString());

        ArrItemIntVO item = new ArrItemIntVO();
        item.setValue(1);
        item.setItemTypeId(findItemTypeId("SRD_NAD"));

        // vytvoření nové ArrItem
        ArrUpdateItemVO updateItem = new ArrUpdateItemVO();
        updateItem.setUpdateOp(UpdateOp.CREATE);
        updateItem.setItem(item);
        ArrUpdateItemVO[] updateItems = { updateItem };

        Receiptable receiptCreate = session.send(destination, updateItems);
        waitingForReceipt(receiptCreate);

        items = itemRepository.findAll();
        assertTrue(items.size() == 2);

        // změna ArrItem
        //item = ArrItemIntVO.newInstance(items.get(0));
        //updateItem.setUpdateOp(UpdateOp.UPDATE);

        //Receiptable receiptDelete = session.send(destination, updateItems);
        //waitingForReceipt(receiptDelete);

        //items = itemRepository.findAll();

        session.disconnect();
    }

    @Test
    public void addLevelTest() throws InterruptedException, ExecutionException, IllegalAccessException, ApiException {
        MyStompSessionHandler sessionHandler = new MyStompSessionHandler();
        StompSession session = connectWebSocketStompClient(sessionHandler);
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
        waitingForReceipt(receipt);

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

    private void waitingForReceipt(Receiptable receipt) throws InterruptedException {
        AtomicReference<ReceiptStatus> receiptStatus = new AtomicReference<ReceiptStatus>();
        receipt.addReceiptTask(() -> {
            logger.debug("Receipt received");
            receiptStatus.set(ReceiptStatus.RCP_RECEIVED);
        });
        receipt.addReceiptLostTask(() -> {
            logger.debug("Receipt lost");
            receiptStatus.set(ReceiptStatus.RCP_LOST);
        });
        while (receiptStatus.get() == null) {
            logger.info("Waiting on receipt...");
            Thread.sleep(100);
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

    private StompSession connectWebSocketStompClient(StompSessionHandler sessionHandler) throws InterruptedException, ExecutionException {
        WebSocketClient client = new StandardWebSocketClient();
        WebSocketStompClient stompClient = new WebSocketStompClient(client);

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
        }

        @Override
        public void handleTransportError(StompSession session, Throwable exception) {
            logger.error("Got an exception: ", exception);
        }
    }

}
