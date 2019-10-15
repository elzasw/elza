package cz.tacr.elza.service.eventnotification.events;

/**
 * Událost, která která nastala nad entitou konkrétní verze stromu a je potřeba její cache stromu přenačíst.
 *
 * @author Petr Compel [<a href="mailto:petr.compel@marbes.cz">petr.compel@marbes.cz</a>]
 * @since 15.01.2016
 */
public class EventStringInVersion extends EventVersion {

    /**
     * Id entity.
     */
    private String entityString;

    public EventStringInVersion(final EventType eventType, final Integer versionId, final String entityString) {
        super(eventType, versionId);
        this.entityString = entityString;
    }

    public String getEntityId() {
        return entityString;
    }

    public void setEntityId(final String entityString) {
        this.entityString = entityString;
    }
}
