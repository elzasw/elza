package cz.tacr.elza.controller.vo;

import cz.tacr.elza.asynchactions.AsyncWorkerVO;
import cz.tacr.elza.domain.AsyncTypeEnum;

import java.util.List;

public class ArrAsyncRequestVO {

    private final AsyncTypeEnum type;

    private final double load;
    private final int requestPerHour;
    private final int waitingRequests;
    private final int runningThreadCount;
    private final int totalThreadCount;
    private final List<AsyncWorkerVO> currentThreads;

    public ArrAsyncRequestVO(AsyncTypeEnum type, double load, int requestPerHour, int waitingRequests, int runningThreadCount, int totalThreadCount, List<AsyncWorkerVO> currentThreads) {
        this.type = type;
        this.load = load;
        this.requestPerHour = requestPerHour;
        this.waitingRequests = waitingRequests;
        this.runningThreadCount = runningThreadCount;
        this.totalThreadCount = totalThreadCount;
        this.currentThreads = currentThreads;
    }

    public double getLoad() {
        return load;
    }

    public int getRequestPerHour() {
        return requestPerHour;
    }

    public int getWaitingRequests() {
        return waitingRequests;
    }

    public int getRunningThreadCount() {
        return runningThreadCount;
    }

    public int getTotalThreadCount() {
        return totalThreadCount;
    }

    public List<AsyncWorkerVO> getCurrentThreads() {
        return currentThreads;
    }

    public AsyncTypeEnum getType() {
        return type;
    }

}
