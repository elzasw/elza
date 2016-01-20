package cz.tacr.elza.service.eventnotification.events;

import java.util.List;

import cz.tacr.elza.service.eventnotification.events.vo.NodeInfo;


/**
 * Událost pro přesunutí uzlu ve stromu.
 *
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 20.01.2016
 */
public class EventNodeMove extends AbstractEventVersion<EventNodeMove> {

    /**
     * Statický uzel před/za/pod který přesouváme.
     */
    private NodeInfo staticLevel;
    /**
     * Seznam přesouvaných uzlů.
     */
    private List<NodeInfo> transportLevels;


    public EventNodeMove(final EventType eventType,
                         final Integer versionId,
                         final NodeInfo staticLevel,
                         final List<NodeInfo> transportLevels) {
        super(eventType, versionId);
        this.staticLevel = staticLevel;
        this.transportLevels = transportLevels;
    }


    @Override
    public String toString() {
        return "EventNodeMove{" +
                "EventType=" + getEventType() +
                ", versionId=" + getVersionId() +
                ", staticLevel=" + staticLevel +
                ", transportLevels=" + transportLevels +
                '}';
    }
}
