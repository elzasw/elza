package cz.tacr.elza.asynchactions;

import java.util.List;

public interface IAsyncWorker extends Runnable {

    AsyncRequest getRequest();

    Long getBeginTime();

    Long getRunningTime();

    void terminate();

    List<AsyncRequest> getRequests();

}
