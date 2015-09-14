package cz.tacr.elza.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;


/**
 * Entita umožňující limitovat hodnoty typu atributu nebo podtypu.
 *
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 20.8.2015
 */
@Entity(name = "rul_desc_item_constraint")
@Table
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class RulDescItemConstraint implements cz.tacr.elza.api.RulDescItemConstraint<RulDescItemType, RulDescItemSpec, ArrFaVersion> {

    @Id
    @GeneratedValue
    private Integer descItemConstraintId;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = RulDescItemType.class)
    @JoinColumn(name = "descItemTypeId", nullable = false)
    private RulDescItemType descItemType;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = RulDescItemSpec.class)
    @JoinColumn(name = "descItemSpecId", nullable = true)
    private RulDescItemSpec descItemSpec;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ArrFaVersion.class)
    @JoinColumn(name = "faVersionId", nullable = true)
    private ArrFaVersion version;

    @Column(nullable = true)
    private Boolean repeatable;

    @Column(length = 250, nullable = true)
    private String regexp;

    @Column(nullable = true)
    private Integer textLenghtLimit;

    @Override
    public Integer getDescItemConstraintId() {
        return descItemConstraintId;
    }

    @Override
    public void setDescItemConstraintId(final Integer descItemConstraintId) {
        this.descItemConstraintId = descItemConstraintId;
    }

    @Override
    public RulDescItemType getDescItemType() {
        return descItemType;
    }

    @Override
    public void setDescItemType(final RulDescItemType descItemType) {
        this.descItemType = descItemType;
    }

    @Override
    public RulDescItemSpec getDescItemSpec() {
        return descItemSpec;
    }

    @Override
    public void setDescItemSpec(final RulDescItemSpec descItemSpec) {
        this.descItemSpec = descItemSpec;
    }

    @Override
    public ArrFaVersion getVersion() {
        return version;
    }

    @Override
    public void setVersion(final ArrFaVersion version) {
        this.version = version;
    }

    @Override
    public Boolean getRepeatable() {
        return repeatable;
    }

    @Override
    public void setRepeatable(final Boolean repeatable) {
        this.repeatable = repeatable;
    }

    @Override
    public String getRegexp() {
        return regexp;
    }

    @Override
    public void setRegexp(final String regexp) {
        this.regexp = regexp;
    }

    @Override
    public Integer getTextLenghtLimit() {
        return textLenghtLimit;
    }

    @Override
    public void setTextLenghtLimit(final Integer textLenghtLimit) {
        this.textLenghtLimit = textLenghtLimit;
    }

    @Override
    public boolean equals(final Object obj) {
        if (!(obj instanceof RulDescItemConstraint)) {
            return false;
        }
        if (this == obj) {
            return true;
        }

        RulDescItemConstraint other = (RulDescItemConstraint) obj;

        return new EqualsBuilder().append(descItemConstraintId, other.getDescItemConstraintId()).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(descItemConstraintId).toHashCode();
    }
}
