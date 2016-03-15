package cz.tacr.elza.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;

import org.springframework.data.rest.core.annotation.RestResource;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;


@Entity(name = "arr_version_conformity")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ArrVersionConformity
        implements cz.tacr.elza.api.ArrVersionConformity<ArrFundVersion> {

    @Id
    @GeneratedValue
    private Integer versionConformityId;

    @Enumerated(EnumType.STRING)
    @Column(length = 3, nullable = true)
    private cz.tacr.elza.domain.ArrVersionConformity.State state;

    @Column(length = 1000, nullable = true)
    private String stateDescription;

    @RestResource(exported = false)
    @OneToOne(fetch = FetchType.LAZY, targetEntity = ArrFundVersion.class)
    @JoinColumn(name = "fundVersionId", nullable = false)
    private ArrFundVersion fundVersion;

    @Override
    public Integer getVersionConformityId() {
        return versionConformityId;
    }

    @Override
    public void setVersionConformityId(final Integer versionConformityId) {
        this.versionConformityId = versionConformityId;
    }

    @Override
    public ArrFundVersion getFundVersion() {
        return fundVersion;
    }

    @Override
    public void setFundVersion(final ArrFundVersion fundVersion) {
        this.fundVersion = fundVersion;
    }

    @Override
    public State getState() {
        return state;
    }

    @Override
    public void setState(final State state) {
        this.state = state;
    }

    @Override
    public String getStateDescription() {
        return stateDescription;
    }

    @Override
    public void setStateDescription(final String stateDescription) {
        this.stateDescription = stateDescription;
    }


}
