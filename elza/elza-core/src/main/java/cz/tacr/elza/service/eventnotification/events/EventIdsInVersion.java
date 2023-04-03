package cz.tacr.elza.service.eventnotification.events;

import java.util.Arrays;

/**
 * Událost, která která nastala nad entitama konkrétní verze stromu.
 *
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

    @Override
    public String toString() {
        String entIds = Arrays.toString(entityIds);
        return "EventIdsInVersion{" +
                "fundVersionId=" + getVersionId() +
                ", entityIds=" + entIds + "}";
    }
}
