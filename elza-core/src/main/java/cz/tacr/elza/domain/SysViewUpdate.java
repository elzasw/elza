package cz.tacr.elza.domain;

import java.time.OffsetDateTime;

import cz.tacr.elza.domain.enumeration.StringLength;
import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

@Entity(name = "sys_view_update")
public class SysViewUpdate {

    @Id
    @GeneratedValue
    @Access(AccessType.PROPERTY)
    private Integer sysViewId;

    @Column(length = StringLength.LENGTH_250, nullable = false)
    private String viewName;

    @Column(nullable = false)
    private OffsetDateTime lastUpdate;

	public Integer getSysViewId() {
		return sysViewId;
	}

	public void setSysViewId(Integer sysViewId) {
		this.sysViewId = sysViewId;
	}

	public String getViewName() {
		return viewName;
	}

	public void setViewName(String viewName) {
		this.viewName = viewName;
	}

	public OffsetDateTime getLastRefresh() {
		return lastUpdate;
	}

	public void setLastRefresh(OffsetDateTime lastUpdate) {
		this.lastUpdate = lastUpdate;
	}
}
