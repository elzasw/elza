package cz.tacr.elza.websocket;

import java.util.HashMap;
import java.util.Map;

import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.support.MessageHandlingRunnable;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.util.Assert;

/**
 * Třída umožňuje rozřazovat požadavky na základě klient session do konkrétních {@link WebSocketTaskProcessor},
 * které je zpracovávají.
 *
 * Ensures order of incoming or outgoing messages (depends on applied channel) by creating one
 * thread per WebSocket connection. This algorithm is based on serial nature of WebSocket.
 */
public class WebSocketThreadPoolTaskExecutor extends ThreadPoolTaskExecutor {

    private static final long serialVersionUID = 1L;

    private final Map<String, WebSocketTaskProcessor> webSocketTaskProcessors = new HashMap<>();

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
	public synchronized void addSession(String sessionId) {
		Assert.notNull(sessionId, "WebSocket session id cannot be null");
		WebSocketTaskProcessor processor = new WebSocketTaskProcessor();
		if (webSocketTaskProcessors.put(sessionId, processor) != null) {
			throw new IllegalStateException("WebSocket session is already registered, id:" + sessionId);
		}
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
		Assert.notNull(sessionId, "WebSocket session id cannot be null");
		WebSocketTaskProcessor processor = webSocketTaskProcessors.get(sessionId);
		if (processor == null) {
			throw new IllegalStateException("WebSocket session does not exist, id:" + sessionId);
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
	public synchronized void removeSession(String sessionId) {
		Assert.notNull(sessionId, "WebSocket session id cannot be null");
		WebSocketTaskProcessor processor = webSocketTaskProcessors.remove(sessionId);
		if (processor == null) {
			throw new IllegalStateException("WebSocket session does not exist, id:" + sessionId);
		}
		processor.block();
	}

	/*
	 * Adds task to processor. Every WebSocket session (connection) has only one processor.
	 */
	@Override
	public void execute(Runnable task) {
		MessageHandlingRunnable mhr = (MessageHandlingRunnable) task;
		String sessionId = SimpMessageHeaderAccessor.getSessionId(mhr.getMessage().getHeaders());
		synchronized (this) {
			// System.out.println("Processing session id " + sessionId + ", " + mhr);
			WebSocketTaskProcessor processor = webSocketTaskProcessors.get(sessionId);
			if (processor == null) {
				throw new IllegalStateException("WebSocket session does not exist, id:" + sessionId);
			}
			// Add might fail but it's safe to ignore because processor was blocked during handled exception.
			processor.add(mhr);
		}
	}
}