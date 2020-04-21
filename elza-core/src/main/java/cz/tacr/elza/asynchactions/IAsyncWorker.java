package cz.tacr.elza.asynchactions;

import java.util.concurrent.Callable;

public interface IAsyncWorker extends Runnable {

    Integer getFundVersionId();

    Long getRequestId();

    Long getBeginTime();

    Long getRunningTime();

    Integer getCurrentId();

    void terminate();


}
