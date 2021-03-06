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
import cz.tacr.elza.service.cache.AccessPointCacheSerializable;

@Entity(name = "ap_binding")
public class ApBinding implements AccessPointCacheSerializable {

    @Id
    @GeneratedValue
    @Access(AccessType.PROPERTY)
    private Integer bindingId;

    @Column(length = StringLength.LENGTH_50, nullable = false)
    private String value;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ApExternalSystem.class)
    @JoinColumn(name = "externalSystemId", nullable = false)
    private ApExternalSystem apExternalSystem;

    @Column(nullable = false, updatable = false, insertable = false)
    private Integer externalSystemId;

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
        this.externalSystemId = apExternalSystem != null ? apExternalSystem.getExternalSystemId() : null;
    }

    public Integer getExternalSystemId() {
        return externalSystemId;
    }

    public ApScope getScope() {
        return scope;
    }

    public void setScope(ApScope scope) {
        this.scope = scope;
    }
}
