package cz.tacr.elza.dataexchange.output.sections;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import cz.tacr.elza.domain.ArrItem;
import cz.tacr.elza.domain.ArrNodeRegister;

public class ExportLevelInfo {

    private final List<ArrItem> items = new ArrayList<>();

    private final int nodeId;

    private final Integer parentNodeId;

    private String nodeUuid;

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

    public List<ArrItem> getItems() {
        return Collections.unmodifiableList(items);
    }

    public void addItem(ArrItem item) {
        items.add(item);
    }

    public List<ArrNodeRegister> getNodeAPs() {
        return nodeAPs;
    }

    void setNodeAPs(List<ArrNodeRegister> nodeAPs) {
        this.nodeAPs = nodeAPs;
    }
}
