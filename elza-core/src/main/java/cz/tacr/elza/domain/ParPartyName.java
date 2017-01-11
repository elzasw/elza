package cz.tacr.elza.domain;

import java.io.Serializable;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import cz.tacr.elza.domain.enumeration.StringLength;


/**
 * Jméno abstraktní osoby.
 *
 * @author Martin Kužel [<a href="mailto:martin.kuzel@marbes.cz">martin.kuzel@marbes.cz</a>]
 */
@Entity(name = "par_party_name")
@Table
@Inheritance(strategy = InheritanceType.JOINED)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ParPartyName implements Serializable {

    public static final String PARTY = "party";

    @Id
    @GeneratedValue
    private Integer partyNameId;

    @ManyToOne(fetch = FetchType.EAGER, targetEntity = ParUnitdate.class)
    @JoinColumn(name = "validFromUnitdateId")
    private ParUnitdate validFrom;

    @ManyToOne(fetch = FetchType.EAGER, targetEntity = ParUnitdate.class)
    @JoinColumn(name = "validToUnitdateId")
    private ParUnitdate validTo;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ParPartyNameFormType.class)
    @JoinColumn(name = "nameFormTypeId")
    private ParPartyNameFormType nameFormType;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ParParty.class)
    @JoinColumn(name = "partyId", nullable = false)
    private ParParty party;

    @Column(length = StringLength.LENGTH_250, nullable = false)
    private String mainPart;

    @Column(length = StringLength.LENGTH_250)
    private String otherPart;

    @Column()
    private String note;

    @Column(length = StringLength.LENGTH_50)
    private String degreeBefore;

    @Column(length = StringLength.LENGTH_50)
    private String degreeAfter;


    @OneToMany(mappedBy = "partyName", fetch = FetchType.EAGER)
    private List<ParPartyNameComplement> partyNameComplements;

    public Integer getPartyNameId() {
        return partyNameId;
    }

    public void setPartyNameId(final Integer partyNameId) {
        this.partyNameId = partyNameId;
    }

    public ParParty getParty() {
        return party;
    }

    public void setParty(final ParParty party) {
        this.party = party;
    }

    public String getMainPart() {
        return mainPart;
    }

    public void setMainPart(final String mainPart) {
        this.mainPart = mainPart;
    }

    public String getOtherPart() {
        return otherPart;
    }

    public void setOtherPart(final String otherPart) {
        this.otherPart = otherPart;
    }

    public String getDegreeBefore() {
        return degreeBefore;
    }

    public void setDegreeBefore(final String degreeBefore) {
        this.degreeBefore = degreeBefore;
    }

    public String getDegreeAfter() {
        return degreeAfter;
    }

    public void setDegreeAfter(final String degreeAfter) {
        this.degreeAfter = degreeAfter;
    }

    public ParUnitdate getValidFrom() {
        return validFrom;
    }

    public void setValidFrom(final ParUnitdate validFrom) {
        this.validFrom = validFrom;
    }

    public ParUnitdate getValidTo() {
        return validTo;
    }

    public void setValidTo(final ParUnitdate validTo) {
        this.validTo = validTo;
    }

    public ParPartyNameFormType getNameFormType() {
        return nameFormType;
    }

    public void setNameFormType(final ParPartyNameFormType nameFormType) {
        this.nameFormType = nameFormType;
    }

    public String getNote() {
        return note;
    }

    public void setNote(final String note) {
        this.note = note;
    }

    public List<ParPartyNameComplement> getPartyNameComplements() {
        return partyNameComplements;
    }

    public void setPartyNameComplements(final List<ParPartyNameComplement> partyNameComplements) {
        this.partyNameComplements = partyNameComplements;
    }

    @Override
    public boolean equals(final Object obj) {
        if (!(obj instanceof ParPartyName)) {
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
