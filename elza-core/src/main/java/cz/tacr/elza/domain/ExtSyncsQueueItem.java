package cz.tacr.elza.domain;

import cz.tacr.elza.domain.enumeration.StringLength;

import javax.persistence.*;
import java.time.OffsetDateTime;

@Entity(name = "ext_syncs_queue_item")
public class ExtSyncsQueueItem {

    public static final String ACCESS_POINT = "accessPoint";
    public static final String EXTERNAL_SYSTEM = "externalSystem";
    public static final String DATE = "date";
    public static final String STATE = "state";

    @Id
    @GeneratedValue
    @Access(AccessType.PROPERTY)
    private Integer extSyncsQueueItemId;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ApAccessPoint.class)
    @JoinColumn(name = "accessPointId", nullable = false)
    private ApAccessPoint accessPoint;

    @Column(nullable = false, updatable = false, insertable = false)
    private Integer accessPointId;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ApExternalSystem.class)
    @JoinColumn(name = "externalSystemId", nullable = false)
    private ApExternalSystem externalSystem;

    @Column(nullable = false, updatable = false, insertable = false)
    private Integer externalSystemId;

    @Column(length = StringLength.LENGTH_4000)
    private String stateMessage;

    @Enumerated(EnumType.STRING)
    @Column(name = "state", length = StringLength.LENGTH_ENUM, nullable = false)
    private ExtAsyncQueueState state;

    @Column(nullable = false)
    private OffsetDateTime date;

    @Column(length = StringLength.LENGTH_250, nullable = false)
    private String username;

    @Column(length = StringLength.LENGTH_50, nullable = true)
    private String batchId;

    public Integer getExtSyncsQueueItemId() {
        return extSyncsQueueItemId;
    }

    public void setExtSyncsQueueItemId(Integer extSyncsQueueItemId) {
        this.extSyncsQueueItemId = extSyncsQueueItemId;
    }

    public ApAccessPoint getAccessPoint() {
        return accessPoint;
    }

    public void setAccessPoint(ApAccessPoint accessPoint) {
        this.accessPoint = accessPoint;
        this.accessPointId = accessPoint != null ? accessPoint.getAccessPointId() : null;
    }

    public Integer getAccessPointId() {
        return accessPointId;
    }

    public ApExternalSystem getExternalSystem() {
        return externalSystem;
    }

    public void setExternalSystem(final ApExternalSystem apExternalSystem) {
        this.externalSystem = apExternalSystem;
        this.externalSystemId = apExternalSystem != null ? apExternalSystem.getExternalSystemId() : null;
    }

    public Integer getExternalSystemId() {
        return externalSystemId;
    }

    public String getStateMessage() {
        return stateMessage;
    }

    public void setStateMessage(String stateMessage) {
        this.stateMessage = stateMessage;
    }

    public String getBatchId() {
        return batchId;
    }

    public void setBatchId(String batchId) {
        this.batchId = batchId;
    }

    public ExtAsyncQueueState getState() {
        return state;
    }

    public void setState(ExtAsyncQueueState state) {
        this.state = state;
    }

    public OffsetDateTime getDate() {
        return date;
    }

    public void setDate(OffsetDateTime date) {
        this.date = date;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public enum ExtAsyncQueueState {

        NEW("Nový"),

        UPDATE("Aktualizace"),

        OK("Odesláno"),

        ERROR("Chyba");

        private String value;

        ExtAsyncQueueState(String value) {
            this.value = value;
        }

        public String value() {
            return value;
        }

        public static ExtAsyncQueueState fromValue(String v) {
            return valueOf(v);
        }


    }
}
