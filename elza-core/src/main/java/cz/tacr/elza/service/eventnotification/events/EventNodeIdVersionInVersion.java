package cz.tacr.elza.service.eventnotification.events;

/**
 * Událost, která která nastala nad Node s ID nad konkrétní verzí stromu a je potřeba změnit verzi node
 *
 * @author Petr Compel [<a href="mailto:petr.compel@marbes.cz">petr.compel@marbes.cz</a>]
 * @since 12.02.2016
 */
public class EventNodeIdVersionInVersion extends EventVersion {

    /**
     * Node id
     */
    private Integer nodeId;
    /**
     * Nová verze node
     */
    private Integer version;

    public EventNodeIdVersionInVersion(final EventType eventType,
                                       final Integer versionId,
                                       final Integer nodeId,
                                       final Integer version) {
        super(eventType, versionId);
        this.nodeId = nodeId;

        this.version = version;
    }

    public Integer getNodeId() {
        return nodeId;
    }

    public void setNodeId(final Integer nodeId) {
        this.nodeId = nodeId;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(final Integer version) {
        this.version = version;
    }

    @Override
    public String toString() {
        return "EventNodeMove{" +
                "EventType=" + getEventType() +
                ", versionId=" + getVersionId() +
                ", nodeId=" + nodeId +
                ", version=" + version +
                '}';
    }
}
