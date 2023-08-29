package cz.tacr.elza.websocket;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.support.MessageHandlingRunnable;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.socket.WebSocketSession;

/**
 * Třída umožňuje rozřazovat požadavky na základě klient session do konkrétních {@link WebSocketTaskProcessor},
 * které je zpracovávají.
 *
 * Ensures order of incoming or outgoing messages (depends on applied channel) by creating one
 * thread per WebSocket connection. This algorithm is based on serial nature of WebSocket.
 */
public class WebSocketThreadPoolTaskExecutor extends ThreadPoolTaskExecutor {

    private static final Logger LOG = LoggerFactory.getLogger(WebSocketThreadPoolTaskExecutor.class);

    private static final long serialVersionUID = 1L;

    private final Map<String, WebSocketTaskProcessor> webSocketTaskProcessors = new HashMap<>();

    private final Map<String, WebSocketSession> sessions = new HashMap<>();

	public WebSocketThreadPoolTaskExecutor() {
		setMaxPoolSize(Integer.MAX_VALUE); // maximum possible threads
		setKeepAliveSeconds(60); // keep alive thread for 60 seconds
		setQueueCapacity(0); // create always new thread
		setCorePoolSize(1);
	}

	/**
	 * Register session to executor.
	 * Must be called after connection established.
	 *
	 * @throws IllegalStateException WebSocket session is already registered.
	 * @throws IllegalArgumentException WebSocket session id cannot be null.
	 */
    public synchronized void addSession(WebSocketSession session) {
        String sessionId = session.getId();
        Validate.notNull(sessionId, "WebSocket session id cannot be null");
        LOG.debug("Adding WebSocket session: {}", sessionId);

		WebSocketTaskProcessor processor = new WebSocketTaskProcessor();
		if (webSocketTaskProcessors.put(sessionId, processor) != null) {
            throw new IllegalStateException("WebSocket session is already registered, id:" + sessionId);
		}
        sessions.put(sessionId, session);
		super.execute(processor);
	}

	/**
	 * Stops task processing for given session.
	 * New and currently waiting messages will not be executed.
	 * Server still accepting new messages until connection is open.
	 *
	 * @throws IllegalStateException WebSocket session does not exist.
	 * @throws IllegalArgumentException WebSocket session id cannot be null.
	 */
	public synchronized void stopSessionExecution(String sessionId) {
        Validate.notNull(sessionId, "WebSocket session id cannot be null");
        LOG.debug("Stop WebSocket session execution: {}", sessionId);

		WebSocketTaskProcessor processor = webSocketTaskProcessors.get(sessionId);
		if (processor == null) {
            LOG.error("WebSocket session does not exist, id: {}. Cannot be stopped.", sessionId);
            return;
		}
		processor.block();
	}

	/**
	 * Unregister session from executor.
	 * Must be called after connection closed.
	 *
	 * @throws IllegalStateException WebSocket session does not exist.
	 * @throws IllegalArgumentException WebSocket session id cannot be null.
	 */
    public synchronized void removeSession(WebSocketSession session) {
        String sessionId = session.getId();
        Validate.notNull(sessionId, "WebSocket session id cannot be null");
        LOG.debug("Remove WebSocket session: {}", sessionId);

		WebSocketTaskProcessor processor = webSocketTaskProcessors.remove(sessionId);
		if (processor == null) {
            LOG.error("WebSocket session does not exist, id: {}. Cannot be removed.", sessionId);
            return;
		}
        sessions.remove(sessionId);
		processor.block();
	}

	/*
	 * Adds task to processor. Every WebSocket session (connection) has only one processor.
	 */
	@Override
	public void execute(Runnable task) {
		MessageHandlingRunnable mhr = (MessageHandlingRunnable) task;
        Message<?> message = mhr.getMessage();
        String sessionId = SimpMessageHeaderAccessor.getSessionId(message.getHeaders());
        SimpMessageType messageType = SimpMessageHeaderAccessor.getMessageType(message.getHeaders());
		synchronized (this) {
            LOG.debug("Executing message ({}, {}) for WebSocket session: {}, handler: {}",
                      this.getThreadNamePrefix(),
                      messageType, sessionId,
                      mhr.getClass().toString());

			WebSocketTaskProcessor processor = webSocketTaskProcessors.get(sessionId);
			if (processor == null) {
                LOG.debug("WebSocket session does not exist, id: {}. Message is not processed.", sessionId);
                return;
			}
            // send heartbeat as priority/first message
            switch (messageType) {
            case CONNECT:
            case CONNECT_ACK:
            case HEARTBEAT:
                if (!processor.addPriority(mhr)) {
                    LOG.error("Cannot add priority message to the processor, sessionId: {}. Message is not processed.",
                              sessionId);
                }
                break;
            default:
                if (!processor.add(mhr)) {
                    LOG.error("Cannot add message to the processor, sessionId: {}. Message is not processed.",
                              sessionId);
                }
            }
		}
	}

    public Collection<WebSocketSession> getSessions() {
        /*Set<Principal> result = new HashSet<>();
        sessions.forEach((sId, wss) -> {
            result.add(wss.getPrincipal());
        });
        return new ArrayList<>(result);*/
        return new ArrayList<>(sessions.values());
    }
}