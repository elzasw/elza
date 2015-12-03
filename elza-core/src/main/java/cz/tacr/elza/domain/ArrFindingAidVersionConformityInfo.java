package cz.tacr.elza.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;

import org.springframework.data.rest.core.annotation.RestResource;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;


@Entity(name = "arr_fa_version_conformity_info")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ArrFindingAidVersionConformityInfo
        implements cz.tacr.elza.api.ArrFindingAidVersionConformityInfo<ArrFindingAidVersion> {

    @Id
    @GeneratedValue
    private Integer faVersionConformityInfoId;

    @Enumerated(EnumType.STRING)
    @Column(length = 3, nullable = true)
    private ArrFindingAidVersionConformityInfo.State state;

    @Column(length = 1000, nullable = true)
    private String stateDescription;

    @RestResource(exported = false)
    @OneToOne(fetch = FetchType.LAZY, targetEntity = ArrFindingAidVersion.class)
    @JoinColumn(name = "faVersionId", nullable = false)
    private ArrFindingAidVersion faVersion;

    @Override
    public Integer getFindingAidVersionConformityInfoId() {
        return faVersionConformityInfoId;
    }

    @Override
    public void setFindingAidVersionConformityInfoId(final Integer findingAidVersionConformityInfoId) {
        this.faVersionConformityInfoId = findingAidVersionConformityInfoId;
    }

    @Override
    public ArrFindingAidVersion getFaVersion() {
        return faVersion;
    }

    @Override
    public void setFaVersion(final ArrFindingAidVersion faVersion) {
        this.faVersion = faVersion;
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
