package cz.tacr.elza.domain;

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

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.springframework.data.rest.core.annotation.RestResource;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;


/**
 * //TODO marik missing comment
 *
 * @author Martin Kužel [<a href="mailto:martin.kuzel@marbes.cz">martin.kuzel@marbes.cz</a>]
 */
@Entity(name = "par_party_type_relation")
@Table
@Inheritance(strategy = InheritanceType.JOINED)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ParPartyTypeRelation implements cz.tacr.elza.api.ParPartyTypeRelation<ParPartyType, ParRelationType> {

    @Id
    @GeneratedValue
    private Integer partyTypeRelationId;

    @RestResource(exported = false)
    @OneToOne(fetch = FetchType.LAZY, targetEntity = ParRelationType.class)
    @JoinColumn(name = "relationTypeId", nullable = false)
    private ParRelationType relationType;

    @RestResource(exported = false)
    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ParPartyType.class)
    @JoinColumn(name = "partyTypeId", nullable = false)
    private ParPartyType partyType;

    @Column(nullable = false)
    private boolean repeatable;

    @Column(nullable = false)
    private Integer viewOrder;

    @Override
    public Integer getPartyTypeRelationId() {
        return partyTypeRelationId;
    }

    @Override
    public void setPartyTypeRelationId(final Integer partyTypeRelationId) {
        this.partyTypeRelationId = partyTypeRelationId;
    }

    @Override
    public ParRelationType getRelationType() {
        return relationType;
    }

    @Override
    public void setRelationType(final ParRelationType relationType) {
        this.relationType = relationType;
    }

    @Override
    public ParPartyType getPartyType() {
        return partyType;
    }

    @Override
    public void setPartyType(final ParPartyType partyType) {
        this.partyType = partyType;
    }

    @Override
    public boolean equals(final Object obj) {
        if (!(obj instanceof ParPartyTypeRelation)) {
            return false;
        }
        if (this == obj) {
            return true;
        }

        ParPartyTypeRelation other = (ParPartyTypeRelation) obj;

        return new EqualsBuilder().append(partyTypeRelationId, other.getPartyTypeRelationId()).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(partyTypeRelationId).toHashCode();
    }

    @Override
    public String toString() {
        return "ParPartyTypeRelation pk=" + partyTypeRelationId;
    }
}
