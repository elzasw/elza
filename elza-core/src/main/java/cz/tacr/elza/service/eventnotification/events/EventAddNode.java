package cz.tacr.elza.service.eventnotification.events;

import cz.tacr.elza.service.eventnotification.events.vo.NodeInfo;


/**
 * Událost přidání nového uzlu.
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 25.01.2016
 */
public class EventAddNode extends EventVersion {

    /**
     * Uzel, ke kterému přidáváme nový (před, za, pod)
     */
    private NodeInfo staticNode;
    /**
     * Rodič statického uzlu.
     */
    private NodeInfo staticNodeParent;

    /**
     * Nově přidaný uzel.
     */
    private NodeInfo node;


    public EventAddNode(final EventType eventType,
                        final Integer versionId,
                        final NodeInfo staticNode,
                        final NodeInfo staticNodeParent,
                        final NodeInfo node) {
        super(eventType, versionId);
        this.staticNode = staticNode;
        this.staticNodeParent = staticNodeParent;
        this.node = node;
    }

    public EventAddNode(final EventType eventType, final Integer versionId) {
        super(eventType, versionId);
    }

    public NodeInfo getStaticNode() {
        return staticNode;
    }

    public void setStaticNode(final NodeInfo staticNode) {
        this.staticNode = staticNode;
    }

    public NodeInfo getStaticNodeParent() {
        return staticNodeParent;
    }

    public void setStaticNodeParent(final NodeInfo staticNodeParent) {
        this.staticNodeParent = staticNodeParent;
    }

    public NodeInfo getNode() {
        return node;
    }

    public void setNode(final NodeInfo node) {
        this.node = node;
    }

    @Override
    public String toString() {
        return "EventAddNode{" +
                "staticNode=" + staticNode +
                ", staticNodeParent=" + staticNodeParent +
                ", node=" + node +
                '}';
    }
}

