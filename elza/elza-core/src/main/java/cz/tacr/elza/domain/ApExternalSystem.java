package cz.tacr.elza.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import cz.tacr.elza.api.ApExternalSystemType;
import cz.tacr.elza.domain.enumeration.StringLength;

/**
 * Externí systémy pro rejstříky/osoby.
 */
@Entity(name = "ap_external_system")
@Cache(region = "domain", usage = CacheConcurrencyStrategy.READ_WRITE)
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
public class ApExternalSystem extends SysExternalSystem {

    @Enumerated(EnumType.STRING)
    @Column(length = StringLength.LENGTH_ENUM, nullable = false)
    private ApExternalSystemType type;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ApScope.class)
    @JoinColumn(name = "scopeId", nullable = true)
    private ApScope scope;

    @Column(updatable = false, insertable = false, nullable = true)
    private Integer scopeId;

    @Column(length = StringLength.LENGTH_50, nullable = true)
    private String userInfo;
    
    @Column(nullable = true)
    private Boolean publishOnlyApproved;

	public ApExternalSystemType getType() {
        return type;
    }

    public void setType(ApExternalSystemType type) {
        this.type = type;
    }

    public ApScope getScope() {
        return scope;
    }

    public void setScope(ApScope scope) {
        this.scope = scope;
    }

    public Integer getScopeId() {
        return scopeId;
    }

    public Boolean getPublishOnlyApproved() {
		return publishOnlyApproved;
	}

	public void setPublishOnlyApproved(Boolean publishOnlyApproved) {
		this.publishOnlyApproved = publishOnlyApproved;
	}

	public String getUserInfo() {
        return userInfo;
    }

    public void setUserInfo(String userInfo) {
        this.userInfo = userInfo;
    }

    @Override
    public String toString() {
        return "ApExternalSystem pk=" + getExternalSystemId();
    }
}
