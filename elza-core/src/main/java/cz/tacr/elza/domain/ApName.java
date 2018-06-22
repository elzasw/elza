package cz.tacr.elza.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import cz.tacr.elza.domain.enumeration.StringLength;
import cz.tacr.elza.domain.interfaces.Versionable;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.springframework.data.rest.core.annotation.RestResource;

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
import javax.persistence.Table;
import javax.persistence.metamodel.SingularAttribute;

import java.io.Serializable;

@Entity(name = "ap_name")
@Table
@Inheritance(strategy = InheritanceType.JOINED)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ApName implements Serializable {

    @Id
    @GeneratedValue
    @Access(AccessType.PROPERTY)
    private Integer nameId;

    @Column(length = StringLength.LENGTH_1000)
    @JsonIgnore
    private String name;

    @Column(length = StringLength.LENGTH_1000)
    @JsonIgnore
    private String complement;

    @Column(length = 3)
    @JsonIgnore
    private String language;

    @Column(nullable = false)
    @JsonIgnore
    private Boolean preferredName;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ApAccessPoint.class)
    @JoinColumn(name = "accessPointId", nullable = false)
    @JsonIgnore
    private ApAccessPoint accessPoint;

    @Column(nullable = false, updatable = false, insertable = false)
    private Integer accessPointId;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ApAccessPoint.class)
    @JoinColumn(name = "nameTypeId", nullable = false)
    @JsonIgnore
    private ApNameType nameType;

    @Column(nullable = false, updatable = false, insertable = false)
    private Integer nameTypeId;


    @RestResource(exported = false)
    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ApChange.class)
    @JoinColumn(name = "createChangeId", nullable = false)
    protected ApChange createChange;

    @Column(name = "createChangeId", nullable = false, updatable = false, insertable = false)
    protected Integer createChangeId;

    @RestResource(exported = false)
    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ApChange.class)
    @JoinColumn(name = "deleteChangeId", nullable = true)
    protected ApChange deleteChange;

    @Column(name = "deleteChangeId", nullable = true, updatable = false, insertable = false)
    protected Integer deleteChangeId;


    /* Konstanty pro vazby a fieldy. */
    public static final String NAME_ID = "nameId";
    public static final String NAME = "name";
    public static final String COMPLEMENT = "complement";
    public static final String LANGUAGE = "language";
    public static final String PREFERRED_NAME = "preferredName";
    public static final String ACCESS_POINT_ID = "accessPointId";
    public static final String NAME_TYPE_ID = "nameTypeId";
    public static final String DELETE_CHANGE_ID = "deleteChangeId";

    public ApName(){}

    public ApName(ApName other) {
        this.nameId = other.nameId;
        this.name = other.name;
        this.complement = other.complement;
        this.language = other.language;
        this.preferredName = other.preferredName;
        this.accessPoint = other.accessPoint;
        this.accessPointId = other.accessPointId;
        this.nameType = other.nameType;
        this.nameTypeId = other.nameTypeId;
        this.createChange = other.createChange;
        this.createChangeId = other.createChangeId;
        this.deleteChange = other.deleteChange;
        this.deleteChangeId = other.deleteChangeId;
    }

    public Integer getNameId() {
        return nameId;
    }

    public void setNameId(Integer nameId) {
        this.nameId = nameId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getComplement() {
        return complement;
    }

    public void setComplement(String complement) {
        this.complement = complement;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public Boolean getPreferredName() {
        return preferredName;
    }

    public void setPreferredName(Boolean preferredName) {
        this.preferredName = preferredName;
    }

    public ApAccessPoint getAccessPoint() {
        return accessPoint;
    }

    public void setAccessPoint(ApAccessPoint accessPoint) {
        this.accessPoint = accessPoint;
    }

    public Integer getAccessPointId() {
        return accessPointId;
    }

    public void setAccessPointId(Integer accessPointId) {
        this.accessPointId = accessPointId;
    }

    public ApNameType getNameType() {
        return nameType;
    }

    public void setNameType(ApNameType nameType) {
        this.nameType = nameType;
    }

    public Integer getNameTypeId() {
        return nameTypeId;
    }

    public void setNameTypeId(Integer nameTypeId) {
        this.nameTypeId = nameTypeId;
    }

    @Override
    public boolean equals(final Object obj) {
        if (!(obj instanceof cz.tacr.elza.domain.ApName)) {
            return false;
        }
        if (this == obj) {
            return true;
        }

        cz.tacr.elza.domain.ApName other = (cz.tacr.elza.domain.ApName) obj;

        return new EqualsBuilder().append(nameId, other.getNameId()).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(nameId).toHashCode();
    }

    @Override
    public String toString() {
        return "ApName pk=" + nameId;
    }

    public ApChange getCreateChange() {
        return createChange;
    }

    public void setCreateChange(ApChange createChange) {
        this.createChange = createChange;
    }

    public ApChange getDeleteChange() {
        return deleteChange;
    }

    public void setDeleteChange(ApChange deleteChange) {
        this.deleteChange = deleteChange;
    }
}
