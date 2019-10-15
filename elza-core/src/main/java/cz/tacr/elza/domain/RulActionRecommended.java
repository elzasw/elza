package cz.tacr.elza.domain;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Doporučení hromadné akce pro typ výstupu.
 *
 * @author Martin Šlapa
 * @since 27.06.2016
 */
@Entity(name = "rul_action_recommended")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class RulActionRecommended {

    @Id
    @GeneratedValue
    @Access(AccessType.PROPERTY) // required to read id without fetch from db
    private Integer actionRecommendedId;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = RulAction.class)
    @JoinColumn(name = "actionId", nullable = false)
    private RulAction action;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = RulOutputType.class)
    @JoinColumn(name = "outputTypeId", nullable = false)
    private RulOutputType outputType;

    /**
     * @return identifikátor entity
     */
    public Integer getActionRecommendedId() {
        return actionRecommendedId;
    }

    /**
     * @param actionRecommendedId identifikátor entity
     */
    public void setActionRecommendedId(final Integer actionRecommendedId) {
        this.actionRecommendedId = actionRecommendedId;
    }

    /**
     * @return hromadná akce
     */
    public RulAction getAction() {
        return action;
    }

    /**
     * @param action hromadná akce
     */
    public void setAction(final RulAction action) {
        this.action = action;
    }

    /**
     * @return typ výstupu
     */
    public RulOutputType getOutputType() {
        return outputType;
    }

    /**
     * @param outputType typ výstupu
     */
    public void setOutputType(final RulOutputType outputType) {
        this.outputType = outputType;
    }
}
