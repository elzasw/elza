package cz.tacr.elza.asynchactions;

import cz.tacr.elza.domain.ArrAsyncRequest;
import cz.tacr.elza.domain.AsyncTypeEnum;
import org.apache.commons.lang3.NotImplementedException;

public class AsyncRequest {

    private final Long requestId;
    private final Integer fundVersionId;
    private final Integer priority;
    private final AsyncTypeEnum type;
    private Integer nodeId;
    private Integer bulkActionId;
    private Integer outputId;

    public AsyncRequest(ArrAsyncRequest request) {
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

    @Override
    public String toString() {
        AsyncTypeEnum type = getType();
        int id;
        switch (type) {
            case BULK:
                id = bulkActionId;
                break;
            case NODE:
                id = nodeId;
                break;
            case OUTPUT:
                id = outputId;
                break;
            default:
                throw new NotImplementedException(type.toString());
        }
        return type + "(" + id + ") p=" + priority + ", fv=" + fundVersionId + ", reqId=" + requestId;
    }
}
