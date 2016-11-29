package cz.tacr.elza.domain;

import javax.persistence.Column;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

import cz.tacr.elza.api.InterpiClass;
import cz.tacr.elza.domain.enumeration.StringLength;

/**
 *
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 29. 11. 2016
 */
public class ParInterpiRelation implements cz.tacr.elza.api.ParInterpiRelation<ParRelationType> {

    @Id
    @GeneratedValue
    private Integer interpiRelationId;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ParRelationType.class)
    @JoinColumn(name = "relationTypeId", nullable = false)
    private ParRelationType relationType;

    @Enumerated(EnumType.STRING)
    @Column(name = "class", length = StringLength.LENGTH_20, nullable = false)
    private InterpiClass cls;

    @Column(length = StringLength.LENGTH_250, nullable = false)
    private String name;

    @Override
    public Integer getInterpiRelationId() {
        return interpiRelationId;
    }

    @Override
    public void setInterpiRelationId(final Integer interpiRelationId) {
        this.interpiRelationId = interpiRelationId;
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
    public InterpiClass getCls() {
        return cls;
    }

    @Override
    public void setCls(final InterpiClass cls) {
        this.cls = cls;
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
        if (!(obj instanceof ParInterpiRelation)) {
            return false;
        }
        if (this == obj) {
            return true;
        }

        ParInterpiRelation other = (ParInterpiRelation) obj;

        return new EqualsBuilder().append(interpiRelationId, other.getInterpiRelationId()).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(interpiRelationId).toHashCode();
    }

    @Override
    public String toString() {
        return "ParInterpiRelation pk=" + interpiRelationId;
    }
}
