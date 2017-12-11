package cz.tacr.elza.service.output;

import java.util.LinkedList;
import java.util.concurrent.RejectedExecutionException;

import org.apache.commons.lang.Validate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

public class OutputGeneratorQueue {

    private final LinkedList<OutputGeneratorWorker> workerQueue = new LinkedList<>();

    private final ThreadPoolTaskExecutor taskExecutor;

    private AsyncQueuedWorkerDispatcher dispatcher;

    public OutputGeneratorQueue(ThreadPoolTaskExecutor taskExecutor) {
        this.taskExecutor = taskExecutor;
    }

    /**
     * Adds worker to queue for processing.
     *
     * @return False when worker already in queue.
     */
    public boolean addWorker(OutputGeneratorWorker worker) {
        Validate.notNull(worker);

        synchronized (workerQueue) {
            if (workerQueue.contains(worker)) {
                return false;
            }
            workerQueue.addLast(worker);
            // notify dispatcher
            notify();
        }
        return true;
    }

    public void startDispatcher() {
        Validate.isTrue(dispatcher == null);
        dispatcher = new AsyncQueuedWorkerDispatcher();
        Thread thread = new Thread(dispatcher, "OutputGeneratorQueueDispatcher");
        thread.start();
    }

    public void stopDispatcher() {
        if (dispatcher != null) {
            dispatcher.terminate();
        }
    }

    private class AsyncQueuedWorkerDispatcher implements Runnable {

        private volatile boolean running = true;

        /**
         * When terminated no more worker will be passed to execution.
         */
        public void terminate() {
            running = false;
        }

        @Override
        public void run() {
            while (running) {
                OutputGeneratorWorker worker = getNextWorker();
                try {
                    taskExecutor.execute(worker);
                } catch (RejectedExecutionException e) {
                    recoverWorker(worker);
                    try {
                        Thread.sleep(10000);
                    } catch (InterruptedException e1) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }

        /**
         * @return First queued worker. Dispatcher will wait when queue is empty.
         */
        private OutputGeneratorWorker getNextWorker() {
            synchronized (workerQueue) {
                while (workerQueue.isEmpty()) {
                    try {
                        workerQueue.wait();
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
                return workerQueue.removeFirst();
            }
        }

        /**
         * Worker is returned to queued at first position.
         */
        private void recoverWorker(OutputGeneratorWorker worker) {
            synchronized (workerQueue) {
                workerQueue.remove(worker); // when exists due to multiple thread access
                workerQueue.addFirst(worker);
            }
        }
    }
}
