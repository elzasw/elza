package cz.tacr.elza.domain;

import java.time.LocalDateTime;
import java.util.List;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import cz.tacr.elza.api.interfaces.ArrFundGetter;
import cz.tacr.elza.domain.enumeration.StringLength;
import cz.tacr.elza.domain.interfaces.Versionable;

/**
 * Archivní pomůcka. Archivní pomůcka je lineárně verzována pomocí
 * {@link ArrFundVersion}.
 *
 * @since 22.7.15
 */
@Entity(name = "arr_fund")
@Cache(region = "fund", usage = CacheConcurrencyStrategy.READ_WRITE)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "id"})
public class ArrFund extends AbstractVersionableEntity implements Versionable, ArrFundGetter {

	@Id
	@GeneratedValue
    @Access(AccessType.PROPERTY) // required to read id without fetch from db
	private Integer fundId;

    @Column(length = 255, nullable = false)
	private String name;

	@Column(nullable = false)
	private LocalDateTime createDate;

	@Column(length = 250)
	private String internalCode;

	@Column
    private Integer fundNumber;

    @Column(length = StringLength.LENGTH_50)
    private String unitdate;

    @Column(length = StringLength.LENGTH_50)
	private String mark;

	@OneToOne(fetch = FetchType.LAZY, targetEntity = ParInstitution.class)
	@JoinColumn(name = "institutionId", nullable = false)
	private ParInstitution institution;

	@OneToMany(mappedBy = "fund", fetch = FetchType.LAZY)
	private List<ArrFundVersion> versions;

	@OneToMany(mappedBy = "fund", fetch = FetchType.LAZY)
	private List<ArrOutput> outputs;

	public Integer getFundId() {
		return fundId;
	}

	public void setFundId(final Integer fundId) {
		this.fundId = fundId;
	}

	public String getName() {
		return name;
	}

	public void setName(final String name) {
		this.name = name;
	}

	public LocalDateTime getCreateDate() {
		return createDate;
	}

	public void setCreateDate(final LocalDateTime createDate) {
		this.createDate = createDate;
	}

	public String getInternalCode() {
		return internalCode;
	}

	public void setInternalCode(final String internalCode) {
		this.internalCode = internalCode;
	}

    public Integer getFundNumber() {
        return fundNumber;
    }

    public void setFundNumber(Integer fundNumber) {
        this.fundNumber = fundNumber;
    }

	public String getUnitdate() {
		return unitdate;
	}

	public void setUnitdate(String unitdate) {
		this.unitdate = unitdate;
	}

	public String getMark() {
		return mark;
	}

	public void setMark(String mark) {
		this.mark = mark;
	}

	public ParInstitution getInstitution() {
		return institution;
	}

	public void setInstitution(final ParInstitution institution) {
		this.institution = institution;
	}

	public List<ArrFundVersion> getVersions() {
		return versions;
	}

	public void setVersions(final List<ArrFundVersion> versions) {
		this.versions = versions;
	}

	@Override
	public String toString() {
		return "ArrFund pk=" + fundId;
	}

	@Override
	public ArrFund getFund() {
		return this;
	}

	public List<ArrOutput> getOutputs() {
		return outputs;
	}

	public void setOutputs(final List<ArrOutput> outputs) {
		this.outputs = outputs;
	}
}
