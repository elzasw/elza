package cz.tacr.elza.dataexchange.output.sections;

import java.util.List;

import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.ArrNodeRegister;

public class ExportLevelInfo {

    private final int nodeId;

    private final Integer parentNodeId;

    private String nodeUuid;

    private List<ArrDescItem> descItems;

    private List<ArrNodeRegister> nodeAPs;

    ExportLevelInfo(int nodeId, Integer parentNodeId) {
        this.nodeId = nodeId;
        this.parentNodeId = parentNodeId;
    }

    public int getNodeId() {
        return nodeId;
    }

    public Integer getParentNodeId() {
        return parentNodeId;
    }

    public String getNodeUuid() {
        return nodeUuid;
    }

    void setNodeUuid(String nodeUuid) {
        this.nodeUuid = nodeUuid;
    }

    public List<ArrDescItem> getDescItems() {
        return descItems;
    }

    void setDescItems(List<ArrDescItem> descItems) {
        this.descItems = descItems;
    }

    public List<ArrNodeRegister> getNodeAPs() {
        return nodeAPs;
    }

    void setNodeAPs(List<ArrNodeRegister> nodeAPs) {
        this.nodeAPs = nodeAPs;
    }
}
