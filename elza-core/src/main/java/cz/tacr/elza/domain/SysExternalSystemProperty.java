package cz.tacr.elza.domain;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import cz.tacr.elza.domain.enumeration.StringLength;

@Entity(name = "sys_external_system_property")
@Cache(region = "domain", usage = CacheConcurrencyStrategy.READ_WRITE)
@Table
public class SysExternalSystemProperty {

    @Id
    @GeneratedValue
    @Access(AccessType.PROPERTY) // required to read id without fetch from db
    private Integer externalSystemPropertyId;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = SysExternalSystem.class)
    @JoinColumn(name = SysExternalSystem.PK, nullable = false)
    private SysExternalSystem externalSystem;

    @Column(name = SysExternalSystem.PK, updatable = false, insertable = false)
    private Integer externalSystemId;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = UsrUser.class)
    @JoinColumn(name = "userId", nullable = true)
    private UsrUser user;

    @Column(nullable = true, updatable = false, insertable = false)
    private Integer userId;

    @Column(length = StringLength.LENGTH_250, nullable = false)
    private String name;

    @Column(length = StringLength.LENGTH_1000)
    private String value;

	public Integer getExternalSystemPropertyId() {
		return externalSystemPropertyId;
	}

	public void setExternalSystemPropertyId(Integer externalSystemPropertyId) {
		this.externalSystemPropertyId = externalSystemPropertyId;
	}

	public SysExternalSystem getExternalSystem() {
		return externalSystem;
	}

	public void setExternalSystem(SysExternalSystem externalSystem) {
		if (externalSystem != null) {
			this.externalSystemId = externalSystem.getExternalSystemId();
		}
		this.externalSystem = externalSystem;
	}

	public Integer getExternalSystemId() {
		return externalSystemId;
	}

	public UsrUser getUser() {
		return user;
	}

	public void setUser(UsrUser user) {
		if (user != null) {
			this.userId = user.getUserId();
		}
		this.user = user;
	}

	public Integer getUserId() {
		return userId;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}
}
