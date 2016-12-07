package cz.tacr.elza.api;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Položka ve frontě pro odeslání do externích systémů.
 *
 * @author Martin Šlapa
 * @since 07.12.2016
 */
public interface ArrRequestQueueItem<R extends ArrRequest> extends Serializable {

    Integer getRequestQueueItemId();

    void setRequestQueueItemId(Integer requestQueueItemId);

    R getRequest();

    void setRequest(R request);

    LocalDateTime getCreate();

    void setCreate(LocalDateTime create);

    LocalDateTime getAttemptToSend();

    void setAttemptToSend(LocalDateTime attemptToSend);

    String getError();

    void setError(String error);

    Boolean getSend();

    void setSend(Boolean send);

}
