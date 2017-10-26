package cz.tacr.elza.controller.vo.nodes;

import cz.tacr.elza.domain.ArrNode;

/**
 * Node id and version
 *
 * Object is used to transfer nodeId and its version
 */
public class ArrNodeVO {

    /**
     * identifikátor uzlu
     */
    private Integer id;

    /**
     * verze uzlu
     */
    private Integer version;

    public ArrNodeVO() {
    }

	protected ArrNodeVO(ArrNode node) {
		this.id = node.getNodeId();
		this.version = node.getVersion();
	}

	public Integer getId() {
        return id;
    }

    public void setId(final Integer id) {
        this.id = id;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(final Integer version) {
        this.version = version;
    }

    public ArrNode createEntity() {
        ArrNode node = new ArrNode();
        node.setNodeId(id);
        node.setVersion(version);
        return node;
    }

    public static ArrNodeVO valueOf(ArrNode node) {
        return new ArrNodeVO(node);
    }
}
