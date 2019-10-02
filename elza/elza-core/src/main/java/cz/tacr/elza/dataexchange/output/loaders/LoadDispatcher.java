package cz.tacr.elza.dataexchange.output.loaders;

/**
 * Load dispatcher interface for {@link Loader#addRequest(Object, LoadDispatcher)} request.
 */
public interface LoadDispatcher<R> {

    void onLoadBegin();

    void onLoad(R result);

    void onLoadEnd();
}
