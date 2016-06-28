package cz.tacr.elza.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

/**
 * Implementace {@link cz.tacr.elza.api.RulActionRecommended}
 *
 * @author Martin Šlapa
 * @since 27.06.2016
 */
@Entity(name = "rul_action_recommended")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class RulActionRecommended implements cz.tacr.elza.api.RulActionRecommended<RulAction, RulOutputType> {

    @Id
    @GeneratedValue
    private Integer actionRecommendedId;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = RulAction.class)
    @JoinColumn(name = "actionId", nullable = false)
    private RulAction action;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = RulOutputType.class)
    @JoinColumn(name = "outputTypeId", nullable = false)
    private RulOutputType outputType;

    @Override
    public Integer getActionRecommendedId() {
        return actionRecommendedId;
    }

    @Override
    public void setActionRecommendedId(final Integer actionRecommendedId) {
        this.actionRecommendedId = actionRecommendedId;
    }

    @Override
    public RulAction getAction() {
        return action;
    }

    @Override
    public void setAction(final RulAction action) {
        this.action = action;
    }

    @Override
    public RulOutputType getOutputType() {
        return outputType;
    }

    @Override
    public void setOutputType(final RulOutputType outputType) {
        this.outputType = outputType;
    }
}
