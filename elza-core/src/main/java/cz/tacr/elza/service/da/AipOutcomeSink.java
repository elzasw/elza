package cz.tacr.elza.service.da;

import javax.annotation.Nullable;

import cz.tacr.elza.api.DaAipActionItemState;
import cz.tacr.elza.domain.DaSyncQueueItem;

/**
 * Records what an action did to the individual AIPs it was asked to act on.
 *
 * The processing reports through this interface so it does not have to know whether an action is
 * being recorded at all: the same code runs for an action a user asked for and for the automatic
 * processing, which has nobody to report to and uses {@link #NONE}.
 */
public interface AipOutcomeSink {

    /** Sink of processing nobody asked for - the outcomes have nowhere to be reported. */
    AipOutcomeSink NONE = new AipOutcomeSink() {

        @Override
        public void record(Integer aipId, DaAipActionItemState state, @Nullable String message) {
            // nothing asked for this processing
        }

        @Override
        public void enqueued(Integer aipId, DaSyncQueueItem queueItem) {
            // nothing asked for this processing
        }
    };

    void record(Integer aipId, DaAipActionItemState state, @Nullable String message);

    /**
     * The AIP is being carried out through the synchronization queue. The outcome is not known
     * yet; the queue item carries the action item and finishes it once the digital archive has
     * answered.
     */
    void enqueued(Integer aipId, DaSyncQueueItem queueItem);

    default void finished(Integer aipId) {
        record(aipId, DaAipActionItemState.FINISHED, null);
    }

    /** The action could not apply to this AIP; the reason is what the user is shown. */
    default void skipped(Integer aipId, String reason) {
        record(aipId, DaAipActionItemState.SKIPPED, reason);
    }

    default void failed(Integer aipId, String reason) {
        record(aipId, DaAipActionItemState.ERROR, reason);
    }
}
