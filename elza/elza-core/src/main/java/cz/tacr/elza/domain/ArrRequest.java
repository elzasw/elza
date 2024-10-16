package cz.tacr.elza.domain;

import java.time.LocalDateTime;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import cz.tacr.elza.domain.enumeration.StringLength;

/**
 * Dotaz pro externí systémy.
 *
 * @author Martin Šlapa
 * @since 07.12.2016
 */
@Entity(name = "arr_request")
@Inheritance(strategy = InheritanceType.JOINED)
@Table
@DiscriminatorColumn(
        name="discriminator",
        discriminatorType= DiscriminatorType.STRING
)
public abstract class ArrRequest {

    public enum ClassType {
        DAO_LINK(Values.DAO_LINK), DAO(Values.DAO), DIGITIZATION(Values.DIGITIZATION);

        private String value;

        ClassType(final String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        public static class Values {
            public static final String DAO_LINK= "DAO_LINK";
            public static final String DAO = "DAO";
            public static final String DIGITIZATION = "DIGITIZATION";
        }
    }

    public enum State {

        /**
         * V přípravě.
         */
        OPEN,

        /**
         * Ve frontě.
         */
        QUEUED,

        /**
         * Odeslán.
         */
        SENT,

        /**
         * Processed in other system.
         */
        PROCESSED,

        /**
         * Request was rejected / failed.
         */
        REJECTED

    }

    public static final String TABLE_NAME = "arr_request";
    public static final String FIELD_REQUEST_ID = "requestId";
    public static final String FIELD_FUND = "fund";
    public static final String FIELD_CREATE_CHANGE_ID = "createChangeId";

    @Id
    @GeneratedValue
    @Access(AccessType.PROPERTY) // required to read id without fetch from db
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

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ArrChange.class)
    @JoinColumn(name = FIELD_CREATE_CHANGE_ID, nullable = false)
    private ArrChange createChange;

    @Column
    private LocalDateTime responseExternalSystem;

    @Column(length = StringLength.LENGTH_20, nullable = false, insertable = false, updatable = false)
    @Enumerated(EnumType.STRING)
    private ClassType discriminator;

    @Column(length = StringLength.LENGTH_1000)
    private String externalSystemCode;

    public Integer getRequestId() {
        return requestId;
    }

    public void setRequestId(final Integer requestId) {
        this.requestId = requestId;
    }

    public ArrFund getFund() {
        return fund;
    }

    public void setFund(final ArrFund fund) {
        this.fund = fund;
    }

    public String getCode() {
        return code;
    }

    public void setCode(final String code) {
        this.code = code;
    }

    public State getState() {
        return state;
    }

    public void setState(final State state) {
        this.state = state;
    }

    public LocalDateTime getResponseExternalSystem() {
        return responseExternalSystem;
    }

    public void setResponseExternalSystem(final LocalDateTime responseExternalSystem) {
        this.responseExternalSystem = responseExternalSystem;
    }

    public String getRejectReason() {
        return rejectReason;
    }

    public void setRejectReason(final String rejectReason) {
        this.rejectReason = rejectReason;
    }

    public ArrChange getCreateChange() {
        return createChange;
    }

    public void setCreateChange(final ArrChange createChange) {
        this.createChange = createChange;
    }

    public ClassType getDiscriminator() {
        return discriminator;
    }

    public String getExternalSystemCode() {
        return externalSystemCode;
    }

    public void setExternalSystemCode(final String externalSystemCode) {
        this.externalSystemCode = externalSystemCode;
    }
}
