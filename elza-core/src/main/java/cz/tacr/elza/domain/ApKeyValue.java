package cz.tacr.elza.domain;

import cz.tacr.elza.domain.enumeration.StringLength;
import cz.tacr.elza.service.cache.AccessPointCacheSerializable;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Entity(name = "ap_key_value")
@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
public class ApKeyValue implements AccessPointCacheSerializable {

    @Id
    @GeneratedValue
    @Access(AccessType.PROPERTY)
    private Integer keyValueId;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ApScope.class)
    @JoinColumn(name = "scope_id", nullable = false)
    private ApScope scope;

    @Column(length = StringLength.LENGTH_50, nullable = false)
    private String keyType;

    @Column(name = "key_value", length = StringLength.LENGTH_4000, nullable = false)
    private String value;

    public Integer getKeyValueId() {
        return keyValueId;
    }

    public void setKeyValueId(Integer keyValueId) {
        this.keyValueId = keyValueId;
    }

    public ApScope getScope() {
        return scope;
    }

    public void setScope(ApScope scope) {
        this.scope = scope;
    }

    public String getKeyType() {
        return keyType;
    }

    public void setKeyType(String keyType) {
        this.keyType = keyType;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
