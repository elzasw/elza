package cz.tacr.elza.service.eventnotification.events.vo;

/**
 * Data uzlu pro události a Websockety.
 *
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 20.01.2016
 */
public class NodeInfo {

    /**
     * Id uzlu.
     */
    private Integer nodeId;
    /**
     * Verze uzlu.
     */
    private Integer version;

    public NodeInfo(final Integer nodeId, final Integer version) {
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
        return "{" +
                "nodeId=" + nodeId +
                ", version=" + version +
                '}';
    }
}
