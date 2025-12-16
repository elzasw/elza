package cz.tacr.elza.dataexchange.output.loaders;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import cz.tacr.elza.common.db.DatabaseType;

/**
 * Abstract implementation for batch loader.
 * 
 * REQ - request type for loader
 * RES - result type for loader
 */
public abstract class AbstractBatchLoader<REQ, RES> implements Loader<REQ, RES> {

    protected final int batchSize;

    private final ArrayList<BatchEntry> batch;

    /**
     * Class for single batch item.
     */
    protected class BatchEntry {

        private final REQ request;

        private final LoadDispatcher<RES> dispatcher;

        private BatchEntry(REQ request, LoadDispatcher<RES> dispatcher) {
            this.request = Objects.requireNonNull(request);
            this.dispatcher = Objects.requireNonNull(dispatcher);
        }

        public REQ getRequest() {
            return request;
        }

        public void setResult(RES result) {
            Objects.requireNonNull(result);

            dispatcher.onLoad(result);
            onBatchEntryLoad(dispatcher, result);
        }

        private void onProcessed() {
            dispatcher.onLoadEnd();
        }
    }

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
     * Groups requests with same id. Key set is used for query as IN search. Map of
     * values is used as lookup for result.
     */
    protected Map<REQ, List<BatchEntry>> getEntityIdLookup(Collection<BatchEntry> entries) {
        Map<REQ, List<BatchEntry>> lookup = new HashMap<>(entries.size());
        for (BatchEntry entry : entries) {
            REQ id = entry.getRequest();
            List<BatchEntry> group = lookup.computeIfAbsent(id, k -> new ArrayList<>());
            group.add(entry);
        }
        return lookup;
    }

    /**
     * Process all batch entries and sets results through {@link BatchEntry#setResult(Object)}.
     *
     * @param entries not-empty
     */
    protected abstract void processBatch(List<BatchEntry> entries);

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
}
