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
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;


/**
 * Entita umožňující limitovat hodnoty typu atributu nebo podtypu.
 *
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 20.8.2015
 */
@Entity(name = "rul_desc_item_constraint")
@Table
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class RulDescItemConstraint implements cz.tacr.elza.api.RulDescItemConstraint<RulDescItemType, RulDescItemSpec,
        ArrFundVersion, RulPackage> {

    @Id
    @GeneratedValue
    private Integer descItemConstraintId;

    @Column(length = 50, nullable = false)
    private String code;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = RulDescItemType.class)
    @JoinColumn(name = "descItemTypeId", nullable = false)
    private RulDescItemType descItemType;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = RulDescItemSpec.class)
    @JoinColumn(name = "descItemSpecId", nullable = true)
    private RulDescItemSpec descItemSpec;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ArrFundVersion.class)
    @JoinColumn(name = "fundVersionId", nullable = true)
    private ArrFundVersion fundVersion;

    @Column(nullable = true)
    private Boolean repeatable;

    @Column(length = 250, nullable = true)
    private String regexp;

    @Column(nullable = true)
    private Integer textLenghtLimit;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = RulPackage.class)
    @JoinColumn(name = "packageId", nullable = false)
    private RulPackage rulPackage;

    @Override
    public Integer getDescItemConstraintId() {
        return descItemConstraintId;
    }

    @Override
    public void setDescItemConstraintId(final Integer descItemConstraintId) {
        this.descItemConstraintId = descItemConstraintId;
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
    public ArrFundVersion getFundVersion() {
        return fundVersion;
    }

    @Override
    public void setFundVersion(final ArrFundVersion fundVersion) {
        this.fundVersion = fundVersion;
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
    public RulPackage getPackage() {
        return rulPackage;
    }

    @Override
    public void setPackage(final RulPackage rulPackage) {
        this.rulPackage = rulPackage;
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

    @Override
    public String toString() {
        return "RulDescItemConstraint pk=" + descItemConstraintId;
    }
}
