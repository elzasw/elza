package cz.tacr.elza.domain;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

import javax.persistence.*;

import cz.tacr.elza.domain.enumeration.StringLength;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Archivní pomůcka. Archivní pomůcka je lineárně verzována pomocí {@link ArrFundVersion}.
 * @author by Ondřej Buriánek, burianek@marbes.cz.
 * @since 22.7.15
 */
@Entity(name = "arr_fund")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "id"})
public class ArrFund extends AbstractVersionableEntity implements cz.tacr.elza.api.ArrFund<ParInstitution>, Serializable {

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

    @Override
    public Integer getFundId() {
        return fundId;
    }

    @Override
    public void setFundId(final Integer fundId) {
        this.fundId = fundId;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(final String name) {
        this.name = name;
    }

    @Override
    public LocalDateTime getCreateDate() {
        return createDate;
    }

    @Override
    public void setCreateDate(final LocalDateTime createDate) {
        this.createDate = createDate;
    }

    @Override
    public String getInternalCode() {
        return internalCode;
    }

    @Override
    public void setInternalCode(final String internalCode) {
        this.internalCode = internalCode;
    }

    @Override
    public ParInstitution getInstitution() {
        return institution;
    }

    @Override
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

    @Override
    public String getUuid() {
        return uuid;
    }

    @Override
    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public List<ArrOutputDefinition> getOutputDefinitions() {
        return outputDefinitions;
    }

    public void setOutputDefinitions(List<ArrOutputDefinition> outputDefinitions) {
        this.outputDefinitions = outputDefinitions;
    }
}
