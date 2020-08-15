package cz.tacr.elza.domain;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

import cz.tacr.elza.domain.enumeration.StringLength;

@Entity(name = "ap_binding")
public class ApBinding {

    @Id
    @GeneratedValue
    @Access(AccessType.PROPERTY)
    private Integer bindingId;

    @Column(length = StringLength.LENGTH_50, nullable = false)
    private String value;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ApExternalSystem.class)
    @JoinColumn(name = "externalSystemId", nullable = false)
    private ApExternalSystem apExternalSystem;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ApScope.class)
    @JoinColumn(name = "scopeId", nullable = false)
    private ApScope scope;

    public Integer getBindingId() {
        return bindingId;
    }

    public void setBindingId(Integer bindingId) {
        this.bindingId = bindingId;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public ApExternalSystem getApExternalSystem() {
        return apExternalSystem;
    }

    public void setApExternalSystem(ApExternalSystem apExternalSystem) {
        this.apExternalSystem = apExternalSystem;
    }

    public ApScope getScope() {
        return scope;
    }

    public void setScope(ApScope scope) {
        this.scope = scope;
    }

//    public ApAccessPoint getAccessPoint() {
//        return accessPoint;
//    }
//
//    public void setAccessPoint(ApAccessPoint accessPoint) {
//        this.accessPoint = accessPoint;
//        this.accessPointId = accessPoint != null ? accessPoint.getAccessPointId() : null;
//    }
//
//    public Integer getAccessPointId() {
//        return accessPointId;
//    }
//
//    public ApExternalIdType getExternalIdType() {
//        return externalIdType;
//    }
//
//    public void setExternalIdType(ApExternalIdType externalIdType) {
//        this.externalIdType = externalIdType;
//        this.externalIdTypeId = externalIdType != null ? externalIdType.getExternalIdTypeId() : null;
//    }
//
//    public Integer getExternalIdTypeId() {
//        return externalIdTypeId;
//    }
//
//    public void setCreateChange(ApChange createChange) {
//        this.createChange = createChange;
//    }
}
