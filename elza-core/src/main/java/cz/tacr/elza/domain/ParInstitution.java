package cz.tacr.elza.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import javax.persistence.*;

/**
 * Implementace {@link cz.tacr.elza.api.ParInstitution}.
 *
 * @author Martin Šlapa
 * @since 18.3.2016
 */
@Entity(name = "par_institution")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "id"})
public class ParInstitution implements cz.tacr.elza.api.ParInstitution<ParInstitutionType, ParParty> {

    @Id
    @GeneratedValue
    private Integer institutionId;

    @Column(length = 50, nullable = false, unique = true)
    private String code;

    @OneToOne(fetch = FetchType.LAZY, targetEntity = ParInstitutionType.class)
    @JoinColumn(name = "institutionTypeId", nullable = false)
    private ParInstitutionType institutionType;

    @OneToOne(fetch = FetchType.LAZY, targetEntity = ParParty.class)
    @JoinColumn(name = "partyId", nullable = false)
    private ParParty party;

    @Override
    public Integer getInstitutionId() {
        return institutionId;
    }

    @Override
    public void setInstitutionId(final Integer institutionId) {
        this.institutionId = institutionId;
    }

    @Override
    public String getCode() {
        return code;
    }

    @Override
    public void setCode(final String code) {
        this.code = code;
    }

    @Override
    public ParInstitutionType getInstitutionType() {
        return institutionType;
    }

    @Override
    public void setInstitutionType(final ParInstitutionType institutionType) {
        this.institutionType = institutionType;
    }

    @Override
    public ParParty getParty() {
        return party;
    }

    @Override
    public void setParty(final ParParty party) {
        this.party = party;
    }
}
