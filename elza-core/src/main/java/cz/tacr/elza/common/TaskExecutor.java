package cz.tacr.elza.common;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.lang.Validate;

public class TaskExecutor {

    /**
     * Internal state. Do not change definition order!
     */
    private enum State {
        INIT, RUNNING, STOPPING, TERMINATED
    }

    private final LinkedList<Runnable> taskQueue = new LinkedList<>();

    private final List<Runnable> runningTasks;

    private final ExecutorService executorService;

    private final int threadPoolSize;

    private volatile State state = State.INIT;

    private Thread managerThread;

    public TaskExecutor(int threadPoolSize) {
        Validate.isTrue(threadPoolSize > 0);

        this.runningTasks = new ArrayList<>(threadPoolSize);
        this.executorService = Executors.newFixedThreadPool(threadPoolSize);
        this.threadPoolSize = threadPoolSize;
    }

    /**
     * Start async queue processing.
     */
    public synchronized void start() {
        Validate.isTrue(state == State.INIT);

        state = State.RUNNING;
        managerThread = new Thread(this::run, "TaskExecutor_managerThread");
        managerThread.start();
    }

    /**
     * Caller stop async queue processing. This operation will block caller thread
     * until manager thread terminates.
     *
     * When terminated no more tasks will be passed to execution.
     */
    public synchronized void stop() {
        if (state == State.RUNNING) {
            state = State.STOPPING;
            // notify manager thread about stopping
            notifyAll();
            // wait until manager thread does not terminate
            while (state != State.TERMINATED) {
                try {
                    wait(100);
                } catch (InterruptedException e) {
                    // nothing to do here
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    /**
     * Adds task to queue for processing.
     *
     * @return False when task already in queue.
     */
    public synchronized void addTask(Runnable task) {
        Validate.isTrue(state.ordinal() <= State.RUNNING.ordinal());
        Validate.notNull(task);

        taskQueue.addLast(task);
        // notify manager thread about new task
        if (runningTasks.size() < threadPoolSize) {
            notifyAll();
        }
    }

    private synchronized void run() {
        while (state == State.RUNNING) {
            if (runningTasks.size() >= threadPoolSize || taskQueue.isEmpty()) {
                try {
                    wait();
                } catch (InterruptedException e) {
                    // nothing to do here
                    state = State.TERMINATED;
                    Thread.currentThread().interrupt();
                }
                continue;
            }

            Runnable task = taskQueue.removeFirst();

            runningTasks.add(task);
            executorService.submit(() -> {
                try {
                    task.run();
                } finally {
                    onTaskFinished(task);
                }
            });
        }
        state = State.TERMINATED;
    }

    private synchronized void onTaskFinished(Runnable task) {
        runningTasks.remove(task);
        // notify manager thread about ended task
        if (taskQueue.size() > 0) {
            notifyAll();
        }
    }
}
