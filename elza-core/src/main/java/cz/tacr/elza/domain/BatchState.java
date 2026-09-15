package cz.tacr.elza.domain;

/**
 * State of an import batch.
 *
 * <p>The states fall into three groups and the distinction drives what may be done with a batch:
 *
 * <ul>
 * <li><b>running</b> ({@link #IN_PROGRESS}, {@link #TEST_IN_PROGRESS}) - the runner is walking the
 *     items and an entry in {@code arr_async_request} carries the request. Nothing may remove the
 *     batch underneath it.</li>
 * <li><b>at rest, resumable</b> ({@link #PREPARATION}, {@link #TEST_FINISHED}, {@link #PAUSED},
 *     {@link #FAILED}) - no runner, no queue entry, but a further run is still a legal transition,
 *     so the input files are what the next run would read.</li>
 * <li><b>final</b> ({@link #FINISHED}, {@link #CANCELLED}) - no successor state at all.</li>
 * </ul>
 */
public enum BatchState {
    PREPARATION,
    IN_PROGRESS,
    TEST_IN_PROGRESS,
    TEST_FINISHED,
    PAUSED,
    FAILED,
    FINISHED,
    CANCELLED;

    /**
     * True while the runner may be walking the items of the batch. These are the only states in
     * which a request for the batch can be waiting in the asynchronous queue, so they are also the
     * only ones in which the batch must not be removed.
     */
    public boolean isRunning() {
        return this == IN_PROGRESS || this == TEST_IN_PROGRESS;
    }

    /**
     * True when no user action can move the batch to another state any more.
     */
    public boolean isFinal() {
        return this == FINISHED || this == CANCELLED;
    }
}
