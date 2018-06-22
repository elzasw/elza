package cz.tacr.elza.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import cz.tacr.elza.domain.interfaces.Versionable;
import org.springframework.data.rest.core.annotation.RestResource;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import java.io.Serializable;

@Entity(name = "ap_external_id")
@Table
@Inheritance(strategy = InheritanceType.JOINED)
@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
public class ApExternalId implements Serializable {
	@Id
	@GeneratedValue
	private Integer externalId;

	@Column(length = 50, nullable = false)
	private String value;

	@ManyToOne(fetch = FetchType.LAZY, targetEntity = ApAccessPoint.class)
	@JoinColumn(name = "accessPointId", nullable = false)
	@JsonIgnore
	private ApAccessPoint accessPoint;

	@Column(nullable = false, updatable = false, insertable = false)
	private Integer accessPointId;

	@ManyToOne(fetch = FetchType.LAZY, targetEntity = ApExternalIdType.class)
	@JoinColumn(name = "externalIdTypeId", nullable = false)
	@JsonIgnore
	private ApExternalIdType externalIdType;

	@Column(nullable = false, updatable = false, insertable = false)
	private Integer externalIdTypeId;

	@RestResource(exported = false)
	@ManyToOne(fetch = FetchType.LAZY, targetEntity = ApChange.class)
	@JoinColumn(name = "createChangeId", nullable = false)
	protected ApChange createChange;

	@Column(name = "createChangeId", nullable = false, updatable = false, insertable = false)
	protected Integer createChangeId;

	@RestResource(exported = false)
	@ManyToOne(fetch = FetchType.LAZY, targetEntity = ApChange.class)
	@JoinColumn(name = "deleteChangeId", nullable = true)
	protected ApChange deleteChange;

	@Column(name = "deleteChangeId", nullable = true, updatable = false, insertable = false)
	protected Integer deleteChangeId;

	/* Konstanty pro vazby a fieldy. */
	public static final String EXTERNAL_ID = "externalId";
	public static final String VALUE = "value";
	public static final String ACCESS_POINT_ID = "accessPointId";
	public static final String DELETE_CHANGE_ID = "deleteChangeId";

	public Integer getExternalId() {
		return externalId;
	}

	public void setExternalId(Integer externalId) {
		this.externalId = externalId;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
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

	public ApExternalIdType getExternalIdType() {
		return externalIdType;
	}

	public void setExternalIdType(ApExternalIdType externalIdType) {
		this.externalIdType = externalIdType;
		this.externalIdTypeId = externalIdType != null ? externalIdType.getExternalIdType() : null;
	}

	public Integer getExternalIdTypeId() {
		return externalIdTypeId;
	}

	public ApChange getCreateChange() {
		return createChange;
	}

	public void setCreateChange(ApChange createChange) {
		this.createChange = createChange;
	}

	public ApChange getDeleteChange() {
		return deleteChange;
	}

	public void setDeleteChange(ApChange deleteChange) {
		this.deleteChange = deleteChange;
	}
}
