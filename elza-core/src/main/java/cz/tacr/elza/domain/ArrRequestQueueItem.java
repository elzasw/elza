package cz.tacr.elza.domain;

import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import cz.tacr.elza.domain.enumeration.StringLength;

/**
 * Položka ve frontě pro odeslání do externích systémů.
 *
 * @author Martin Šlapa
 * @since 07.12.2016
 */
@Entity(name = "arr_request_queue_item")
@Table
public class ArrRequestQueueItem {

    @Id
    @GeneratedValue
    private Integer requestQueueItemId;

    @ManyToOne(fetch = FetchType.EAGER, targetEntity = ArrRequest.class)
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

    public Integer getRequestQueueItemId() {
        return requestQueueItemId;
    }

    public void setRequestQueueItemId(final Integer requestQueueItemId) {
        this.requestQueueItemId = requestQueueItemId;
    }

    public ArrRequest getRequest() {
        return request;
    }

    public void setRequest(final ArrRequest request) {
        this.request = request;
    }

    public ArrChange getCreateChange() {
        return createChange;
    }

    public void setCreateChange(final ArrChange createChange) {
        this.createChange= createChange;
    }

    public LocalDateTime getAttemptToSend() {
        return attemptToSend;
    }

    public void setAttemptToSend(final LocalDateTime attemptToSend) {
        this.attemptToSend = attemptToSend;
    }

    public String getError() {
        return error;
    }

    public void setError(final String error) {
        this.error = error;
    }

    public Boolean getSend() {
        return send;
    }

    public void setSend(final Boolean send) {
        this.send = send;
    }
}
