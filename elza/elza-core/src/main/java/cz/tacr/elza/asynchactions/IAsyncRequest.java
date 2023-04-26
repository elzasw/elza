package cz.tacr.elza.asynchactions;

import cz.tacr.elza.domain.AsyncTypeEnum;

public interface IAsyncRequest {

    Long getRequestId();

    Integer getFundVersionId();

    Integer getCurrentId();

    AsyncTypeEnum getType();

    int getPriority();
}
