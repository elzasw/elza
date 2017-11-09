package cz.tacr.elza.websocket.service;

import org.apache.commons.lang3.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.messaging.Message;
import org.springframework.messaging.converter.SmartMessageConverter;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.AbstractSubscribableChannel;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Třída pro možnost poslání zpět zprávy klientovi s odpovědí operace- např. při založení dat jsou
 * tímto callbackem poslána založená data atp. Jedná se o obdobu návratové hodnoty v kontroleru.
 */
@Service
public class WebScoketStompService {

    private static final byte[] EMPTY_PAYLOAD = new byte[0];

    @Autowired
    @Qualifier("clientOutboundChannel")
    private transient AbstractSubscribableChannel clientOutboundChannel;

    @Autowired
    private transient SimpMessagingTemplate messagingTemplate;

    @Autowired
    private transient SmartMessageConverter messageConverter;

    /**
     * Use {@link #sendReceiptAfterCommit(String, Object, StompHeaderAccessor)} instead.
     */
    @Deprecated
    public void sendMessageWithReceiptAfterCommit(String destination, Object payload, StompHeaderAccessor requestHeaders) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
            @Override
            public void afterCommit() {
                sendMessageWithReceipt(destination, payload, requestHeaders);
            }
        });
    }

    public void sendReceiptAfterCommit(Object payload, StompHeaderAccessor requestHeaders) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
            @Override
            public void afterCommit() {
                sendReceipt(payload, requestHeaders);
            }
        });
    }

    /**
     * Use {@link #sendReceipt(Object, StompHeaderAccessor)} instead.
     */
    @Deprecated
    public void sendMessageWithReceipt(String destination, Object payload, StompHeaderAccessor requestHeaders) {
        String receiptId = Validate.notEmpty(requestHeaders.getReceipt());

        StompHeaderAccessor responseHeaders = StompHeaderAccessor.create(StompCommand.SEND);
        responseHeaders.setReceiptId(receiptId);
        responseHeaders.setLeaveMutable(true); // conversion can modify headers (e.g. content type)

        String user = requestHeaders.getUser().getName();
        if (user == null) {
            messagingTemplate.convertAndSend(destination, payload, requestHeaders.getMessageHeaders());
        } else {
            messagingTemplate.convertAndSendToUser(user, destination, payload, requestHeaders.getMessageHeaders());
        }
    }

    public void sendReceipt(StompHeaderAccessor requestHeaders) {
        sendReceipt(EMPTY_PAYLOAD, requestHeaders);
    }

    public void sendReceipt(Object payload, StompHeaderAccessor requestHeaders) {
        Validate.notNull(payload);

        String sessionId = Validate.notEmpty(requestHeaders.getSessionId());
        String receiptId = Validate.notEmpty(requestHeaders.getReceipt());

        StompHeaderAccessor responseHeaders = StompHeaderAccessor.create(StompCommand.RECEIPT);
        responseHeaders.setSessionId(sessionId);
        responseHeaders.setReceiptId(receiptId);
        responseHeaders.setLeaveMutable(true); // conversion can modify headers (e.g. content type)

        Message<?> message = messageConverter.toMessage(payload, responseHeaders.getMessageHeaders());

        clientOutboundChannel.send(message);
    }
}
