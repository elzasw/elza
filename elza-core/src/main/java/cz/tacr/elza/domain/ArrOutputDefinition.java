package cz.tacr.elza.domain;

import java.time.LocalDateTime;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Implementace třídy {@link cz.tacr.elza.api.ArrOutputDefinition}
 *
 * @author Martin Šlapa
 * @since 01.04.2016
 */
@Entity(name = "arr_output_definition")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "id"})
public class ArrOutputDefinition extends AbstractVersionableEntity {

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

    public Integer getOutputDefinitionId() {
        return outputDefinitionId;
    }

    public void setOutputDefinitionId(final Integer outputDefinitionId) {
        this.outputDefinitionId = outputDefinitionId;
    }

    public LocalDateTime getLastUpdate() {
        return lastUpdate;
    }

    public void setLastUpdate(final LocalDateTime lastUpdate) {
        this.lastUpdate = lastUpdate;
    }

    public ArrFund getFund() {
        return fund;
    }

    public void setFund(final ArrFund fund) {
        this.fund = fund;
    }

    public String getInternalCode() {
        return internalCode;
    }

    public void setInternalCode(final String internalCode) {
        this.internalCode = internalCode;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public OutputState getState() {
        return state;
    }

    public void setState(final OutputState state) {
        this.state = state;
    }

    public Boolean getTemporary() {
        return temporary;
    }

    public void setTemporary(final Boolean temporary) {
        this.temporary = temporary;
    }

    public Boolean getDeleted() {
        return deleted;
    }

    public void setDeleted(final Boolean deleted) {
        this.deleted = deleted;
    }

    public List<ArrOutput> getOutputs() {
        return outputs;
    }

    public void setOutputs(final List<ArrOutput> outputs) {
        this.outputs = outputs;
    }

    public List<ArrNodeOutput> getOutputNodes() {
        return outputNodes;
    }

    public void setOutputNodes(final List<ArrNodeOutput> outputNodes) {
        this.outputNodes = outputNodes;
    }

    public RulOutputType getOutputType() {
        return outputType;
    }

    public void setOutputType(final RulOutputType outputType) {
        this.outputType = outputType;
    }

    public RulTemplate getTemplate() {
        return template;
    }

    public void setTemplate(final RulTemplate template) {
        this.template = template;
    }

    public ArrOutputResult getOutputResult() {
        return outputResult;
    }

    public void setOutputResult(final ArrOutputResult outputResult) {
        this.outputResult = outputResult;
    }

    public String getError() {
        return error;
    }

    public void setError(final String error) {
        this.error = error;
    }

    /**
     * Stav outputu
     */
    public enum OutputState {
        /**
         * Rozpracovaný
         */
        OPEN,
        /**
         * Běží hromadná akce
         */
        COMPUTING,
        /**
         * Generování
         */
        GENERATING,
        /**
         * Vygenerovaný
         */
        FINISHED,
        /**
         * Vygenerovaný neaktuální
         */
        OUTDATED
    }
}
