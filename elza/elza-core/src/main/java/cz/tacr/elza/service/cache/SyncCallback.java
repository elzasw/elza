package cz.tacr.elza.service.cache;

/**
 * Interface pro rozeznání objektů, které se mají serializovat.
 *
 * @since 20.07.2017
 */
@FunctionalInterface
public interface SyncCallback {

    void call();

}
