package cz.tacr.elza.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import cz.tacr.elza.domain.enumeration.StringLength;
import cz.tacr.elza.domain.interfaces.Versionable;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.Table;
import java.io.Serializable;

@Entity(name = "ap_name_type")
@Table
@Inheritance(strategy = InheritanceType.JOINED)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ApNameType implements Serializable {
    @Id
    @GeneratedValue
    @Access(AccessType.PROPERTY)
    private Integer nameTypeId;

    @Column(length = StringLength.LENGTH_50, nullable = false, unique = true)
    @JsonIgnore
    private String code;

    @Column(length = StringLength.LENGTH_250)
    @JsonIgnore
    private String name;

    /* Konstanty pro vazby a fieldy. */
    public static final String NAME_TYPE_ID = "nameTypeId";
    public static final String NAME = "name";
    public static final String CODE = "code";


    public Integer getNameTypeId() {
        return nameTypeId;
    }

    public void setNameTypeId(Integer nameTypeId) {
        this.nameTypeId = nameTypeId;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public boolean equals(final Object obj) {
        if (!(obj instanceof cz.tacr.elza.domain.ApNameType)) {
            return false;
        }
        if (this == obj) {
            return true;
        }

        cz.tacr.elza.domain.ApNameType other = (cz.tacr.elza.domain.ApNameType) obj;

        return new EqualsBuilder().append(nameTypeId, other.getNameTypeId()).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(nameTypeId).toHashCode();
    }

    @Override
    public String toString() {
        return "ApNameType pk=" + nameTypeId;
    }
}
