package cz.tacr.elza.asynchactions;

import org.springframework.context.ApplicationEvent;

/**
 * Třída pro předání informace o dokončení z jednotlivých workerů do servisní třídy
 */
public class AsyncRequestEvent extends ApplicationEvent {

    private final AsyncRequest asyncRequest;

    private final IAsyncWorker worker;

    private final Throwable error;

    public static AsyncRequestEvent fail(AsyncRequest asyncRequest, final IAsyncWorker worker, Throwable t) {
        return new AsyncRequestEvent(asyncRequest, worker, t);
    }

    public static AsyncRequestEvent success(AsyncRequest asyncRequest, final IAsyncWorker worker) {
        return new AsyncRequestEvent(asyncRequest, worker, null);
    }

    protected AsyncRequestEvent(AsyncRequest asyncRequest, final IAsyncWorker worker, Throwable error) {
        super(asyncRequest);
        this.asyncRequest = asyncRequest;
        this.worker = worker;
        this.error = error;
    }

    public boolean success() {
        return error == null;
    }

    public AsyncRequest getAsyncRequest() {
        return asyncRequest;
    }

    public IAsyncWorker getWorker() {
        return worker;
    }

    public Throwable getError() {
        return error;
    }
}
