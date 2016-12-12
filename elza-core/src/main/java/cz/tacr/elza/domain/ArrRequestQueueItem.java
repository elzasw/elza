package cz.tacr.elza.domain;

import cz.tacr.elza.domain.enumeration.StringLength;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import java.time.LocalDateTime;

/**
 * Implementace {@link cz.tacr.elza.api.ArrRequestQueueItem}
 *
 * @author Martin Šlapa
 * @since 07.12.2016
 */
@Entity(name = "arr_request_queue_item")
@Table
public class ArrRequestQueueItem implements cz.tacr.elza.api.ArrRequestQueueItem<ArrRequest, ArrChange> {

    @Id
    @GeneratedValue
    private Integer requestQueueItemId;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ArrRequest.class)
    @JoinColumn(name = "requestId", nullable = false)
    private ArrRequest request;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ArrChange.class)
    @JoinColumn(name = "createChangeId", nullable = false)
    private ArrChange createChange;

    @Column
    private LocalDateTime attemptToSend;

    @Column(length = StringLength.LENGTH_1000)
    private String error;

    @Column(nullable = false)
    private Boolean send;

    @Override
    public Integer getRequestQueueItemId() {
        return requestQueueItemId;
    }

    @Override
    public void setRequestQueueItemId(final Integer requestQueueItemId) {
        this.requestQueueItemId = requestQueueItemId;
    }

    @Override
    public ArrRequest getRequest() {
        return request;
    }

    @Override
    public void setRequest(final ArrRequest request) {
        this.request = request;
    }

    @Override
    public ArrChange getCreateChange() {
        return createChange;
    }

    @Override
    public void setCreateChange(final ArrChange createChange) {
        this.createChange= createChange;
    }

    @Override
    public LocalDateTime getAttemptToSend() {
        return attemptToSend;
    }

    @Override
    public void setAttemptToSend(final LocalDateTime attemptToSend) {
        this.attemptToSend = attemptToSend;
    }

    @Override
    public String getError() {
        return error;
    }

    @Override
    public void setError(final String error) {
        this.error = error;
    }

    @Override
    public Boolean getSend() {
        return send;
    }

    @Override
    public void setSend(final Boolean send) {
        this.send = send;
    }
}
