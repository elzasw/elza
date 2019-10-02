package cz.tacr.elza.domain;

import java.io.Serializable;
import java.util.Comparator;
import java.util.List;

import javax.persistence.Access;
import javax.persistence.AccessType;
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
import org.springframework.data.rest.core.annotation.RestResource;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import cz.tacr.elza.domain.enumeration.StringLength;
import cz.tacr.elza.domain.interfaces.Versionable;


/**
 * Vztah osob k entitám
 *
 * @author Martin Kužel [<a href="mailto:martin.kuzel@marbes.cz">martin.kuzel@marbes.cz</a>]
 */
@Entity(name = "par_relation")
@Table
@Inheritance(strategy = InheritanceType.JOINED)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ParRelation extends AbstractVersionableEntity implements Versionable, Serializable {

    @Id
    @GeneratedValue
    @Access(AccessType.PROPERTY) // required to read id without fetch from db
    private Integer relationId;

    @RestResource(exported = false)
    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ParParty.class)
    @JoinColumn(name = "partyId", nullable = false)
    private ParParty party;

    @RestResource(exported = false)
    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ParRelationType.class)
    @JoinColumn(name = "relationTypeId", nullable = false)
    private ParRelationType relationType;

    @Column(nullable = false, insertable = false, updatable = false)
    private Integer relationTypeId;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ParUnitdate.class)
    @JoinColumn(name = "fromUnitdateId")
    private ParUnitdate from;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ParUnitdate.class)
    @JoinColumn(name = "toUnitdateId")
    private ParUnitdate to;

    @Column(length = StringLength.LENGTH_1000)
    private String note;

    @Column
    private String source;

    @OneToMany(mappedBy = "relation", fetch = FetchType.LAZY)
    private List<ParRelationEntity> relationEntities;

    public Integer getRelationId() {
        return relationId;
    }

    public void setRelationId(final Integer relationId) {
        this.relationId = relationId;
    }

    public ParParty getParty() {
        return party;
    }

    public void setParty(final ParParty party) {
        this.party = party;
    }

    public ParRelationType getRelationType() {
        return relationType;
    }

    public void setRelationType(final ParRelationType relationType) {
        this.relationType = relationType;
        this.relationTypeId = relationType != null ? relationType.getRelationTypeId() : null;
    }

    public Integer getRelationTypeId() {
        return relationTypeId;
    }

    public ParUnitdate getFrom() {
        return from;
    }

    public void setFrom(final ParUnitdate from) {
        this.from = from;
    }

    public ParUnitdate getTo() {
        return to;
    }

    public void setTo(final ParUnitdate to) {
        this.to = to;
    }

    public String getNote() {
        return note;
    }

    public void setNote(final String note) {
        this.note = note;
    }

    public String getSource() {
        return source;
    }

    public void setSource(final String source) {
        this.source = source;
    }

    public List<ParRelationEntity> getRelationEntities() {
        return relationEntities;
    }

    public void setRelationEntities(final List<ParRelationEntity> relationEntities) {
        this.relationEntities = relationEntities;
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


    /**
     * Řazení objektů.
     */
    public static class ParRelationComparator implements Comparator<ParRelation> {

        @Override
        public int compare(final ParRelation o1, final ParRelation o2) {
            return o1.getRelationId().compareTo(o2.getRelationId());
        }
    }
}
