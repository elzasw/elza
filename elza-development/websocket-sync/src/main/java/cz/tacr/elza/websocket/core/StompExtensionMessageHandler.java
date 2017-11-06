package cz.tacr.elza.websocket.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.messaging.simp.broker.AbstractBrokerMessageHandler;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;

public class StompExtensionMessageHandler extends AbstractBrokerMessageHandler {

	private static final Logger logger = LoggerFactory.getLogger(StompExtensionMessageHandler.class);

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
		logger.info("incoming/outgoing stomp message, command:" + clientAccessor.getCommand());
	}
}