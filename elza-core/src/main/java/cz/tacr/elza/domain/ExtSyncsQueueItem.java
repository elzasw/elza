package cz.tacr.elza.domain;

import cz.tacr.elza.domain.enumeration.StringLength;

import jakarta.persistence.*;
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
    @JoinColumn(name = "accessPointId", nullable = true)
    private ApAccessPoint accessPoint;

    @Column(nullable = true, updatable = false, insertable = false)
    private Integer accessPointId;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ApBinding.class)
    @JoinColumn(name = "bindingId", nullable = false)
    private ApBinding binding;

    @Column(nullable = true, updatable = false, insertable = false)
    private Integer bindingId;

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

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = UsrUser.class, optional = false)
    @JoinColumn(name = "userId", nullable = true)
    private UsrUser user;

    @Column(nullable = true, updatable = false, insertable = false)
    private Integer userId;

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

    public ApBinding getBinding() {
        return binding;
    }

    public void setBinding(ApBinding binding) {
        this.binding = binding;
        this.bindingId = binding != null ? binding.getBindingId() : null;
    }

    public Integer getBindingId() {
        return bindingId;
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

    public UsrUser getUser() {
        return user;
    }

    public void setUser(UsrUser user) {
        this.user = user;
        this.userId = user != null ? user.getUserId() : null;
    }

    public Integer getUserId() {
        return userId;
    }

    public enum ExtAsyncQueueState {

        UPDATE("K aktualizaci"),

        IMPORT_NEW("Ke stažení"),

        IMPORT_OK("Aktualizováno/Staženo"), // předchozí OK

        EXPORT_NEW("K odeslání"),

        EXPORT_START("Začít odesílání"),

        EXPORT_OK("Odesláno"),

        EXPORT_CANCEL("Odesílání přerušeno"),

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
