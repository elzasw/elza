package cz.tacr.elza.asynchactions;

import cz.tacr.elza.domain.AsyncTypeEnum;

/**
 * Třída pro uchovávání informace o zpracovaných požadavcích pro AsyncRequestService
 */
public class TimeRequestInfo{

    private AsyncTypeEnum type;

    private Long timeFinished;

    public TimeRequestInfo(AsyncTypeEnum type, Long timeFinished) {
        this.type = type;
        this.timeFinished = timeFinished;
    }

    public AsyncTypeEnum getType() {
        return type;
    }

    public Long getTimeFinished() {
        return timeFinished;
    }
}
