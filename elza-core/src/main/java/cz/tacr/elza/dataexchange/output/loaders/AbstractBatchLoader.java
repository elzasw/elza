package cz.tacr.elza.dataexchange.output.loaders;

import java.util.ArrayList;

import org.apache.commons.lang3.Validate;

import cz.tacr.elza.common.db.DatabaseType;

/**
 * Abstract implementation for batch loader.
 */
public abstract class AbstractBatchLoader<REQ, RES> implements Loader<REQ, RES> {

    protected final int batchSize;

    private final ArrayList<BatchEntry> batch;

    public AbstractBatchLoader(int batchSize) {
        this.batchSize = Math.min(batchSize, DatabaseType.getCurrent().getMaxInClauseSize());
        this.batch = new ArrayList<>(this.batchSize);
    }

    @Override
    public void addRequest(REQ request, LoadDispatcher<RES> dispatcher) {
        dispatcher.onLoadBegin();

        BatchEntry entry = new BatchEntry(request, dispatcher);
        batch.add(entry);

        if (batch.size() >= batchSize) {
            flush();
        }
    }

    @Override
    public void flush() {
        if (batch.isEmpty()) {
            return;
        }

        processBatch(batch);

        for (BatchEntry be : batch) {
            be.onProcessed();
        }

        batch.clear();
    }

    /**
     * Process all batch entries and sets results through {@link BatchEntry#setResult(Object)}.
     *
     * @param entries not-empty
     */
    protected abstract void processBatch(ArrayList<BatchEntry> entries);

    /**
     * Called when result loaded. One request can have multiple results.
     * @param request
     *
     * @param request not-null
     * @param dispatcher not-null
     * @param result not-null
     */
    protected void onBatchEntryLoad(LoadDispatcher<RES> dispatcher, RES result) {
    }

    protected class BatchEntry {

        private final REQ request;

        private final LoadDispatcher<RES> dispatcher;

        private BatchEntry(REQ request, LoadDispatcher<RES> dispatcher) {
            this.request = Validate.notNull(request);
            this.dispatcher = Validate.notNull(dispatcher);
        }

        public REQ getRequest() {
            return request;
        }

        public void setResult(RES result) {
            Validate.notNull(result);

            dispatcher.onLoad(result);
            onBatchEntryLoad(dispatcher, result);
        }

        private void onProcessed() {
            dispatcher.onLoadEnd();
        }
    }
}
