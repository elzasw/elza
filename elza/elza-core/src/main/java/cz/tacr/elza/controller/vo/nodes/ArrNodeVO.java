package cz.tacr.elza.controller.vo.nodes;

import cz.tacr.elza.domain.ArrFund;
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

    /**
     * UUID uzlu
     */
    private String uuid;

    public ArrNodeVO() {
    }

	protected ArrNodeVO(ArrNode node) {
		this.id = node.getNodeId();
		this.version = node.getVersion();
		this.uuid = node.getUuid();
	}

    public static ArrNodeVO newInstance(final ArrNode node) {
        ArrNodeVO result = new ArrNodeVO();
        result.setId(node.getNodeId());
        result.setVersion(node.getVersion());
        result.setUuid(node.getUuid());
        return result;
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

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    /**
     * Create entity from this value object
     * 
     * @param fund
     *            Parent fund
     * 
     * @return
     */
    public ArrNode createEntity(ArrFund fund) {
        ArrNode node = new ArrNode();
        node.setNodeId(id);
        node.setVersion(version);
        node.setFund(fund);
        return node;
    }

    public ArrNode createEntity() {
        ArrNode node = new ArrNode();
        node.setNodeId(id);
        node.setVersion(version);
        node.setUuid(uuid);
        return node;
    }

    public static ArrNodeVO valueOf(ArrNode node) {
        return new ArrNodeVO(node);
    }
}
