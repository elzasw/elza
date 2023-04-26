package cz.tacr.elza.websocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.messaging.Message;
import org.springframework.messaging.converter.SmartMessageConverter;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.AbstractSubscribableChannel;
import org.springframework.web.bind.annotation.ControllerAdvice;

/**
 * Exception handler for WebSocket controllers.
 *
 * Catches fatal exceptions which will send to client and disconnect WS connection.
 */
@ControllerAdvice(annotations = WebSocketAwareController.class)
public class MessageProcessingExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(MessageProcessingExceptionHandler.class);

    @Autowired
    @Qualifier("clientInboundChannelExecutor")
    private transient WebSocketThreadPoolTaskExecutor executor;

    @Autowired
    @Qualifier("clientOutboundChannel")
    private transient AbstractSubscribableChannel clientOutboundChannel;

    @Autowired
    @Qualifier("brokerMessageConverter")
    private transient SmartMessageConverter messageConverter;

    /**
     * Entry point for any exception caused by business logic or WebSocket communication.
     */
    @MessageExceptionHandler
    public void handle(final Exception e, final StompHeaderAccessor clientAccessor) {
        handleException(e, clientAccessor);

        String message = new StringBuilder("Processing of incomming WebSocket message caused exception, user=")
                .append(clientAccessor.getUser().getName())
                .append(", sessionId=")
                .append(clientAccessor.getSessionId())
                .append(". Sending STOMP ERROR to client.")
                .toString();
        logger.error(message, e);
    }

    protected void handleException(final Exception e, final StompHeaderAccessor clientAccessor) {
        executor.stopSessionExecution(clientAccessor.getSessionId());
        sendError(clientAccessor, new ErrorDescription(e.getMessage(), e.getStackTrace()));
    }

    private void sendError(final StompHeaderAccessor clientAccessor, final ErrorDescription description) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.ERROR);
        accessor.setSessionId(clientAccessor.getSessionId());
        accessor.setUser(clientAccessor.getUser());
        accessor.setMessage(description.getMessage());
        accessor.setReceiptId(clientAccessor.getReceipt());
        accessor.setLeaveMutable(true);

        // convert message and send
        Message<?> errorMessage = messageConverter.toMessage(description, accessor.getMessageHeaders());
        clientOutboundChannel.send(errorMessage);
    }
}
