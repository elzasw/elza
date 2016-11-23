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
@Entity(name = "par_relation_type_role_type")
@Table
@Inheritance(strategy = InheritanceType.JOINED)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ParRelationTypeRoleType implements cz.tacr.elza.api.ParRelationTypeRoleType<ParRelationType, ParRelationRoleType> {

    @Id
    @GeneratedValue
    private Integer relationTypeRoleTypeId;

    @RestResource(exported = false)
    @OneToOne(fetch = FetchType.LAZY, targetEntity = ParRelationType.class)
    @JoinColumn(name = "relationTypeId", nullable = false)
    private ParRelationType relationType;

    @RestResource(exported = false)
    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ParRelationRoleType.class)
    @JoinColumn(name = "roleTypeId", nullable = false)
    private ParRelationRoleType roleType;

    @Column(nullable = false)
    private Boolean repeatable;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = RulPackage.class)
    @JoinColumn(name = "packageId", nullable = false)
    private RulPackage rulPackage;

    @Override
    public Integer getRelationTypeRoleTypeId() {
        return relationTypeRoleTypeId;
    }

    @Override
    public void setRelationTypeRoleTypeId(final Integer relationTypeRoleTypeId) {
        this.relationTypeRoleTypeId = relationTypeRoleTypeId;
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
    public ParRelationRoleType getRoleType() {
        return roleType;
    }

    @Override
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

    @Override
    public Boolean getRepeatable() {
        return repeatable;
    }

    @Override
    public void setRepeatable(Boolean repeatable) {
        this.repeatable = repeatable;
    }

    public RulPackage getRulPackage() {
        return rulPackage;
    }

    public void setRulPackage(final RulPackage rulPackage) {
        this.rulPackage = rulPackage;
    }
}
