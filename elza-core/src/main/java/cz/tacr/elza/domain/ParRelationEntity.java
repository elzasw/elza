package cz.tacr.elza.domain;

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
import javax.persistence.Lob;
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
@Entity(name = "par_relation_entity")
@Table
@Inheritance(strategy = InheritanceType.JOINED)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ParRelationEntity {

    @Id
    @GeneratedValue
    @Access(AccessType.PROPERTY) // required to read id without fetch from db
    private Integer relationEntityId;

    @RestResource(exported = false)
    @OneToOne(fetch = FetchType.LAZY, targetEntity = ParRelation.class)
    @JoinColumn(name = "relationId", nullable = false)
    private ParRelation relation;

    @RestResource(exported = false)
    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ApAccessPoint.class)
    @JoinColumn(name = "recordId", nullable = false)
    private ApAccessPoint accessPoint;

    @RestResource(exported = false)
    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ParRelationRoleType.class)
    @JoinColumn(name = "roleTypeId", nullable = false)
    private ParRelationRoleType roleType;

    @Column(nullable = false, insertable = false, updatable = false)
    private Integer roleTypeId;

    @Column
    @Lob
    @org.hibernate.annotations.Type(type = "org.hibernate.type.TextType")
    private String note;

    public Integer getRelationEntityId() {
        return relationEntityId;
    }

    public void setRelationEntityId(final Integer relationEntityId) {
        this.relationEntityId = relationEntityId;
    }

    public ParRelation getRelation() {
        return relation;
    }

    public void setRelation(final ParRelation relation) {
        this.relation = relation;
    }

    public ApAccessPoint getAccessPoint() {
        return accessPoint;
    }

    public void setAccessPoint(ApAccessPoint accessPoint) {
        this.accessPoint = accessPoint;
    }

    public ParRelationRoleType getRoleType() {
        return roleType;
    }

    public void setRoleType(final ParRelationRoleType roleType) {
        this.roleType = roleType;
        this.roleTypeId = roleType != null ? roleType.getRoleTypeId() : null;
    }

    public Integer getRoleTypeId() {
        return roleTypeId;
    }

    public String getNote() {
        return note;
    }

    public void setNote(final String note) {
        this.note = note;
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
