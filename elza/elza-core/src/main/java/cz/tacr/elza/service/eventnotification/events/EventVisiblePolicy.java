package cz.tacr.elza.service.eventnotification.events;

/**
 * Událost visible policy, která která ovlivnila podstrom.
 *
 * @author Martin Šlapa
 * @since 12.2.2016
 */
public class EventVisiblePolicy extends EventVersion {

    public enum InvalidateNodes {
        LIST, // pouze uzly v seznamu
        ALL // všechny uzly
    }

    private InvalidateNodes invalidateNodes;

    private Integer[] nodeIds;

    public EventVisiblePolicy(final EventType eventType,
                              final Integer versionId,
                              final InvalidateNodes invalidateNodes,
                              final Integer... nodeIds) {
        super(eventType, versionId);
        this.invalidateNodes = invalidateNodes;
        this.nodeIds = nodeIds;
    }

    public InvalidateNodes getInvalidateNodes() {
        return invalidateNodes;
    }

    public void setInvalidateNodes(final InvalidateNodes invalidateNodes) {
        this.invalidateNodes = invalidateNodes;
    }

    public Integer[] getNodeIds() {
        return nodeIds;
    }

    public void setNodeIds(final Integer[] nodeIds) {
        this.nodeIds = nodeIds;
    }
}
