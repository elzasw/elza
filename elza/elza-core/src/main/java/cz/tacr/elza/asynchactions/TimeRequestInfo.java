package cz.tacr.elza.asynchactions;

/**
 * Třída pro uchovávání informace o zpracovaných požadavcích pro AsyncRequestService
 */
public class TimeRequestInfo {

    private final Long timeFinished;

    public TimeRequestInfo() {
        this.timeFinished = System.currentTimeMillis();
    }

    public Long getTimeFinished() {
        return timeFinished;
    }
}
