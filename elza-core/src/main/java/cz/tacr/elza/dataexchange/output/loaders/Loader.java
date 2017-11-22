package cz.tacr.elza.dataexchange.output.loaders;

/**
 * Interface for delayed loader based on request and result dispatcher.
 */
public interface Loader<REQ, RES> {

    void addRequest(REQ request, LoadDispatcher<RES> dispatcher);

    void flush();
}
