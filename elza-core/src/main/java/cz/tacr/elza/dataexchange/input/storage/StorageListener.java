package cz.tacr.elza.dataexchange.input.storage;

public interface StorageListener {

    /**
     * @param item source
     */
    void onEntityPersist(EntityWrapper item);
}
