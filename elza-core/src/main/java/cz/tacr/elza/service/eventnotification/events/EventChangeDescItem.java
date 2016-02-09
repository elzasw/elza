package cz.tacr.elza.service.eventnotification.events;

/**
 * Údálost volána při změně hodnoty atributu.
 *
 * @author Martin Šlapa
 * @since 2.2.2016
 */
public class EventChangeDescItem extends EventVersion<EventChangeDescItem> {

    /**
     * Object id hodnoty atributu.
     */
    private Integer descItemObjectId;

    /**
     * Idetifikator uzlu.
     */
    private Integer nodeId;

    public EventChangeDescItem(final EventType eventType, final Integer versionId, final Integer descItemObjectId,
                               final Integer nodeId) {
        super(eventType, versionId);
        this.descItemObjectId = descItemObjectId;
        this.nodeId = nodeId;
    }

    public Integer getDescItemObjectId() {
        return descItemObjectId;
    }

    public void setDescItemObjectId(final Integer descItemObjectId) {
        this.descItemObjectId = descItemObjectId;
    }

    public Integer getNodeId() {
        return nodeId;
    }

    public void setNodeId(final Integer nodeId) {
        this.nodeId = nodeId;
    }
}
