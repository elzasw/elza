package cz.tacr.elza.service.eventnotification.events;

public class EventIdRequestIdInVersion extends EventVersion {

    /**
     * Id entity.
     */
    private Integer entityId;

    /**
     * Id požadavku.
     */
    private Integer requestId;

    public EventIdRequestIdInVersion(final EventType eventType, final Integer versionId, final Integer entityId, final Integer requestId) {
        super(eventType, versionId);
        this.entityId = entityId;
        this.requestId = requestId;
    }

    public Integer getEntityId() {
        return entityId;
    }

    public void setEntityId(final Integer entityId) {
        this.entityId = entityId;
    }

    public Integer getRequestId() {
        return requestId;
    }

    public void setRequestId(final Integer requestId) {
        this.requestId = requestId;
    }
}
