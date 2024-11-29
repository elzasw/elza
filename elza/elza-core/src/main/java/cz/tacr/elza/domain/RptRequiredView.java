package cz.tacr.elza.domain;

import cz.tacr.elza.domain.enumeration.StringLength;
import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Entity(name = "rpt_required_view")
public class RptRequiredView {

    @Id
    @GeneratedValue
    @Access(AccessType.PROPERTY)
    private Integer viewId;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = RptReport.class)
    @JoinColumn(name = "report_id")
    private RptReport report;

    @Column(length = StringLength.LENGTH_250, nullable = false)
    private String viewName;

	public Integer getViewId() {
		return viewId;
	}

	public void setViewId(Integer viewId) {
		this.viewId = viewId;
	}

	public RptReport getReport() {
		return report;
	}

	public void setReport(RptReport report) {
		this.report = report;
	}

	public String getViewName() {
		return viewName;
	}

	public void setViewName(String viewName) {
		this.viewName = viewName;
	}
}
