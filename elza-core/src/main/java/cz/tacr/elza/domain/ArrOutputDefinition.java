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
 * Výstup z archivního souboru.
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

    @Column(nullable = false, insertable = false, updatable = false)
    private Integer fundId;

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

    @Column(nullable = true)
    private String outputSettings;

    @OneToMany(mappedBy = "outputDefinition", fetch = FetchType.LAZY)
    private List<ArrOutput> outputs;

    @OneToMany(mappedBy = "outputDefinition", fetch = FetchType.LAZY)
    private List<ArrNodeOutput> outputNodes;

    @OneToOne(mappedBy = "outputDefinition", fetch = FetchType.LAZY)
    private ArrOutputResult outputResult;

    /**
     * @return identifikátor entity
     */
    public Integer getOutputDefinitionId() {
        return outputDefinitionId;
    }

    /**
     * @param outputDefinitionId identifikátor entity
     */
    public void setOutputDefinitionId(final Integer outputDefinitionId) {
        this.outputDefinitionId = outputDefinitionId;
    }

    public LocalDateTime getLastUpdate() {
        return lastUpdate;
    }

    public void setLastUpdate(final LocalDateTime lastUpdate) {
        this.lastUpdate = lastUpdate;
    }

    /**
     * @return archivní soubor
     */
    public ArrFund getFund() {
        return fund;
    }

    /**
     * @param fund archivní soubor
     */
    public void setFund(final ArrFund fund) {
        this.fund = fund;
        this.fundId = fund != null ? fund.getFundId() : null;
    }

    public Integer getFundId() {
        return fundId;
    }

    /**
     * @return kód výstupu
     */
    public String getInternalCode() {
        return internalCode;
    }

    /**
     * @param internalCode kód výstupu
     */
    public void setInternalCode(final String internalCode) {
        this.internalCode = internalCode;
    }

    /**
     * @return jméno výstupu
     */
    public String getName() {
        return name;
    }

    /**
     * @param name jméno výstupu
     */
    public void setName(final String name) {
        this.name = name;
    }

    /**
     * @return stav
     */
    public OutputState getState() {
        return state;
    }

    /**
     * @param state stav
     */
    public void setState(final OutputState state) {
        this.state = state;
    }

    /**
     * @return příznak dočasného výstupu, lze použít pro Adhoc výstupy
     */
    public Boolean getTemporary() {
        return temporary;
    }

    /**
     * @param temporary příznak dočasného výstupu, lze použít pro Adhoc výstupy
     */
    public void setTemporary(final Boolean temporary) {
        this.temporary = temporary;
    }

    /**
     * @return příznak, že byl archivní fond smazán
     */
    public Boolean getDeleted() {
        return deleted;
    }

    /**
     * @param deleted příznak, že byl archivní fond smazán
     */
    public void setDeleted(final Boolean deleted) {
        this.deleted = deleted;
    }

    /**
     * @return List verzí outputů
     */
    public List<ArrOutput> getOutputs() {
        return outputs;
    }

    /**
     * @param outputs List verzí outputů
     */
    public void setOutputs(final List<ArrOutput> outputs) {
        this.outputs = outputs;
    }

    /**
     * @return List nodů outputu
     */
    public List<ArrNodeOutput> getOutputNodes() {
        return outputNodes;
    }

    /**
     * @param outputNodes List nodů outputu
     */
    public void setOutputNodes(final List<ArrNodeOutput> outputNodes) {
        this.outputNodes = outputNodes;
    }

    /**
     * @return Typ outputu
     */
    public RulOutputType getOutputType() {
        return outputType;
    }

    /**
     * @param outputType Typ outputu
     */
    public void setOutputType(final RulOutputType outputType) {
        this.outputType = outputType;
    }

    /**
     * @return šablona outputu
     */
    public RulTemplate getTemplate() {
        return template;
    }

    /**
     * @param template šablona outputu
     */
    public void setTemplate(final RulTemplate template) {
        this.template = template;
    }

    /**
     * @return Výsledek outputu
     */
    public ArrOutputResult getOutputResult() {
        return outputResult;
    }

    /**
     * @param outputResult Výsledek outputu
     */
    public void setOutputResult(final ArrOutputResult outputResult) {
        this.outputResult = outputResult;
    }

    /**
     * @return Vrátí chybu outputu
     */
    public String getError() {
        return error;
    }

    /**
     * @param error nastaví chybu outputu
     */
    public void setError(final String error) {
        this.error = error;
    }

    /**
     * @return nastavení šablony
     */
    public String getOutputSettings() {
        return outputSettings;
    }

    /**
     * @param outputSettings nastaví konfiguraci šablony
     */
    public void setOutputSettings(String outputSettings) {
        this.outputSettings = outputSettings;
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
