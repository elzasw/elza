package cz.tacr.elza.websocket;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.messaging.simp.broker.AbstractBrokerMessageHandler;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.util.StringUtils;

/**
 * @author Jaroslav Todt [jaroslav.todt@lightcomp.cz]
 * @since 31.8.2016
 */
public class StompExtensionMessageHandler extends AbstractBrokerMessageHandler {

    private static final byte[] EMPTY_PAYLOAD = new byte[0];

    public StompExtensionMessageHandler(final SubscribableChannel inboundChannel, final MessageChannel outboundChannel,
                                        final SubscribableChannel brokerChannel) {
        super(inboundChannel, outboundChannel, brokerChannel);
    }

    @Override
    protected void handleMessageInternal(final Message<?> message) {
//		System.out.println("$$$$$$$$$ handleMessageInternal");
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
        if (accessor.getCommand() != null) {
            handleStompMessage(accessor);
        }
    }

    private void handleStompMessage(final StompHeaderAccessor clientAccessor) {
        if (StompCommand.SEND.equals(clientAccessor.getCommand())) {
            if (StringUtils.hasLength(clientAccessor.getReceipt())) {
                sendReceipt(clientAccessor);
            }
        }
    }

    private void sendReceipt(final StompHeaderAccessor clientAccessor) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.RECEIPT);
        accessor.setSessionId(clientAccessor.getSessionId());
        accessor.setUser(clientAccessor.getUser());
        accessor.setReceiptId(clientAccessor.getReceipt());

        // Send message
//		getClientOutboundChannel().send(new GenericMessage<>(EMPTY_PAYLOAD, accessor.getMessageHeaders()));
//		getClientOutboundChannel().send(new GenericMessage<>("ahoj".getBytes(), accessor.getMessageHeaders()));
//		System.out.println("----------------------------------");
    }
}