package cz.tacr.elza.asynchactions.nodevalid;

import cz.tacr.elza.asynchactions.AsyncRequestBase;
import cz.tacr.elza.domain.ArrAsyncRequest;

public class NodeValidationRequest extends AsyncRequestBase {

    final Integer nodeId;
    private boolean failed = false;

    public NodeValidationRequest(ArrAsyncRequest request) {
        super(request.getAsyncRequestId(),
                request.getPriority(),
                request.getFundVersion() != null ? request.getFundVersion().getFundVersionId() : null,
                request.getType());

        this.nodeId = request.getNode().getNodeId();
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

}
