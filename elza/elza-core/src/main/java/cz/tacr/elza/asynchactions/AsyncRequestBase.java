package cz.tacr.elza.asynchactions;

import cz.tacr.elza.domain.AsyncTypeEnum;

abstract public class AsyncRequestBase implements IAsyncRequest {

    protected final Long requestId;

    protected final Integer fundVersionId;

    protected final int priority;

    protected final AsyncTypeEnum type;

    protected AsyncRequestBase(final Long requestId,
                               final int priority,
                               final Integer fundVersionId,
                               final AsyncTypeEnum type) {
        this.requestId = requestId;
        this.priority = priority;
        this.fundVersionId = fundVersionId;
        this.type = type;
    }

    @Override
    public Long getRequestId() {
        return requestId;
    }

    @Override
    public Integer getFundVersionId() {
        return fundVersionId;
    }

    @Override
    public AsyncTypeEnum getType() {
        return type;
    }

    @Override
    public int getPriority() {
        return priority;
    }
}
