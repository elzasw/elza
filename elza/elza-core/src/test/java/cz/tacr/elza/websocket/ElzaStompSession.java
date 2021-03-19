package cz.tacr.elza.websocket;

import org.springframework.messaging.Message;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.simp.stomp.DefaultStompSession;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSessionHandler;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.util.Assert;

public class ElzaStompSession extends DefaultStompSession {

    public interface ReceiptAcceptor {
        void handleReceiptReceived(String receiptId, Message<byte[]> message);
    };

    ReceiptAcceptor rcpAcceptor;

    public ElzaStompSession(StompSessionHandler sessionHandler, StompHeaders connectHeaders, ReceiptAcceptor receiptAcceptor) {
        super(sessionHandler, connectHeaders);
        this.rcpAcceptor = receiptAcceptor;
    }

    @Override
    public void handleMessage(Message<byte[]> message) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        Assert.state(accessor != null, "No StompHeaderAccessor");

        accessor.setSessionId(getSessionId());
        StompCommand command = accessor.getCommand();

        if (StompCommand.RECEIPT.equals(command)) {
            String receiptId = accessor.getReceiptId();
            if (rcpAcceptor != null) {
                rcpAcceptor.handleReceiptReceived(receiptId, message);
            }
        }
        super.handleMessage(message);
    }
}
