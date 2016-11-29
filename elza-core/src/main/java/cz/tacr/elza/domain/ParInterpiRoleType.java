package cz.tacr.elza.domain;

import javax.persistence.Column;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

import cz.tacr.elza.domain.enumeration.StringLength;

/**
 *
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 29. 11. 2016
 */
public class ParInterpiRoleType implements cz.tacr.elza.api.ParInterpiRoleType<ParInterpiRelation, ParRelationRoleType> {

    @Id
    @GeneratedValue
    private Integer interpiRoleTypeId;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ParInterpiRelation.class)
    @JoinColumn(name = "interpiRelationId", nullable = false)
    private ParInterpiRelation interpiRelation;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ParRelationRoleType.class)
    @JoinColumn(name = "relationRoleTypeId", nullable = false)
    private ParRelationRoleType relationRoleType;

    @Column(length = StringLength.LENGTH_250, nullable = false)
    private String name;

    @Override
	public Integer getInterpiRoleTypeId() {
        return interpiRoleTypeId;
    }

    @Override
	public void setInterpiRoleTypeId(final Integer interpiRoleTypeId) {
        this.interpiRoleTypeId = interpiRoleTypeId;
    }

    @Override
	public ParInterpiRelation getInterpiRelation() {
        return interpiRelation;
    }

    @Override
	public void setInterpiRelation(final ParInterpiRelation interpiRelation) {
        this.interpiRelation = interpiRelation;
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
	public String getName() {
        return name;
    }

    @Override
	public void setName(final String name) {
        this.name = name;
    }

    @Override
    public boolean equals(final Object obj) {
        if (!(obj instanceof ParInterpiRoleType)) {
            return false;
        }
        if (this == obj) {
            return true;
        }

        ParInterpiRoleType other = (ParInterpiRoleType) obj;

        return new EqualsBuilder().append(interpiRoleTypeId, other.getInterpiRoleTypeId()).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(interpiRoleTypeId).toHashCode();
    }

    @Override
    public String toString() {
        return "ParInterpiRoleType pk=" + interpiRoleTypeId;
    }
}
