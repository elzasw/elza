package cz.tacr.elza.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.springframework.data.rest.core.annotation.RestResource;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Table;


/**
 * //TODO marik missing comment
 *
 * @author Martin Kužel [<a href="mailto:martin.kuzel@marbes.cz">martin.kuzel@marbes.cz</a>]
 */
@Entity(name = "par_relation")
@Table
@Inheritance(strategy = InheritanceType.JOINED)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ParRelation extends AbstractVersionableEntity implements cz.tacr.elza.api.ParRelation<ParParty, ParRelationType, ParUnitdate> {

    @Id
    @GeneratedValue
    private Integer relationId;

    @RestResource(exported = false)
    @OneToOne(fetch = FetchType.LAZY, targetEntity = ParParty.class)
    @JoinColumn(name = "partyId", nullable = false)
    private ParParty party;

    @RestResource(exported = false)
    @OneToOne(fetch = FetchType.LAZY, targetEntity = ParRelationType.class)
    @JoinColumn(name = "relationTypeId", nullable = false)
    private ParRelationType complementType;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ParUnitdate.class)
    @JoinColumn(name = "fromUnitdateId")
    private ParUnitdate from;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ParUnitdate.class)
    @JoinColumn(name = "toUnitdateId")
    private ParUnitdate to;

    @Column(length = 1000)
    private String dateNote;

    @Column(length = 1000)
    private String note;


    @Override
    public Integer getRelationId() {
        return relationId;
    }

    @Override
    public void setRelationId(final Integer relationId) {
        this.relationId = relationId;
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
    public ParRelationType getComplementType() {
        return complementType;
    }

    @Override
    public void setComplementType(final ParRelationType complementType) {
        this.complementType = complementType;
    }

    @Override
    public ParUnitdate getFrom() {
        return from;
    }

    @Override
    public void setFrom(final ParUnitdate from) {
        this.from = from;
    }

    @Override
    public ParUnitdate getTo() {
        return to;
    }

    @Override
    public void setTo(final ParUnitdate to) {
        this.to = to;
    }

    @Override
    public String getDateNote() {
        return dateNote;
    }

    @Override
    public void setDateNote(final String dateNote) {
        this.dateNote = dateNote;
    }

    @Override
    public String getNote() {
        return note;
    }

    @Override
    public void setNote(final String note) {
        this.note = note;
    }

    @Override
    public boolean equals(final Object obj) {
        if (!(obj instanceof ParRelation)) {
            return false;
        }
        if (this == obj) {
            return true;
        }

        ParRelation other = (ParRelation) obj;

        return new EqualsBuilder().append(relationId, other.getRelationId()).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(relationId).toHashCode();
    }

    @Override
    public String toString() {
        return "ParRelation pk=" + relationId;
    }
}
