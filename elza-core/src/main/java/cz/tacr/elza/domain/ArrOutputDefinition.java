package cz.tacr.elza.domain;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import javax.persistence.*;

/**
 * Implementace třídy {@link cz.tacr.elza.api.ArrOutputDefinition}
 *
 * @author Martin Šlapa
 * @since 01.04.2016
 */
@Entity(name = "arr_output_definition")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "id"})
public class ArrOutputDefinition implements cz.tacr.elza.api.ArrOutputDefinition<ArrFund, ArrNodeOutput, ArrOutput, RulOutputType> {

    @Id
    @GeneratedValue
    private Integer outputDefinitionId;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ArrFund.class)
    @JoinColumn(name = "fundId", nullable = false)
    private ArrFund fund;

    @Column(length = 250)
    private String internalCode;

    @Column(length = 250, nullable = false)
    private String name;

    @Column(nullable = false)
    private Boolean temporary;

    @Column(nullable = false)
    private Boolean deleted;

    @OneToMany(mappedBy = "outputDefinition", fetch = FetchType.LAZY)
    private List<ArrOutput> outputs;

    @OneToMany(mappedBy = "outputDefinition", fetch = FetchType.LAZY)
    private List<ArrNodeOutput> outputNodes;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = RulOutputType.class)
    @JoinColumn(name = "outputTypeId", nullable = false)
    private RulOutputType outputType;

    @Override
    public Integer getOutputDefinitionId() {
        return outputDefinitionId;
    }

    @Override
    public void setOutputDefinitionId(final Integer outputDefinitionId) {
        this.outputDefinitionId = outputDefinitionId;
    }

    @Override
    public ArrFund getFund() {
        return fund;
    }

    @Override
    public void setFund(final ArrFund fund) {
        this.fund = fund;
    }

    @Override
    public String getInternalCode() {
        return internalCode;
    }

    @Override
    public void setInternalCode(final String internalCode) {
        this.internalCode = internalCode;
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
    public Boolean getTemporary() {
        return temporary;
    }

    @Override
    public void setTemporary(final Boolean temporary) {
        this.temporary = temporary;
    }

    @Override
    public Boolean getDeleted() {
        return deleted;
    }

    @Override
    public void setDeleted(final Boolean deleted) {
        this.deleted = deleted;
    }

    @Override
    public List<ArrOutput> getOutputs() {
        return outputs;
    }

    @Override
    public void setOutputs(final List<ArrOutput> outputs) {
        this.outputs = outputs;
    }

    @Override
    public List<ArrNodeOutput> getOutputNodes() {
        return outputNodes;
    }

    @Override
    public void setOutputNodes(final List<ArrNodeOutput> outputNodes) {
        this.outputNodes = outputNodes;
    }

    @Override
    public RulOutputType getOutputType() {
        return outputType;
    }

    @Override
    public void setOutputType(RulOutputType outputType) {
        this.outputType = outputType;
    }
}
