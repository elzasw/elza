package cz.tacr.elza.service.eventnotification.events;

/**
 * Událost s Id a String ve verzi Fund
 *
 * @author Petr Compel [<a href="mailto:petr.compel@marbes.cz">petr.compel@marbes.cz</a>]
 * @since 15.01.2016
 */
public class EventIdAndStringInVersion extends EventVersion {

    /**
     * Id entity
     */
    private Integer entityId;

    /**
     * String
     */
    private String entityString;

    public EventIdAndStringInVersion(final EventType eventType, final Integer versionId, final Integer entityId, final String entityString) {
        super(eventType, versionId);
        this.entityId = entityId;
        this.entityString = entityString;
    }

    public String getEntityString() {
        return entityString;
    }

    public void setEntityString(final String entityString) {
        this.entityString = entityString;
    }

    public Integer getEntityId() {
        return entityId;
    }

    public void setEntityId(final Integer entityId) {
        this.entityId = entityId;
    }
}
