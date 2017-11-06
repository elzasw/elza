package cz.tacr.elza.websocket.core;

import java.util.LinkedList;
import java.util.Queue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.support.MessageHandlingRunnable;

/**
 * Task processor for one WebSocket. Thread of processor will wait for new tasks.
 * Incoming tasks are stored in queue. Add and block methods are thread-safe.
 */
public class WebSocketTaskProcessor implements Runnable {

	private static final Logger LOG = LoggerFactory.getLogger(WebSocketTaskProcessor.class);

	private final Queue<MessageHandlingRunnable> taskQueue = new LinkedList<>();

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
			return false;
		}
		taskQueue.add(task);
		notifyAll();
		return true;
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
				mhr.run();
			}
		} catch (Exception e) {
			LOG.error("WebSocket task processor was stopped due to unhandled exception:", e);
			block();
		}
	}
}