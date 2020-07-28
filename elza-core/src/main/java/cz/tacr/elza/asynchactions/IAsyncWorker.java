package cz.tacr.elza.asynchactions;

public interface IAsyncWorker extends Runnable {

    AsyncRequest getRequest();

    Long getBeginTime();

    Long getRunningTime();

    void terminate();

}
