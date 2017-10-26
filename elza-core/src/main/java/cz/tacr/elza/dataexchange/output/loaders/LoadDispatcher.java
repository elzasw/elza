package cz.tacr.elza.dataexchange.output.loaders;

public interface LoadDispatcher<R> {

    void onLoadBegin();

    void onLoad(R result);

    void onLoadEnd();
}
