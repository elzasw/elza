package cz.tacr.elza.asynchactions;

import java.util.List;

public interface IAsyncWorker extends Runnable {

    IAsyncRequest getRequest();

    Long getBeginTime();

    Long getRunningTime();

    void terminate();

    List<? extends IAsyncRequest> getRequests();

}
