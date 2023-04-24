package cz.tacr.elza.websocket;

import java.nio.charset.StandardCharsets;
import java.util.Deque;
import java.util.LinkedList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.support.MessageHandlingRunnable;

/**
 * Třída umožňuje přidání požadavků pro zpracování z jednoho konkrétního klienta
 * (session).
 *
 * Task processor for one WebSocket. Thread of processor will wait for new
 * tasks.
 * Incoming tasks are stored in queue. Add and block methods are thread-safe.
 */
public class WebSocketTaskProcessor implements Runnable {

	private static final Logger LOG = LoggerFactory.getLogger(WebSocketTaskProcessor.class);

    private final Deque<MessageHandlingRunnable> taskQueue = new LinkedList<>();

	private boolean blocked = false;

	public synchronized boolean isBlocked() {
		return blocked;
	}

	/**
	 * Blocks processor and notifies all waiting threads.
	 */
	public synchronized void block() {
		if (blocked) {
			return;
		}
		blocked = true;
		notifyAll();
	}

	/**
	 * Adds task to queue. All waiting threads will be notified.
	 *
	 * @return False if task cannot be added (processor is blocked) otherwise true.
	 */
	public synchronized boolean add(MessageHandlingRunnable task) {
		if (blocked) {
            LOG.debug("Cannot add message, queue is blocked");
			return false;
		}

		taskQueue.add(task);
		notifyAll();
		return true;
	}

    /**
     * Adds priority task to queue. All waiting threads will be notified.
     * Message will be as first in the queue
     *
     * @return False if task cannot be added (processor is blocked) otherwise true.
     */
    public synchronized boolean addPriority(MessageHandlingRunnable task) {
        if (blocked) {
            LOG.debug("Cannot add message, queue is blocked");
            return false;
        }

        taskQueue.addFirst(task);
        notifyAll();
        return true;
    }

    private void logMessage(String message, MessageHandlingRunnable task) {
        Message<?> msg = task.getMessage();
        Object payload = null;
        MessageHeaders headers = null;
        SimpMessageType msgType = null;
        MessageHandler msh = task.getMessageHandler();
        StompCommand stompCommand = null;
        if (msg != null) {
            payload = msg.getPayload();
            headers = msg.getHeaders();
            if (headers != null) {
                msgType = (SimpMessageType) headers.get("simpMessageType");
                stompCommand = (StompCommand) headers.get("stompCommand");
            }
            // convert payload to readable string
            if (SimpMessageType.MESSAGE.equals(msgType)) {
                if (payload != null && payload instanceof byte[]) {
                    byte payloadArr[] = (byte[]) payload;
                    payload = new String(payloadArr, StandardCharsets.UTF_8);
                }
            }
        }
        LOG.debug(message + ", handler: {}, headers: {}, payload: {}",
                  msh.toString(),
                  (msgType != null) ? (msgType + ((stompCommand != null) ? ("(" + stompCommand + ")") : "")) : headers,
                  payload);
    }

    /**
     * Returns last task from queue. Current thread will wait while queue is empty.
     *
     * @return Task or null when processor is blocked.
     */
	private synchronized MessageHandlingRunnable dequeue() throws InterruptedException {
		while (taskQueue.isEmpty() && !blocked) {
			wait();
		}
		if (blocked) {
			return null;
		}
		return taskQueue.remove();
	}

	@Override
	public void run() {
		try {
			while (true) {
				MessageHandlingRunnable mhr = dequeue();
				if (mhr == null) {
					return;
				}

                if (LOG.isDebugEnabled()) {
                    logMessage("Processing message", mhr);
                }

				mhr.run();
			}
		} catch (Exception e) {
			LOG.error("WebSocket task processor was stopped due to unhandled exception:", e);
			block();
		}
	}
}