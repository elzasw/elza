package cz.tacr.elza.domain.projection;

public class NodeIdFundVersionIdInfo {

    private final Integer nodeId;
    
    private final Integer fundVersionId;

    public NodeIdFundVersionIdInfo(Integer nodeId, Integer fundVersionId) {
        this.nodeId = nodeId;
        this.fundVersionId = fundVersionId;
    }

    public Integer getNodeId() {
        return nodeId;
    }

    public Integer getFundVersionId() {
        return fundVersionId;
    }
}