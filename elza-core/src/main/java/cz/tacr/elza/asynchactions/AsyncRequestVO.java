package cz.tacr.elza.asynchactions;

import cz.tacr.elza.domain.ArrAsyncRequest;
import cz.tacr.elza.domain.AsyncTypeEnum;

public class AsyncRequestVO {

    Long requestId;
    Integer fundVersionId;
    Integer nodeId;
    Integer bulkActionId;
    Integer outputId;
    Integer priority;
    AsyncTypeEnum type;

    public AsyncRequestVO() {
    }

    public AsyncRequestVO(ArrAsyncRequest request) {
        this.requestId = request.getAsyncRequestId();
        this.fundVersionId = request.getFundVersion().getFundVersionId();
        this.type = request.getType();
        this.priority = request.getPriority();
        switch (type) {
            case NODE:
                this.nodeId = request.getNode().getNodeId();
                break;
            case BULK:
                this.bulkActionId = request.getBulkAction().getBulkActionRunId();
                break;
            case OUTPUT:
                this.outputId = request.getOutput().getOutputId();
                break;
        }
    }

    public Long getRequestId() {
        return requestId;
    }

    public void setRequestId(Long requestId) {
        this.requestId = requestId;
    }

    public Integer getFundVersionId() {
        return fundVersionId;
    }

    public void setFundVersionId(Integer fundVersionId) {
        this.fundVersionId = fundVersionId;
    }

    public Integer getNodeId() {
        return nodeId;
    }

    public void setNodeId(Integer nodeId) {
        this.nodeId = nodeId;
    }

    public Integer getBulkActionId() {
        return bulkActionId;
    }

    public void setBulkActionId(Integer bulkActionId) {
        this.bulkActionId = bulkActionId;
    }

    public Integer getOutputId() {
        return outputId;
    }

    public void setOutputId(Integer outputId) {
        this.outputId = outputId;
    }

    public AsyncTypeEnum getType() {
        return type;
    }

    public void setType(AsyncTypeEnum type) {
        this.type = type;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }
}
