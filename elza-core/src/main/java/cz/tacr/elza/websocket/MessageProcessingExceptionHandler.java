package cz.tacr.elza.websocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.messaging.Message;
import org.springframework.messaging.converter.SmartMessageConverter;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.AbstractSubscribableChannel;
import org.springframework.web.bind.annotation.ControllerAdvice;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Jaroslav Todt [jaroslav.todt@lightcomp.cz]
 * @since 27.8.2016
 */
@ControllerAdvice(annotations = WebSocketAwareController.class)
public class MessageProcessingExceptionHandler {

	private static final Logger LOG = LoggerFactory.getLogger(MessageProcessingExceptionHandler.class);

	@Autowired
	private transient WebSocketThreadPoolTaskExecutor executor;

	@Autowired
	@Qualifier("clientOutboundChannel")
	private transient AbstractSubscribableChannel clientOutboundChannel;

	@Autowired
	private transient SmartMessageConverter messageConverter;

	/**
	 * Entry point for any exception caused by business logic or WebSocket communication.
	 */
	@MessageExceptionHandler
	public void handle(Exception e, StompHeaderAccessor clientAccessor) {
		handleException(e, clientAccessor);

		String message = new StringBuilder("Processing of incomming WebSocket message caused exception, user=")
				.append(clientAccessor.getUser().getName())
				.append(", sessionId=")
				.append(clientAccessor.getSessionId())
				.append(". Sending STOMP ERROR to client.")
				.toString();
		LOG.error(message, e);
	}

	protected void handleException(Exception e, StompHeaderAccessor clientAccessor) {
		executor.stopSessionExecution(clientAccessor.getSessionId());
		sendError(clientAccessor, new ErrorDescription(e.getMessage(), e.getStackTrace()));
	}

	@Autowired
	private SimpMessagingTemplate messagingTemplate;

	private void sendError(StompHeaderAccessor clientAccessor, ErrorDescription description) {
		StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.ERROR);
		accessor.setSessionId(clientAccessor.getSessionId());
		accessor.setUser(clientAccessor.getUser());
		accessor.setMessage(description.getMessage());
		accessor.setReceiptId(clientAccessor.getReceipt());
		accessor.setLeaveMutable(true);

		// Convert message and send
		Message<?> errorMessage = messageConverter.toMessage(description, accessor.getMessageHeaders());
		clientOutboundChannel.send(errorMessage);

		// Odeslání dat zpět - návrh poslání odchytitelné chybové hlášky ze serveru, která neschodí websocket spojení - není poslána jako stmp command error
//		Map sendHeader = new HashMap();
//		sendHeader.put("receipt-id", clientAccessor.getReceipt());
//		messagingTemplate.convertAndSend("/topic/api/changes", description, sendHeader);
	}
}
