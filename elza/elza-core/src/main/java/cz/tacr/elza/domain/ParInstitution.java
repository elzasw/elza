package cz.tacr.elza.domain;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Instituce.
 *
 * @author Martin Šlapa
 * @since 18.3.2016
 */
@Entity(name = "par_institution")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "id"})
public class ParInstitution {

    @Id
    @GeneratedValue
    @Access(AccessType.PROPERTY) // required to read id without fetch from db
    private Integer institutionId;

    @Column(length = 250)
    private String internalCode;

    @OneToOne(fetch = FetchType.LAZY, targetEntity = ParInstitutionType.class)
    @JoinColumn(name = "institutionTypeId", nullable = false)
    private ParInstitutionType institutionType;

    @OneToOne(fetch = FetchType.LAZY, targetEntity = ParParty.class)
    @JoinColumn(name = "partyId", nullable = false)
    private ParParty party;

    @Column(updatable = false, insertable = false, nullable = false)
    private Integer partyId;

    /**
     * @return identifikátor
     */
    public Integer getInstitutionId() {
        return institutionId;
    }

    /**
     * @param institutionId identifikátor
     */
    public void setInstitutionId(final Integer institutionId) {
        this.institutionId = institutionId;
    }

    /**
     * @return kód instituce
     */
    public String getInternalCode() {
        return internalCode;
    }

    /**
     * @param internalCode kód instituce
     */
    public void setInternalCode(final String internalCode) {
        this.internalCode = internalCode;
    }

    /**
     * @return typ instituce
     */
    public ParInstitutionType getInstitutionType() {
        return institutionType;
    }

    /**
     * @param institutionType typ instituce
     */
    public void setInstitutionType(final ParInstitutionType institutionType) {
        this.institutionType = institutionType;
    }

    /**
     * @return osoba
     */
    public ParParty getParty() {
        return party;
    }

    /**
     * @param party osoba
     */
    public void setParty(final ParParty party) {
        this.party = party;
        this.partyId = party == null ? null : party.getPartyId();
    }

    public Integer getPartyId() {
        return partyId;
    }
}
