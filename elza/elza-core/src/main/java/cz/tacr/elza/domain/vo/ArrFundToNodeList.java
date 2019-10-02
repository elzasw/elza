package cz.tacr.elza.domain.vo;

import java.util.List;

public class ArrFundToNodeList {

    // --- fields ---

    private final Integer fundId;
    private final List<Integer> nodeIdList;

    // --- getters/setters ---

    public Integer getFundId() {
        return fundId;
    }

    public List<Integer> getNodeIdList() {
        return nodeIdList;
    }

    public int getNodeCount() {
        return nodeIdList.size();
    }

    public ArrFundToNodeList(Integer fundId, List<Integer> nodeIdList) {
        this.fundId = fundId;
        this.nodeIdList = nodeIdList;
    }
}
