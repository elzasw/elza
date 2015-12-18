package cz.tacr.elza.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;


/**
 * Seznam rolí entit ve vztahu.
 */
@Entity(name = "par_relation_role_type")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "id"})
public class ParRelationRoleType implements cz.tacr.elza.api.ParRelationRoleType {

    @Id
    @GeneratedValue
    private Integer roleTypeId;

    @Column(length = 250, nullable = false)
    private String name;

    @Column(length = 50, nullable = false, unique = true)
    private String code;

    @Override
    public Integer getRoleTypeId() {
        return roleTypeId;
    }

    @Override
    public void setRoleTypeId(final Integer roleTypeId) {
        this.roleTypeId = roleTypeId;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(final String name) {
        this.name = name;
    }

    @Override
    public String getCode() {
        return code;
    }

    @Override
    public void setCode(final String code) {
        this.code = code;
    }


    @Override
    public boolean equals(final Object obj) {
        if (!(obj instanceof cz.tacr.elza.api.ParRelationRoleType)) {
            return false;
        }
        if (this == obj) {
            return true;
        }

        ParRelationRoleType other = (ParRelationRoleType) obj;

        return new EqualsBuilder().append(roleTypeId, other.getRoleTypeId()).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(roleTypeId).toHashCode();
    }

    @Override
    public String toString() {
        return "ParRelationRoleType pk=" + roleTypeId;
    }

}
