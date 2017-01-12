package cz.tacr.elza.domain;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

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

import cz.tacr.elza.api.Versionable;
import cz.tacr.elza.api.interfaces.IArrFund;
import cz.tacr.elza.domain.enumeration.StringLength;

/**
 * Archivní pomůcka. Archivní pomůcka je lineárně verzována pomocí {@link ArrFundVersion}.
 * @author by Ondřej Buriánek, burianek@marbes.cz.
 * @since 22.7.15
 */
@Entity(name = "arr_fund")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "id"})
public class ArrFund extends AbstractVersionableEntity implements Versionable, Serializable, IArrFund {

    @Id
    @GeneratedValue
    private Integer fundId;

    @Column(length = 255, nullable = false)
    private String name;

    @Column(nullable = false)
    private LocalDateTime createDate;

    @Column(length = 250)
    private String internalCode;

    @Column(length = StringLength.LENGTH_36)
    private String uuid;

    @OneToOne(fetch = FetchType.LAZY, targetEntity = ParInstitution.class)
    @JoinColumn(name = "institutionId", nullable = false)
    private ParInstitution institution;

    @OneToMany(mappedBy = "fund", fetch = FetchType.LAZY)
    private List<ArrFundVersion> versions;

    @OneToMany(mappedBy = "fund", fetch = FetchType.LAZY)
    private List<ArrOutputDefinition> outputDefinitions;

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

    public String getUuid() {
        return uuid;
    }

    public void setUuid(final String uuid) {
        this.uuid = uuid;
    }

    public List<ArrOutputDefinition> getOutputDefinitions() {
        return outputDefinitions;
    }

    public void setOutputDefinitions(final List<ArrOutputDefinition> outputDefinitions) {
        this.outputDefinitions = outputDefinitions;
    }
}
