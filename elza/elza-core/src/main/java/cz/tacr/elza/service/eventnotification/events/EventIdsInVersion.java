package cz.tacr.elza.service.eventnotification.events;

/**
 * Událost, která která nastala nad entitama konkrétní verze stromu.
 *
 * @author Martin Šlapa
 * @since 12.2.2016
 */
public class EventIdsInVersion extends EventVersion {

    /**
     * Id entit.
     */
    private Integer[] entityIds;

    public EventIdsInVersion(final EventType eventType, final Integer versionId, final Integer... entityIds) {
        super(eventType, versionId);
        this.entityIds = entityIds;
    }

    public Integer[] getEntityIds() {
        return entityIds;
    }

    public void setEntityIds(final Integer... entityId) {
        this.entityIds = entityId;
    }
}
