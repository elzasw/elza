package cz.tacr.elza.domain;

import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;


/**
 * Jméno abstraktní osoby.
 *
 * @author Martin Kužel [<a href="mailto:martin.kuzel@marbes.cz">martin.kuzel@marbes.cz</a>]
 */
@Entity(name = "par_party_name")
@Table
@Inheritance(strategy = InheritanceType.JOINED)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ParPartyName extends AbstractVersionableEntity
        implements cz.tacr.elza.api.ParPartyName<ParParty> {

    @Id
    @GeneratedValue
    private Integer partyNameId;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ParParty.class)
    @JoinColumn(name = "partyId", nullable = false)
    private ParParty party;

    @Column(length = 255)
    private String mainPart;

    @Column(length = 255)
    private String otherPart;

    @Column()
    private String anotation;

    @Column(length = 50)
    private String degreeBefore;

    @Column(length = 50)
    private String degreeAfter;

    @Column()
    private LocalDateTime validFrom;

    @Column()
    private LocalDateTime validTo;


    @Override
    public Integer getPartyNameId() {
        return partyNameId;
    }

    @Override
    public void setPartyNameId(final Integer partyNameId) {
        this.partyNameId = partyNameId;
    }

    @Override
    public ParParty getParty() {
        return party;
    }

    @Override
    public void setParty(final ParParty party) {
        this.party = party;
    }

    @Override
    public String getMainPart() {
        return mainPart;
    }

    @Override
    public void setMainPart(String mainPart) {
        this.mainPart = mainPart;
    }

    @Override
    public String getOtherPart() {
        return otherPart;
    }

    @Override
    public void setOtherPart(String otherPart) {
        this.otherPart = otherPart;
    }

    @Override
    public String getAnotation() {
        return anotation;
    }

    @Override
    public void setAnotation(String anotation) {
        this.anotation = anotation;
    }

    @Override
    public String getDegreeBefore() {
        return degreeBefore;
    }

    @Override
    public void setDegreeBefore(String degreeBefore) {
        this.degreeBefore = degreeBefore;
    }

    @Override
    public String getDegreeAfter() {
        return degreeAfter;
    }

    @Override
    public void setDegreeAfter(String degreeAfter) {
        this.degreeAfter = degreeAfter;
    }

    @Override
    public LocalDateTime getValidFrom() {
        return validFrom;
    }

    @Override
    public void setValidFrom(LocalDateTime validFrom) {
        this.validFrom = validFrom;
    }

    @Override
    public LocalDateTime getValidTo() {
        return validTo;
    }

    @Override
    public void setValidTo(LocalDateTime validTo) {
        this.validTo = validTo;
    }

    @Override
    public boolean equals(final Object obj) {
        if (!(obj instanceof cz.tacr.elza.api.ParPartyName)) {
            return false;
        }
        if (this == obj) {
            return true;
        }

        ParPartyName other = (ParPartyName) obj;

        return new EqualsBuilder().append(partyNameId, other.getPartyNameId()).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(partyNameId).toHashCode();
    }

    @Override
    public String toString() {
        return "ParPartyName pk=" + partyNameId;
    }
}
