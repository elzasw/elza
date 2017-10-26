package cz.tacr.elza.dataexchange.output.loaders;

public interface Loader<REQ, RES> {

    void addRequest(REQ request, LoadDispatcher<RES> dispatcher);

    void flush();
}
