package cz.tacr.elza.domain;

import cz.tacr.elza.domain.enumeration.StringLength;
import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Entity(name = "rpt_param")
public class RptParam {

    @Id
    @GeneratedValue
    @Access(AccessType.PROPERTY)
    private Integer paramId;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = RptReport.class)
    @JoinColumn(name = "report_id")
    private RptReport report;

    @Column(name = "report_id", updatable = false, insertable = false)
    private Integer reportId;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = RptValueType.class)
    @JoinColumn(name = "value_type_id")
    private RptValueType valueType;

    @Column(name = "value_type_id", updatable = false, insertable = false)
    private Integer valueTypeId;

    @Column(length = StringLength.LENGTH_50, nullable = false)
    private String code;

    @Column(length = StringLength.LENGTH_250, nullable = false)
    private String name;

    @Column
    private Boolean repeatable;

    @Column
    private Boolean required;

    @Enumerated(EnumType.STRING)
    @Column(length = StringLength.LENGTH_ENUM, nullable = true)
    private RptDefaultValueGenerator generator;

	public Integer getParamId() {
		return paramId;
	}

	public void setParamId(Integer paramId) {
		this.paramId = paramId;
	}

    public Integer getReportId() {
        return reportId;
    }

    public RptReport getReport() {
		return report;
	}

	public void setReport(RptReport report) {
		this.report = report;
		this.reportId = (report != null) ? report.getReportId() : null;
	}

    public Integer getValueTypeId() {
        return valueTypeId;
    }

    public RptValueType getValueType() {
		return valueType;
	}

	public void setValueType(RptValueType valueType) {
		this.valueType = valueType;
		this.valueTypeId = (valueType != null) ? valueType.getValueTypeId() : null;
	}

    public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Boolean getRepeatable() {
		return repeatable;
	}

	public void setRepeatable(Boolean repeatable) {
		this.repeatable = repeatable;
	}

	public Boolean getRequired() {
		return required;
	}

	public void setRequired(Boolean required) {
		this.required = required;
	}

	public RptDefaultValueGenerator getGenerator() {
		return generator;
	}

	public void setGenerator(RptDefaultValueGenerator generator) {
		this.generator = generator;
	}

}