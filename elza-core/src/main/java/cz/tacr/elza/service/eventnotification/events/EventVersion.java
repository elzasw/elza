package cz.tacr.elza.service.eventnotification.events;

/**
 * Událost, která se vztahuje k verzi stromu.
 *
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 20.01.2016
 */
public class EventVersion extends AbstractEventSimple {

    /**
     * Id verze stromu.
     */
    private Integer versionId;


    public EventVersion(final EventType eventType, final Integer versionId) {
        super(eventType);
        this.versionId = versionId;
    }

    public Integer getVersionId() {
        return versionId;
    }
}
