package cz.tacr.elza.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import javax.persistence.*;

/**
 * Implementace třídy {@link cz.tacr.elza.api.ArrOutputResult}
 *
 * @author Petr Compel <petr.compel@marbes.cz>
 * @since 20.6.2016
 */
@Entity(name = "arr_output_result")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ArrOutputResult implements cz.tacr.elza.api.ArrOutputResult<ArrOutputDefinition, RulTemplate, ArrChange> {

    @Id
    @GeneratedValue
    private Integer outputResultId;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ArrOutputDefinition.class)
    @JoinColumn(name = "outputDefinitionId", nullable = false)
    private ArrOutputDefinition outputDefinition;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = RulTemplate.class)
    @JoinColumn(name = "templateId", nullable = false)
    private RulTemplate template;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ArrChange.class)
    @JoinColumn(name = "changeId", nullable = false)
    private ArrChange change;

    @Override
    public Integer getOutputResultId() {
        return outputResultId;
    }

    public void setOutputResultId(final Integer outputResultId) {
        this.outputResultId = outputResultId;
    }

    @Override
    public ArrOutputDefinition getOutputDefinition() {
        return outputDefinition;
    }

    @Override
    public void setOutputDefinition(ArrOutputDefinition outputDefinition) {
        this.outputDefinition = outputDefinition;
    }


    @Override
    public RulTemplate getTemplate() {
        return template;
    }

    @Override
    public void setTemplate(RulTemplate template) {
        this.template = template;
    }


    @Override
    public ArrChange getChange() {
        return change;
    }

    @Override
    public void setChange(ArrChange change) {
        this.change = change;
    }

}
