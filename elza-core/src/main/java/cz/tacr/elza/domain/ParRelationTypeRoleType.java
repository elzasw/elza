package cz.tacr.elza.domain;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
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
@Entity(name = "par_relation_type_role_type")
@Table
//@Inheritance(strategy = InheritanceType.JOINED)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ParRelationTypeRoleType {

    @Id
    @GeneratedValue
    @Access(AccessType.PROPERTY) // required to read id without fetch from db
    private Integer relationTypeRoleTypeId;

    @RestResource(exported = false)
    @OneToOne(fetch = FetchType.LAZY, targetEntity = ParRelationType.class)
    @JoinColumn(name = "relationTypeId", nullable = false)
    private ParRelationType relationType;

    @Column(nullable = false, insertable = false, updatable = false)
    private Integer relationTypeId;

    @RestResource(exported = false)
    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ParRelationRoleType.class)
    @JoinColumn(name = "roleTypeId", nullable = false)
    private ParRelationRoleType roleType;

    @Column(nullable = false)
    private Boolean repeatable;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = RulPackage.class)
    @JoinColumn(name = "packageId", nullable = false)
    private RulPackage rulPackage;

    public Integer getRelationTypeRoleTypeId() {
        return relationTypeRoleTypeId;
    }

    public void setRelationTypeRoleTypeId(final Integer relationTypeRoleTypeId) {
        this.relationTypeRoleTypeId = relationTypeRoleTypeId;
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

    public ParRelationRoleType getRoleType() {
        return roleType;
    }

    public void setRoleType(final ParRelationRoleType roleType) {
        this.roleType = roleType;
    }

    @Override
    public boolean equals(final Object obj) {
        if (!(obj instanceof ParRelationTypeRoleType)) {
            return false;
        }
        if (this == obj) {
            return true;
        }

        ParRelationTypeRoleType other = (ParRelationTypeRoleType) obj;

        return new EqualsBuilder().append(relationTypeRoleTypeId, other.getRelationTypeRoleTypeId()).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(relationTypeRoleTypeId).toHashCode();
    }

    @Override
    public String toString() {
        return "ParRelationTypeRoleType pk=" + relationTypeRoleTypeId;
    }

    public Boolean getRepeatable() {
        return repeatable;
    }

    public void setRepeatable(final Boolean repeatable) {
        this.repeatable = repeatable;
    }

    public RulPackage getRulPackage() {
        return rulPackage;
    }

    public void setRulPackage(final RulPackage rulPackage) {
        this.rulPackage = rulPackage;
    }
}
