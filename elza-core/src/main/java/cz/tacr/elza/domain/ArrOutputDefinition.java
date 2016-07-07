package cz.tacr.elza.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Implementace třídy {@link cz.tacr.elza.api.ArrOutputDefinition}
 *
 * @author Martin Šlapa
 * @since 01.04.2016
 */
@Entity(name = "arr_output_definition")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "id"})
public class ArrOutputDefinition extends AbstractVersionableEntity implements cz.tacr.elza.api.ArrOutputDefinition<ArrFund, ArrNodeOutput, ArrOutput, RulOutputType, RulTemplate, ArrOutputResult> {

    @Id
    @GeneratedValue
    private Integer outputDefinitionId;

    @Column(nullable = true)
    private LocalDateTime lastUpdate;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ArrFund.class)
    @JoinColumn(name = "fundId", nullable = false)
    private ArrFund fund;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = RulOutputType.class)
    @JoinColumn(name = "outputTypeId", nullable = false)
    private RulOutputType outputType;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = RulTemplate.class)
    @JoinColumn(name = "templateId")
    private RulTemplate template;

    @Column(length = 250)
    private String internalCode;

    @Column(length = 250, nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(length = 12, nullable = false)
    private OutputState state;

    @Column(length = 10000)
    private String error;

    @Column(nullable = false)
    private Boolean temporary;

    @Column(nullable = false)
    private Boolean deleted;

    @OneToMany(mappedBy = "outputDefinition", fetch = FetchType.LAZY)
    private List<ArrOutput> outputs;

    @OneToMany(mappedBy = "outputDefinition", fetch = FetchType.LAZY)
    private List<ArrNodeOutput> outputNodes;

    @OneToOne(mappedBy = "outputDefinition", fetch = FetchType.LAZY)
    private ArrOutputResult outputResult;

    @Override
    public Integer getOutputDefinitionId() {
        return outputDefinitionId;
    }

    @Override
    public void setOutputDefinitionId(final Integer outputDefinitionId) {
        this.outputDefinitionId = outputDefinitionId;
    }

    @Override
    public LocalDateTime getLastUpdate() {
        return lastUpdate;
    }

    @Override
    public void setLastUpdate(final LocalDateTime lastUpdate) {
        this.lastUpdate = lastUpdate;
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
    public OutputState getState() {
        return state;
    }

    @Override
    public void setState(OutputState state) {
        this.state = state;
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

    @Override
    public RulTemplate getTemplate() {
        return template;
    }

    @Override
    public void setTemplate(RulTemplate template) {
        this.template = template;
    }

    @Override
    public ArrOutputResult getOutputResult() {
        return outputResult;
    }

    @Override
    public void setOutputResult(ArrOutputResult outputResult) {
        this.outputResult = outputResult;
    }

    @Override
    public String getError() {
        return error;
    }

    @Override
    public void setError(String error) {
        this.error = error;
    }
}
