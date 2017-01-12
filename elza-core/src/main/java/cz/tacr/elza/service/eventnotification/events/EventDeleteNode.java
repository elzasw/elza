package cz.tacr.elza.service.eventnotification.events;

/**
 * Událost smazání uzlu.
 *
 * @author Martin Šlapa
 * @since 3.2.2016
 */
public class EventDeleteNode extends EventVersion {

    private Integer nodeId;

    private Integer parentNodeId;

    public EventDeleteNode(final EventType eventType,
                           final Integer versionId,
                           final Integer nodeId,
                           final Integer parentNodeId) {
        super(eventType, versionId);
        this.nodeId = nodeId;
        this.parentNodeId = parentNodeId;
    }

    public Integer getNodeId() {
        return nodeId;
    }

    public void setNodeId(final Integer nodeId) {
        this.nodeId = nodeId;
    }

    public Integer getParentNodeId() {
        return parentNodeId;
    }

    public void setParentNodeId(final Integer parentNodeId) {
        this.parentNodeId = parentNodeId;
    }
}

