package cz.tacr.elza.dataexchange.output.loaders;

import java.util.ArrayList;

import org.apache.commons.lang3.Validate;

import cz.tacr.elza.core.DatabaseType;

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
            be.onLoadEnd();
        }

        batch.clear();
    }

    /**
     * Called during flush only when entries are not empty.
     *
     * @param entries not-empty
     */
    protected abstract void processBatch(ArrayList<BatchEntry> entries);

    protected void onRequestLoad(RES result, LoadDispatcher<RES> dispatcher) {
    }

    public class BatchEntry {

        private final REQ request;

        private final LoadDispatcher<RES> dispatcher;

        private BatchEntry(REQ request, LoadDispatcher<RES> dispatcher) {
            this.request = Validate.notNull(request);
            this.dispatcher = Validate.notNull(dispatcher);
        }

        public REQ getRequest() {
            return request;
        }

        public void addResult(RES result) {
            Validate.notNull(result);

            dispatcher.onLoad(result);
            onRequestLoad(result, dispatcher);
        }

        private void onLoadEnd() {
            dispatcher.onLoadEnd();
        }
    }
}
