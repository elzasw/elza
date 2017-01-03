package cz.tacr.elza.domain;

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

import cz.tacr.elza.api.InterpiClass;
import cz.tacr.elza.domain.enumeration.StringLength;

/**
 * Mapování vztahů mezi INTERPI a ELZA.
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 30. 11. 2016
 */
@Entity(name = "par_interpi_mapping")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@Table
public class ParInterpiMapping implements cz.tacr.elza.api.ParInterpiMapping<ParRelationRoleType, ParRelationType> {

    @Id
    @GeneratedValue
    private Integer interpiMappingId;

    /** Typ vztahu ELZA. */
    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ParRelationRoleType.class)
    @JoinColumn(name = "relationRoleTypeId", nullable = false)
    private ParRelationRoleType relationRoleType;

    /** Typ role entity ELZA. */
    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ParRelationType.class)
    @JoinColumn(name = "relationTypeId", nullable = false)
    private ParRelationType relationType;

    @Enumerated(EnumType.STRING)
    @Column(length = StringLength.LENGTH_20, nullable = false)
    private InterpiClass interpiClass;

    /** Typ vztahu INTERPI. */
    @Column(length = StringLength.LENGTH_250, nullable = true)
    private String interpiRelationType;

    /** Typ role INTERPI. */
    @Column(length = StringLength.LENGTH_250, nullable = true)
    private String interpiRoleType;

    @Override
    public Integer getInterpiMappingId() {
        return interpiMappingId;
    }

    @Override
    public void setInterpiMappingId(final Integer interpiMappingId) {
        this.interpiMappingId = interpiMappingId;
    }

    @Override
    public ParRelationRoleType getRelationRoleType() {
        return relationRoleType;
    }

    @Override
    public void setRelationRoleType(final ParRelationRoleType relationRoleType) {
        this.relationRoleType = relationRoleType;
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
    public InterpiClass getInterpiClass() {
        return interpiClass;
    }

    @Override
    public void setInterpiClass(final InterpiClass interpiClass) {
        this.interpiClass = interpiClass;
    }

    @Override
    public String getInterpiRelationType() {
        return interpiRelationType;
    }

    @Override
    public void setInterpiRelationType(final String interpiRelationType) {
        this.interpiRelationType = interpiRelationType;
    }

    @Override
    public String getInterpiRoleType() {
        return interpiRoleType;
    }

    @Override
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
