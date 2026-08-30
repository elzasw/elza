package cz.tacr.elza.service.eventnotification.events;

public class EventFundImport extends EventFund {

    private final String message;

    public EventFundImport(EventType eventType, Integer fundId, Integer versionId, String message) {
        super(eventType, fundId, versionId);
        this.message = message;
    }

    public String getMessage() { 
    	return message;
    }
}
