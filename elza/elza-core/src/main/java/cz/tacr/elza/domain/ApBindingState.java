package cz.tacr.elza.domain;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import cz.tacr.elza.domain.enumeration.StringLength;
import cz.tacr.elza.service.cache.AccessPointCacheSerializable;

@Entity(name = "ap_binding_state")
@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
public class ApBindingState implements AccessPointCacheSerializable {

    public static final String ACCESS_POINT_ID = "accessPointId";
    public static final String DELETE_CHANGE_ID = "deleteChangeId";

    @Id
    @GeneratedValue
    @Access(AccessType.PROPERTY)
    private Integer bindingStateId;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ApBinding.class)
    @JoinColumn(name = "bindingId", nullable = false)
    private ApBinding binding;

    @Column(updatable = false, insertable = false)
    private Integer bindingId;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ApAccessPoint.class)
    @JoinColumn(name = "accessPointId")
    private ApAccessPoint accessPoint;

    @Column(updatable = false, insertable = false)
    private Integer accessPointId;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ApExternalSystem.class)
    @JoinColumn(name = "externalSystemId", nullable = false)
    private ApExternalSystem apExternalSystem;

    @Column(nullable = false, updatable = false, insertable = false)
    private Integer externalSystemId;

    @Column(length = StringLength.LENGTH_50)
    private String extState;

    @Column(length = StringLength.LENGTH_50)
    private String extRevision;

    @Column(length = StringLength.LENGTH_50)
    private String extUser;

    @Column(length = StringLength.LENGTH_50)
    private String extReplacedBy;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ApChange.class)
    @JoinColumn(name = "syncChangeId")
    private ApChange syncChange;

    @Column(nullable = false, updatable = false, insertable = false)
    private Integer syncChangeId;

    @Enumerated(EnumType.STRING)
    @Column(length = StringLength.LENGTH_ENUM)
    private SyncState syncOk;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ApChange.class)
    @JoinColumn(name = "createChangeId", nullable = false)
    private ApChange createChange;

    @Column(nullable = false, updatable = false, insertable = false)
    private Integer createChangeId;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ApChange.class)
    @JoinColumn(name = "deleteChangeId")
    private ApChange deleteChange;

    @Column(nullable = true, updatable = false, insertable = false)
    private Integer deleteChangeId;

    public Integer getBindingStateId() {
        return bindingStateId;
    }

    public void setBindingStateId(Integer bindingStateId) {
        this.bindingStateId = bindingStateId;
    }

    public ApBinding getBinding() {
        return binding;
    }

    public void setBinding(ApBinding binding) {
        this.binding = binding;
    }

    public Integer getBindingId() {
        if (bindingId != null) {
            return bindingId;
        } else if (binding != null) {
            return binding.getBindingId();
        } else {
            return null;
        }
    }

    public ApAccessPoint getAccessPoint() {
        return accessPoint;
    }

    public void setAccessPoint(ApAccessPoint accessPoint) {
        this.accessPoint = accessPoint;
        this.accessPointId = accessPoint != null ? accessPoint.getAccessPointId() : null;
    }

    public Integer getAccessPointId() {
        if (accessPointId != null) {
            return accessPointId;
        } else if (accessPoint != null) {
            return accessPoint.getAccessPointId();
        } else {
            return null;
        }
    }

    public ApExternalSystem getApExternalSystem() {
        return apExternalSystem;
    }

    public void setApExternalSystem(ApExternalSystem apExternalSystem) {
        this.apExternalSystem = apExternalSystem;
        this.externalSystemId = apExternalSystem != null ? apExternalSystem.getExternalSystemId() : null;
    }

    public Integer getExternalSystemId() {
        return externalSystemId;
    }

    public String getExtState() {
        return extState;
    }

    public void setExtState(String extState) {
        this.extState = extState;
    }

    public String getExtRevision() {
        return extRevision;
    }

    public void setExtRevision(String extRevision) {
        this.extRevision = extRevision;
    }

    public String getExtUser() {
        return extUser;
    }

    public void setExtUser(String extUser) {
        this.extUser = extUser;
    }

    public String getExtReplacedBy() {
        return extReplacedBy;
    }

    public void setExtReplacedBy(String extReplacedBy) {
        this.extReplacedBy = extReplacedBy;
    }

    public ApChange getSyncChange() {
        return syncChange;
    }

    public void setSyncChange(ApChange syncChange) {
        this.syncChange = syncChange;
        if (syncChange == null) {
            this.syncChangeId = null;
        } else {
            this.syncChangeId = syncChange.getChangeId();
        }
    }

    public Integer getSyncChangeId() {
        return syncChangeId;
    }

    public SyncState getSyncOk() {
        return syncOk;
    }

    public void setSyncOk(SyncState syncOk) {
        this.syncOk = syncOk;
    }

    public ApChange getCreateChange() {
        return createChange;
    }

    public void setCreateChange(ApChange createChange) {
        this.createChange = createChange;
        if (createChange == null) {
            this.createChangeId = null;
        } else {
            this.createChangeId = createChange.getChangeId();
        }
    }

    public Integer getCreateChangeId() {
        return createChangeId;
    }

    public ApChange getDeleteChange() {
        return deleteChange;
    }

    public void setDeleteChange(ApChange deleteChange) {
        this.deleteChange = deleteChange;
        if (deleteChange == null) {
            this.deleteChangeId = null;
        } else {
            this.deleteChangeId = deleteChange.getChangeId();
        }
    }

    public Integer getDeleteChangeId() {
        return deleteChangeId;
    }
}
