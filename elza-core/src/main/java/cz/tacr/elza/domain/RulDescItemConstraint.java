package cz.tacr.elza.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

import javax.persistence.*;


/**
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

    public Integer getDescItemConstraintId() {
        return descItemConstraintId;
    }

    public void setDescItemConstraintId(final Integer descItemConstraintId) {
        this.descItemConstraintId = descItemConstraintId;
    }

    public RulDescItemType getDescItemType() {
        return descItemType;
    }

    public void setDescItemType(final RulDescItemType descItemType) {
        this.descItemType = descItemType;
    }

    public RulDescItemSpec getDescItemSpec() {
        return descItemSpec;
    }

    public void setDescItemSpec(final RulDescItemSpec descItemSpec) {
        this.descItemSpec = descItemSpec;
    }

    public ArrFaVersion getVersion() {
        return version;
    }

    public void setVersion(final ArrFaVersion version) {
        this.version = version;
    }

    public Boolean getRepeatable() {
        return repeatable;
    }

    public void setRepeatable(final Boolean repeatable) {
        this.repeatable = repeatable;
    }

    public String getRegexp() {
        return regexp;
    }

    public void setRegexp(final String regexp) {
        this.regexp = regexp;
    }

    public Integer getTextLenghtLimit() {
        return textLenghtLimit;
    }

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
