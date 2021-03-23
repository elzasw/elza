package cz.tacr.elza.websocket;

import org.springframework.lang.Nullable;
import org.springframework.messaging.simp.stomp.ConnectionHandlingStompSession;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSessionHandler;
import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import cz.tacr.elza.websocket.ElzaStompSession.ReceiptAcceptor;

public class WebSocketStompClientElza extends WebSocketStompClient {

    ReceiptAcceptor receiptAcceptor;

    public WebSocketStompClientElza(WebSocketClient webSocketClient, ReceiptAcceptor receiptAcceptor) {
        super(webSocketClient);
        this.receiptAcceptor = receiptAcceptor;
    }

    /**
     * Factory method for create and configure a new session.
     * @param connectHeaders headers for the STOMP CONNECT frame
     * @param handler the handler for the STOMP session
     * @return the created session
     */
    @Override
    protected ConnectionHandlingStompSession createSession(@Nullable StompHeaders connectHeaders, StompSessionHandler handler) {

        connectHeaders = processConnectHeaders(connectHeaders);
        ElzaStompSession session = new ElzaStompSession(handler, connectHeaders, receiptAcceptor);
        session.setMessageConverter(getMessageConverter());
        session.setTaskScheduler(getTaskScheduler());
        session.setReceiptTimeLimit(getReceiptTimeLimit());
        return session;
    }
}
