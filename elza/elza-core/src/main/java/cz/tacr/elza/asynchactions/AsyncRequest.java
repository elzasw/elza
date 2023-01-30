package cz.tacr.elza.asynchactions;

import org.apache.commons.lang3.NotImplementedException;

import cz.tacr.elza.domain.ArrAsyncRequest;
import cz.tacr.elza.domain.AsyncTypeEnum;

public class AsyncRequest {

    private final Long requestId;
    private final Integer fundVersionId;
    private final Integer priority;
    private final AsyncTypeEnum type;
    private Integer nodeId;
    private Integer bulkActionId;
    private Integer outputId;
    private Integer accessPointId;
    private Integer userId;
    private boolean failed = false;

    public AsyncRequest(ArrAsyncRequest request) {
        this.requestId = request.getAsyncRequestId();
        this.fundVersionId = request.getFundVersion() != null ? request.getFundVersion().getFundVersionId() : null;
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
                this.userId = request.getUserId();
                break;
            case AP:
                this.accessPointId = request.getAccessPoint().getAccessPointId();
                break;
            default:
                throw new NotImplementedException("Neimplmentovaný typ: " + type);
        }
    }

    public Integer getCurrentId() {
        switch (type) {
            case OUTPUT:
                return outputId;
            case BULK:
                return bulkActionId;
            case NODE:
                return nodeId;
            case AP:
                return accessPointId;
            default:
                throw new NotImplementedException("Neimplmentovaný typ: " + type);
        }
    }

    public Long getRequestId() {
        return requestId;
    }

    public Integer getFundVersionId() {
        return fundVersionId;
    }

    public Integer getNodeId() {
        return nodeId;
    }

    public Integer getBulkActionId() {
        return bulkActionId;
    }

    public Integer getOutputId() {
        return outputId;
    }

    public Integer getPriority() {
        return priority;
    }

    public AsyncTypeEnum getType() {
        return type;
    }

    public Integer getUserId() {
        return userId;
    }

    public Integer getAccessPointId() {
        return accessPointId;
    }

    public void setFailed(boolean failed) {
        this.failed = failed;
    }

    public boolean isFailed() {
        return failed;
    }

    @Override
    public String toString() {
        return getType() + "(" + getCurrentId() + ") p=" + priority + ", fv=" + fundVersionId + ", reqId=" + requestId;
    }
}
