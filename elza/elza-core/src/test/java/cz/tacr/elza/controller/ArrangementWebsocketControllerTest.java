package cz.tacr.elza.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Type;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.service.FundLevelService;
import cz.tacr.elza.test.ApiException;
import cz.tacr.elza.test.controller.vo.Fund;

public class ArrangementWebsocketControllerTest extends AbstractControllerTest {

    static private Logger logger = LoggerFactory.getLogger(ArrangementWebsocketControllerTest.class);

    enum ReceiptStatus {
        RCP_WAITING,
        RCP_RECEIVED,
        RCP_LOST
    };

    @Test
    public void addLevelTest() throws ApiException, InterruptedException, ExecutionException, IllegalAccessException {
        MyStompSessionHandler sessionHandler = new MyStompSessionHandler();
        StompSession session = connectWebSocketStompClient(sessionHandler);

        FieldUtils.writeField(StompCommand.RECEIPT, "body", true, true);

        //session.subscribe("/topic/api/changes", sessionHandler); // funguje i bez tohoto řádku

        Fund fund = createFund("Jmeno", "kod");
        helperTestService.waitForWorkers();
        ArrFundVersionVO fundVersion = getOpenVersion(fund);

        ArrangementController.FaTreeParam input = new ArrangementController.FaTreeParam();
        input.setVersionId(fundVersion.getId());
        TreeData treeData = getFundTree(input);

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

        session.setAutoReceipt(true);
        Receiptable receipt = session.send("/app/arrangement/levels/add", addLevelParam);

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
            logger.info("Waiting on receipt");
            Thread.sleep(100);
        }
        assertEquals(ReceiptStatus.RCP_RECEIVED, receiptStatus.get());

        // Monitorování výsledků dotazu
        List<ArrNode> nodes = nodeRepository.findAll();
        assertTrue(nodes.size() == 3);

        session.disconnect();
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
            return AddLevelParam.class;
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
