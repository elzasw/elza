package cz.tacr.elza.deimport.processor;

/**
 * Processor for data exchange item. Implementations are not thread safe and new
 * instance must be created for each item.
 */
public interface ItemProcessor {

    /**
     * Process import item.
     */
    void process(Object item);
}
