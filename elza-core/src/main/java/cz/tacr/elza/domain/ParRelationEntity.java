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
@Entity(name = "par_relation_entity")
@Table
@Inheritance(strategy = InheritanceType.JOINED)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ParRelationEntity implements cz.tacr.elza.api.ParRelationEntity<ParRelation, RegRecord, ParRelationRoleType> {

    @Id
    @GeneratedValue
    private Integer relationEntityId;

    @RestResource(exported = false)
    @OneToOne(fetch = FetchType.LAZY, targetEntity = ParRelation.class)
    @JoinColumn(name = "relationId", nullable = false)
    private ParRelation relation;

    @RestResource(exported = false)
    @ManyToOne(fetch = FetchType.LAZY, targetEntity = RegRecord.class)
    @JoinColumn(name = "recordId", nullable = false)
    private RegRecord record;

    @RestResource(exported = false)
    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ParRelationRoleType.class)
    @JoinColumn(name = "roleTypeId", nullable = false)
    private ParRelationRoleType roleType;


    @Override
    public Integer getRelationEntityId() {
        return relationEntityId;
    }

    @Override
    public void setRelationEntityId(final Integer relationEntityId) {
        this.relationEntityId = relationEntityId;
    }

    @Override
    public ParRelation getRelation() {
        return relation;
    }

    @Override
    public void setRelation(final ParRelation relation) {
        this.relation = relation;
    }

    @Override
    public RegRecord getRecord() {
        return record;
    }

    @Override
    public void setRecord(final RegRecord record) {
        this.record = record;
    }

    @Override
    public ParRelationRoleType getRoleType() {
        return roleType;
    }

    @Override
    public void setRoleType(final ParRelationRoleType roleType) {
        this.roleType = roleType;
    }



    @Override
    public boolean equals(final Object obj) {
        if (!(obj instanceof ParRelationEntity)) {
            return false;
        }
        if (this == obj) {
            return true;
        }

        ParRelationEntity other = (ParRelationEntity) obj;

        return new EqualsBuilder().append(relationEntityId, other.getRelationEntityId()).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(relationEntityId).toHashCode();
    }

    @Override
    public String toString() {
        return "ParRelationEntity pk=" + relationEntityId;
    }
}
