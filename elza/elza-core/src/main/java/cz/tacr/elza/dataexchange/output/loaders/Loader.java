package cz.tacr.elza.dataexchange.output.loaders;

/**
 * Interface for delayed loader based on request and result dispatcher.
 */
public interface Loader<REQ, RES> {

	/**
	 * Add request.
	 * @param request
	 * @param dispatcher
	 */
    void addRequest(REQ request, LoadDispatcher<RES> dispatcher);

	/**
	 * Process all batch entries and sets results through {@link LoadDispatcher#onLoad(Object)}.
	 * @param entries
	 */
    void flush();
}
