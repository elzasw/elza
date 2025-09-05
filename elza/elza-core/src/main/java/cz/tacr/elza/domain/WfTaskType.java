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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import cz.tacr.elza.domain.enumeration.StringLength;

@Entity(name = "wf_task_type")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "id"})
public class WfTaskType {

	public static final String AP_CONFIRM = "AP_CONFIRM";
	public static final String AP_UPDATE = "AP_UPDATE";

	public static final String AP_REV_CONFIRM = "AP_REV_CONFIRM";
	public static final String AP_REV_UPDATE = "AP_REV_UPDATE";

	@Id
    @GeneratedValue
    @Access(AccessType.PROPERTY)
    private Integer taskTypeId;

    @Column(length = StringLength.LENGTH_50, nullable = false, unique = true)
    private String code;

    @Column(length = StringLength.LENGTH_250, nullable = false)
    private String name;

    @Column(length = StringLength.LENGTH_250, nullable = true)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = RulPackage.class, optional = false)
    @JoinColumn(name = "package_id", nullable = false)
    private RulPackage rulPackage;

    // --- getters/setters ---

    public Integer getTaskTypeId() {
        return taskTypeId;
    }

    public void setTaskTypeId(Integer taskTypeId) {
        this.taskTypeId = taskTypeId;
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

    public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public RulPackage getRulPackage() {
        return rulPackage;
    }

    public void setRulPackage(RulPackage rulPackage) {
        this.rulPackage = rulPackage;
    }

    // --- methods ---

    @Override
    public String toString() {
        return "WfTaskType pk=" + taskTypeId;
    }
}
