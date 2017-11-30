package cz.tacr.elza.dataexchange.input.storage;

/**
 * Metrics for persistent entity.
 */
public interface EntityMetrics {

    /**
     * Heuristic value which determines memory footprint of entity.
     */
    long getMemoryScore();
}