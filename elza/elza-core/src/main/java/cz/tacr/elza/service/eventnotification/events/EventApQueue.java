package cz.tacr.elza.service.eventnotification.events;

public class EventApQueue extends AbstractEventSimple {

    private Integer accessPointId;
    
    private Integer itemQueueId; 
    
    private Integer externalSystemId;

    public EventApQueue(EventType eventType,
                        Integer accessPointId,
                        Integer itemQueueId,
                        Integer externalSystemId) {
        super(eventType);
        this.accessPointId = accessPointId;
        this.itemQueueId = itemQueueId;
        this.externalSystemId = externalSystemId;
    }

    public Integer getAccessPointId() {
        return accessPointId;
    }

    public Integer getItemQueueId() {
        return itemQueueId;
    }

    public Integer getExternalSystemId() {
        return externalSystemId;
    }
}
