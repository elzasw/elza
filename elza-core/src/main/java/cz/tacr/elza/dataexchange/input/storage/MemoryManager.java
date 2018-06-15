package cz.tacr.elza.dataexchange.input.storage;

public interface MemoryManager {

    long getAvailableMemoryScore();

    /**
     * Flush all changes and clears persistent context.
     * 
     * @param clearAll
     *            If false only managed entities manages will be cleared. If true
     *            all persistent context will be cleared.
     */
    public void flushAndClear(boolean clearAll);

    /**
     * @param item
     *            source wrapper
     * @param entity
     *            persisted or merged entity
     */
    void onEntityPersist(EntityWrapper item, Object entity);
}
