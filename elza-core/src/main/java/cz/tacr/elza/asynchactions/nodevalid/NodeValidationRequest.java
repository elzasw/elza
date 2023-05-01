package cz.tacr.elza.asynchactions.nodevalid;

import cz.tacr.elza.asynchactions.AsyncRequestBase;
import cz.tacr.elza.domain.ArrAsyncRequest;

public class NodeValidationRequest extends AsyncRequestBase {

    final Integer nodeId;
    private boolean failed = false;

    public NodeValidationRequest(ArrAsyncRequest request) {
        super(request.getAsyncRequestId(),
                request.getPriority(),
                request.getFundVersionId(),
                request.getType());

        this.nodeId = request.getNodeId();
    }

    Integer getNodeId() {
        return nodeId;
    }


    public void setFailed(boolean failed) {
        this.failed = failed;
    }

    public boolean isFailed() {
        return failed;
    }

    @Override
    public Integer getCurrentId() {
        return nodeId;
    }

    @Override
    public String toString() {
        return getType() + "(" + getCurrentId() + ") p=" + priority + ", fv=" + fundVersionId + ", reqId=" + requestId;
    }
}
