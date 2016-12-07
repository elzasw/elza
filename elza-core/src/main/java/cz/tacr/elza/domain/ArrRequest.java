package cz.tacr.elza.domain;

import cz.tacr.elza.domain.enumeration.StringLength;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import java.time.LocalDateTime;

/**
 * Implementace {@link cz.tacr.elza.api.ArrRequest}
 *
 * @author Martin Šlapa
 * @since 07.12.2016
 */
@Entity(name = "arr_request")
@Inheritance(strategy = InheritanceType.JOINED)
@Table
public abstract class ArrRequest implements cz.tacr.elza.api.ArrRequest<ArrFund> {

    @Id
    @GeneratedValue
    private Integer requestId;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ArrFund.class)
    @JoinColumn(name = "fundId", nullable = false)
    private ArrFund fund;

    @Column(length = StringLength.LENGTH_50, nullable = false, unique = true)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private State state;

    @Column(length = StringLength.LENGTH_1000)
    private String rejectReason;

    @Column
    private LocalDateTime responseExternalSystem;

    @Override
    public Integer getRequestId() {
        return requestId;
    }

    @Override
    public void setRequestId(final Integer requestId) {
        this.requestId = requestId;
    }

    @Override
    public ArrFund getFund() {
        return fund;
    }

    @Override
    public void setFund(final ArrFund fund) {
        this.fund = fund;
    }

    @Override
    public String getCode() {
        return code;
    }

    @Override
    public void setCode(final String code) {
        this.code = code;
    }

    @Override
    public State getState() {
        return state;
    }

    @Override
    public void setState(final State state) {
        this.state = state;
    }

    @Override
    public LocalDateTime getResponseExternalSystem() {
        return responseExternalSystem;
    }

    @Override
    public void setResponseExternalSystem(final LocalDateTime responseExternalSystem) {
        this.responseExternalSystem = responseExternalSystem;
    }

    @Override
    public String getRejectReason() {
        return rejectReason;
    }

    @Override
    public void setRejectReason(final String rejectReason) {
        this.rejectReason = rejectReason;
    }
}
