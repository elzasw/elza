package cz.tacr.elza.dataexchange.input.storage;

public interface StorageListener {

    /**
     * @param item source
     * @param entity merged entity
     */
    void onEntityPersist(EntityWrapper item, Object entity);
}
