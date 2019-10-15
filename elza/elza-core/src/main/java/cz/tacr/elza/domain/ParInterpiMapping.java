package cz.tacr.elza.domain;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import cz.tacr.elza.api.enums.InterpiClass;
import cz.tacr.elza.domain.enumeration.StringLength;

/**
 * Mapování vztahů mezi INTERPI a ELZA.
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 30. 11. 2016
 */
@Entity(name = "par_interpi_mapping")
@Cache(region = "domain", usage = CacheConcurrencyStrategy.READ_WRITE)
@Table
public class ParInterpiMapping {

    @Id
    @GeneratedValue
    @Access(AccessType.PROPERTY) // required to read id without fetch from db
    private Integer interpiMappingId;

    /** Typ vztahu ELZA. */
    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ParRelationRoleType.class)
    @JoinColumn(name = "relationRoleTypeId")
    private ParRelationRoleType relationRoleType;

    /** Typ role entity ELZA. */
    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ParRelationType.class)
    @JoinColumn(name = "relationTypeId", nullable = false)
    private ParRelationType relationType;

    @Enumerated(EnumType.STRING)
    @Column(length = StringLength.LENGTH_20, nullable = false)
    private InterpiClass interpiClass;

    /** Typ vztahu INTERPI. */
    @Column(length = StringLength.LENGTH_250)
    private String interpiRelationType;

    /** Typ role INTERPI. */
    @Column(length = StringLength.LENGTH_250)
    private String interpiRoleType;

    public Integer getInterpiMappingId() {
        return interpiMappingId;
    }

    public void setInterpiMappingId(final Integer interpiMappingId) {
        this.interpiMappingId = interpiMappingId;
    }

    public ParRelationRoleType getRelationRoleType() {
        return relationRoleType;
    }

    public void setRelationRoleType(final ParRelationRoleType relationRoleType) {
        this.relationRoleType = relationRoleType;
    }

    public ParRelationType getRelationType() {
        return relationType;
    }

    public void setRelationType(final ParRelationType relationType) {
        this.relationType = relationType;
    }

    public InterpiClass getInterpiClass() {
        return interpiClass;
    }

    public void setInterpiClass(final InterpiClass interpiClass) {
        this.interpiClass = interpiClass;
    }

    public String getInterpiRelationType() {
        return interpiRelationType;
    }

    public void setInterpiRelationType(final String interpiRelationType) {
        this.interpiRelationType = interpiRelationType;
    }

    public String getInterpiRoleType() {
        return interpiRoleType;
    }

    public void setInterpiRoleType(final String interpiRoleType) {
        this.interpiRoleType = interpiRoleType;
    }


    @Override
    public boolean equals(final Object obj) {
        if (!(obj instanceof ParInterpiMapping)) {
            return false;
        }
        if (this == obj) {
            return true;
        }

        ParInterpiMapping other = (ParInterpiMapping) obj;

        return new EqualsBuilder().append(interpiMappingId, other.getInterpiMappingId()).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(interpiMappingId).toHashCode();
    }

    @Override
    public String toString() {
        return "ParInterpiMapping pk=" + interpiMappingId;
    }
}
