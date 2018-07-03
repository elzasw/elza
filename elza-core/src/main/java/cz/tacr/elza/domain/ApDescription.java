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

@Entity(name = "ap_description")
@Table
@Inheritance(strategy = InheritanceType.JOINED)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ApDescription implements Serializable {
    @Id
    @GeneratedValue
    @Access(AccessType.PROPERTY)
    private Integer descriptionId;

    @Column(length = StringLength.LENGTH_1000, nullable = false)
    @JsonIgnore
    private String description;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ApAccessPoint.class)
    @JoinColumn(name = "accessPointId", nullable = false)
    @JsonIgnore
    private ApAccessPoint accessPoint;

    @Column(nullable = false, updatable = false, insertable = false)
    private Integer accessPointId;


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
    public static final String DESCRIPTION_ID = "descriptionId";
    public static final String DESCRIPTION = "description";
    public static final String ACCESS_POINT_ID = "accessPointId";
    public static final String ACCESS_POINT = "accessPoint";
    public static final String DELETE_CHANGE = "deleteChange";
    public static final String DELETE_CHANGE_ID = "deleteChangeId";

    public ApDescription() {}

    public ApDescription(ApDescription other) {
        this.descriptionId = other.descriptionId;
        this.description = other.description;
        this.accessPoint = other.accessPoint;
        this.accessPointId = other.accessPointId;
        this.createChange = other.createChange;
        this.createChangeId = other.createChangeId;
        this.deleteChange = other.deleteChange;
        this.deleteChangeId = other.deleteChangeId;
    }

    public Integer getDescriptionId() {
        return descriptionId;
    }

    public void setDescriptionId(Integer descriptionId) {
        this.descriptionId = descriptionId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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

    @Override
    public boolean equals(final Object obj) {
        if (!(obj instanceof cz.tacr.elza.domain.ApDescription)) {
            return false;
        }
        if (this == obj) {
            return true;
        }

        cz.tacr.elza.domain.ApDescription other = (cz.tacr.elza.domain.ApDescription) obj;

        return new EqualsBuilder().append(descriptionId, other.getDescriptionId()).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(descriptionId).toHashCode();
    }

    @Override
    public String toString() {
        return "ApDescription pk=" + descriptionId;
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
