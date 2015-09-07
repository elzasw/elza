package cz.tacr.elza.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

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
import java.time.LocalDateTime;


/**
 * Jméno abstraktní osoby.
 *
 * @author Martin Kužel [<a href="mailto:martin.kuzel@marbes.cz">martin.kuzel@marbes.cz</a>]
 */
@Entity(name = "par_abstract_party_name")
@Table
@Inheritance(strategy = InheritanceType.JOINED)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ParAbstractPartyName extends AbstractVersionableEntity
        implements cz.tacr.elza.api.ParAbstractPartyName<ParAbstractParty> {

    @Id
    @GeneratedValue
    private Integer abstractPartyNameId;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ParAbstractParty.class)
    @JoinColumn(name = "abstractPartyId", nullable = false)
    private ParAbstractParty abstractParty;

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
    public Integer getAbstractPartyNameId() {
        return abstractPartyNameId;
    }

    @Override
    public void setAbstractPartyNameId(final Integer abstractPartyNameId) {
        this.abstractPartyNameId = abstractPartyNameId;
    }

    @Override
    public ParAbstractParty getAbstractParty() {
        return abstractParty;
    }

    @Override
    public void setAbstractParty(final ParAbstractParty abstractParty) {
        this.abstractParty = abstractParty;
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
        if (!(obj instanceof cz.tacr.elza.api.ParAbstractPartyName)) {
            return false;
        }
        if (this == obj) {
            return true;
        }

        ParAbstractPartyName other = (ParAbstractPartyName) obj;

        return new EqualsBuilder().append(abstractPartyNameId, other.getAbstractPartyNameId()).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(abstractPartyNameId).toHashCode();
    }

}
