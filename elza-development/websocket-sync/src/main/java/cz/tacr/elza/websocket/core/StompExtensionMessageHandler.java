package cz.tacr.elza.websocket.core;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.messaging.simp.broker.AbstractBrokerMessageHandler;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.util.StringUtils;

/**
 * @author Jaroslav Todt [jaroslav.todt@lightcomp.cz]
 * @since 31.8.2016
 */
public class StompExtensionMessageHandler extends AbstractBrokerMessageHandler {

	private static final byte[] EMPTY_PAYLOAD = new byte[0];

	public StompExtensionMessageHandler(SubscribableChannel inboundChannel, MessageChannel outboundChannel,
			SubscribableChannel brokerChannel) {
		super(inboundChannel, outboundChannel, brokerChannel);
	}

	@Override
	protected void handleMessageInternal(Message<?> message) {
		StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
		if (accessor.getCommand() != null) {
			handleStompMessage(accessor);
		}
	}

	private void handleStompMessage(StompHeaderAccessor clientAccessor) {
		if (StompCommand.SEND.equals(clientAccessor.getCommand())) {
			if (StringUtils.hasLength(clientAccessor.getReceipt())) {
				sendReceipt(clientAccessor);
			}
		}
	}

	private void sendReceipt(StompHeaderAccessor clientAccessor) {
		StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.RECEIPT);
		accessor.setSessionId(clientAccessor.getSessionId());
		accessor.setUser(clientAccessor.getUser());
		accessor.setReceiptId(clientAccessor.getReceipt());

		// Send message
		getClientOutboundChannel().send(new GenericMessage<>(EMPTY_PAYLOAD, accessor.getMessageHeaders()));
	}
}