package cz.tacr.elza.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;


/**
 * Seznam typů vztahů.
 */
@Entity(name = "par_relation_type")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "id"})
public class ParRelationType implements cz.tacr.elza.api.ParRelationType {

    public enum ClassType{
        VZNIK("B"),
        ZANIK("E"),
        VZTAH("R");

        private String classType;

        ClassType(final String classType) {
            this.classType = classType;
        }

        public String getClassType() {
            return classType;
        }
    }

    @Id
    @GeneratedValue
    private Integer relationTypeId;

    @Column(length = 250, nullable = false)
    private String name;

    @Column(length = 50, nullable = false, unique = true)
    private String code;

    @Column(length = 50)
    private String classType;


    @Override
    public Integer getRelationTypeId() {
        return relationTypeId;
    }

    @Override
    public void setRelationTypeId(final Integer relationTypeId) {
        this.relationTypeId = relationTypeId;
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
    public String getClassType() {
        return classType;
    }

    @Override
    public void setClassType(final String classType) {
        this.classType = classType;
    }

    @Override
    public boolean equals(final Object obj) {
        if (!(obj instanceof cz.tacr.elza.api.ParRelationType)) {
            return false;
        }
        if (this == obj) {
            return true;
        }

        ParRelationType other = (ParRelationType) obj;

        return new EqualsBuilder().append(relationTypeId, other.getRelationTypeId()).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(relationTypeId).toHashCode();
    }

    @Override
    public String toString() {
        return "ParRelationType pk=" + relationTypeId;
    }

}
