package cz.tacr.elza.service.eventnotification.events;

/**
 * Událost, která která nastala nad entitou konkrétní verze stromu a je potřeba její cache stromu přenačíst.
 *
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 15.01.2016
 */
public class EventIdInVersion extends EventVersion {

    /**
     * Id entity.
     */
    private Integer entityId;

    public EventIdInVersion(final EventType eventType, final Integer versionId, final Integer entityId) {
        super(eventType, versionId);
        this.entityId = entityId;
    }

    public Integer getEntityId() {
        return entityId;
    }

    public void setEntityId(final Integer entityId) {
        this.entityId = entityId;
    }
}
