package cz.tacr.elza.domain;

import java.time.LocalDateTime;
import java.util.List;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import cz.tacr.elza.api.interfaces.IArrFund;
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
public class ArrFund extends AbstractVersionableEntity implements Versionable, IArrFund {

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
