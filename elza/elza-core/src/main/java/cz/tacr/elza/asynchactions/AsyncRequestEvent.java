package cz.tacr.elza.asynchactions;

import org.springframework.context.ApplicationEvent;

/**
 * Třída pro předání informace o dokončení z jednotlivých workerů do servisní třídy
 */
public class AsyncRequestEvent extends ApplicationEvent {

    private AsyncRequestVO asyncRequestVO;

    public AsyncRequestEvent(AsyncRequestVO asyncRequestVO) {
        super(asyncRequestVO);
        this.asyncRequestVO = asyncRequestVO;
    }

    public AsyncRequestVO getAsyncRequestVO() {
        return asyncRequestVO;
    }
}
