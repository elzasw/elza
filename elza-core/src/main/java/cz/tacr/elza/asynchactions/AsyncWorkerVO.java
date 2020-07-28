package cz.tacr.elza.asynchactions;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

public class AsyncWorkerVO {

    private Integer fundVersionId;
    private Long requestId;
    private LocalDateTime beginTime;
    private Long runningTime;
    private Integer currentId;

    public AsyncWorkerVO(Integer fundVersionId, Long requestId, Long beginTime, Long runningTime, Integer currentId) {
        this.fundVersionId = fundVersionId;
        this.requestId = requestId;
        this.beginTime = beginTime != null ? Instant.ofEpochMilli(beginTime).atZone(ZoneId.systemDefault()).toLocalDateTime() : null;
        this.runningTime = runningTime;
        this.currentId = currentId;
    }

    public Integer getFundVersionId() {
        return fundVersionId;
    }

    public Long getRequestId() {
        return requestId;
    }

    public LocalDateTime getBeginTime() {
        return beginTime;
    }

    public Long getRunningTime() {
        return runningTime;
    }

    public Integer getCurrentId() {
        return currentId;
    }
}
