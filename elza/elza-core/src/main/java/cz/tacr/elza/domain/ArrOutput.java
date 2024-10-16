package cz.tacr.elza.domain;

import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;

/**
 * Výstup z archivního souboru.
 *
 * @author Martin Šlapa
 * @since 01.04.2016
 */
@Entity(name = "arr_output")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "id"})
public class ArrOutput extends AbstractVersionableEntity {

    public static final String TABLE_NAME = "arr_output";

    public static final String FIELD_CREATE_CHANGE_ID = "createChangeId";
    public static final String FIELD_DELETE_CHANGE_ID = "deleteChangeId";
    public static final String ANONYMIZED_AP_ID = "anonymizedApId";

    @Id
    @GeneratedValue
    @Access(AccessType.PROPERTY) // required to read id without fetch from db
    private Integer outputId;

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

    @Column(length = 250)
    private String internalCode;

    @Column(length = 250, nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(length = 12, nullable = false)
    private OutputState state;

    @Column
    private String error;

    @Column(nullable = true)
    private String outputSettings;

    @OneToMany(mappedBy = "output", fetch = FetchType.LAZY)
    private List<ArrNodeOutput> outputNodes;

    @OneToMany(mappedBy = "output", fetch = FetchType.LAZY)
    private List<ArrOutputResult> outputResults;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ArrChange.class)
    @JoinColumn(name = FIELD_CREATE_CHANGE_ID, nullable = false)
    private ArrChange createChange;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ArrChange.class)
    @JoinColumn(name = FIELD_DELETE_CHANGE_ID)
    private ArrChange deleteChange;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ApAccessPoint.class)
    @JoinColumn(name = ANONYMIZED_AP_ID)
    private ApAccessPoint anonymizedAp;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = RulOutputFilter.class)
    @JoinColumn(name = "outputFilterId")
    private RulOutputFilter outputFilter;

    /**
     * @return  identifikátor entity
     */
    public Integer getOutputId() {
        return outputId;
    }

    /**
     * @param outputId  identifikátor entity
     */
    public void setOutputId(Integer outputId) {
        this.outputId = outputId;
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
     * @return Výsledek outputu
     */
    public List<ArrOutputResult> getOutputResults() {
        return outputResults;
    }

    /**
     * @param outputResults Výsledek outputu
     */
    public void setOutputResults(final List<ArrOutputResult> outputResults) {
        this.outputResults = outputResults;
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
     * @return změna vytvoření
     */
    public ArrChange getCreateChange() {
        return createChange;
    }

    /**
     * @param createChange změna vytvoření
     */
    public void setCreateChange(ArrChange createChange) {
        this.createChange = createChange;
    }

    /**
     * @return změna smazání
     */
    public ArrChange getDeleteChange() {
        return deleteChange;
    }

    /**
     * @param deleteChange změna smazání
     */
    public void setDeleteChange(ArrChange deleteChange) {
        this.deleteChange = deleteChange;
    }

    public ApAccessPoint getAnonymizedAp() {
        return anonymizedAp;
    }

    public void setAnonymizedAp(ApAccessPoint anonymizedAp) {
        this.anonymizedAp = anonymizedAp;
    }

    public RulOutputFilter getOutputFilter() {
        return outputFilter;
    }

    public void setOutputFilter(RulOutputFilter outputFilter) {
        this.outputFilter = outputFilter;
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
