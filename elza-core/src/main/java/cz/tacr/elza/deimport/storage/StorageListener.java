package cz.tacr.elza.deimport.storage;

public interface StorageListener {

    /**
     * @param item source
     * @param entity merged entity
     */
    void onEntityPersist(EntityWrapper item, Object entity);
}
