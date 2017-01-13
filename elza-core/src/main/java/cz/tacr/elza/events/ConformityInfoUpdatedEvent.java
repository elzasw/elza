package cz.tacr.elza.events;

/**
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 2.12.2015
 */
public class ConformityInfoUpdatedEvent {

    private Integer nodeId;

    public ConformityInfoUpdatedEvent(final Integer nodeId) {
        this.nodeId = nodeId;
    }

    public Integer getNodeId() {
        return nodeId;
    }
}
