package cz.tacr.elza.domain;

import java.util.Arrays;

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
import org.springframework.data.rest.core.annotation.RestResource;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;


/**
 * popis {@link cz.tacr.elza.api.RulFaView}.
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 20.8.2015
 */
@Entity(name = "rul_fa_view")
@Table
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class RulFaView extends AbstractVersionableEntity implements cz.tacr.elza.api.RulFaView<RulArrangementType, RulRuleSet> {

    @Id
    @GeneratedValue
    private Integer faViewId;

    @RestResource(exported = false)
    @ManyToOne(fetch = FetchType.LAZY, targetEntity = RulArrangementType.class)
    @JoinColumn(name = "arrangementTypeId", nullable = false)
    private RulArrangementType arrangementType;

    @RestResource(exported = false)
    @ManyToOne(fetch = FetchType.LAZY, targetEntity = RulRuleSet.class)
    @JoinColumn(name = "ruleSetId", nullable = false)
    private RulRuleSet ruleSet;

    @Column(length = 1000, nullable = false)
    private String viewSpecification;

    @Override
    public Integer getFaViewId() {
        return faViewId;
    }

    @Override
    public void setFaViewId(final Integer faViewId) {
        this.faViewId = faViewId;
    }

    @Override
    public RulArrangementType getArrangementType() {
        return arrangementType;
    }

    @Override
    public void setArrangementType(final RulArrangementType arrangementType) {
        this.arrangementType = arrangementType;
    }

    @Override
    public RulRuleSet getRuleSet() {
        return ruleSet;
    }

    @Override
    public void setRuleSet(final RulRuleSet ruleSet) {
        this.ruleSet = ruleSet;
    }

    @Override
    public String getViewSpecification() {
        return viewSpecification;
    }

    @Override
    public void setViewSpecification(final String viewSpecification) {
        this.viewSpecification = viewSpecification;
    }

    @Override
    public boolean equals(final Object obj) {
        if (!(obj instanceof RulFaView)) {
            return false;
        }
        if (this == obj) {
            return true;
        }

        RulFaView other = (RulFaView) obj;

        return new EqualsBuilder().append(faViewId, other.getFaViewId()).isEquals();
    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this, Arrays.asList("arrangementType", "ruleSet"));
    }

    @Override
    public String toString() {
        return "RulFaView pk=" + faViewId;
    }
}
