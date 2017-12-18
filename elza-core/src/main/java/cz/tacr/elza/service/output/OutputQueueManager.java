package cz.tacr.elza.service.output;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.lang.Validate;

class OutputQueueManager {

    /**
     * Internal state. Do not change definition order!
     */
    private enum State {
        INIT, RUNNING, STOPPING, TERMINATED
    }

    private final LinkedList<OutputGeneratorWorker> workerQueue = new LinkedList<>();

    private final List<OutputGeneratorWorker> runningWorkers;

    private final ExecutorService executorService;

    private final int threadPoolSize;

    private volatile State state = State.INIT;

    private Thread managerThread;

    public OutputQueueManager(int threadPoolSize) {
        Validate.isTrue(threadPoolSize > 0);

        this.runningWorkers = new ArrayList<>(threadPoolSize);
        this.executorService = Executors.newFixedThreadPool(threadPoolSize);
        this.threadPoolSize = threadPoolSize;
    }

    /**
     * Start async queue processing.
     */
    public synchronized void start() {
        Validate.isTrue(state == State.INIT);

        state = State.RUNNING;
        managerThread = new Thread(this::run, "OutputQueueManager");
        managerThread.start();
    }

    /**
     * Caller stop async queue processing. This operation will block caller thread
     * until manager thread does not terminate.
     *
     * When terminated no more workers will be passed to execution.
     */
    public synchronized void stop() {
        if (state == State.RUNNING) {
            state = State.STOPPING;
            // notify manager thread about stopping
            notify();
            // wait until manager thread does not terminate
            while (state != State.TERMINATED) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                }
            }
        }
    }

    /**
     * Adds worker to queue for processing.
     *
     * @return False when worker already in queue.
     */
    public synchronized void addWorker(OutputGeneratorWorker worker) {
        Validate.isTrue(state.ordinal() <= State.RUNNING.ordinal());
        Validate.notNull(worker);

        workerQueue.addLast(worker);
        // notify manager thread about new worker
        if (runningWorkers.size() < threadPoolSize) {
            notify();
        }
    }

    private synchronized void run() {
        while (state == State.RUNNING) {
            if (runningWorkers.size() >= threadPoolSize || workerQueue.isEmpty()) {
                try {
                    wait();
                } catch (InterruptedException e) {
                }
                continue;
            }

            OutputGeneratorWorker worker = workerQueue.removeFirst();

            runningWorkers.add(worker);
            executorService.submit(() -> {
                try {
                    worker.run();
                } finally {
                    onWorkerFinished(worker);
                }
            });
        }
        state = State.TERMINATED;
    }

    private synchronized void onWorkerFinished(OutputGeneratorWorker worker) {
        runningWorkers.remove(worker);
        // notify manager thread about ended worker
        if (workerQueue.size() > 0) {
            notify();
        }
    }
}
