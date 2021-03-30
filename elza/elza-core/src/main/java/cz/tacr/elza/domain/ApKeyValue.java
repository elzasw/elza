package cz.tacr.elza.domain;

import cz.tacr.elza.domain.enumeration.StringLength;
import cz.tacr.elza.service.cache.AccessPointCacheSerializable;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

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

    @Column(length = StringLength.LENGTH_4000, nullable = false)
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
