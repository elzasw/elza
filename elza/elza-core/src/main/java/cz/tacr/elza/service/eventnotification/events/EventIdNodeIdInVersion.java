package cz.tacr.elza.service.eventnotification.events;

import java.util.List;

public class EventIdNodeIdInVersion extends EventVersion {

    /**
     * Id entity.
     */
    private Integer entityId;

    /**
     * Id node.
     */
    private List<Integer> nodeIds;

    public EventIdNodeIdInVersion(final EventType eventType, final Integer versionId, final Integer entityId, final List<Integer>  nodeIds) {
        super(eventType, versionId);
        this.entityId = entityId;
        this.nodeIds = nodeIds;
    }

    public Integer getEntityId() {
        return entityId;
    }

    public void setEntityId(final Integer entityId) {
        this.entityId = entityId;
    }

    public List<Integer> getNodeIds() {
        return nodeIds;
    }

    public void setNodeIds(final List<Integer> nodeIds) {
        this.nodeIds = nodeIds;
    }
}
