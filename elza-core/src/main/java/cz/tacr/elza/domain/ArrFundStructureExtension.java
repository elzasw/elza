package cz.tacr.elza.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;


/**
 * Aktivace rozšíření v archivním souboru.
 *
 * @since 30.10.2017
 */
@Entity(name = "arr_fund_structure_extension")
@Table
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ArrFundStructureExtension {

    @Id
    @GeneratedValue
    private Integer fundStructureExtensionId;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ArrChange.class)
    @JoinColumn(name = "createChangeId", nullable = false)
    private ArrChange createChange;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ArrChange.class)
    @JoinColumn(name = "deleteChangeId")
    private ArrChange deleteChange;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = RulStructureExtension.class)
    @JoinColumn(name = "structureExtensionId", nullable = false)
    private RulStructureExtension structureExtension;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ArrFund.class)
    @JoinColumn(name = "fundId", nullable = false)
    private ArrFund fund;

    /**
     * @return identifikátor entity
     */
    public Integer getFundStructureExtensionId() {
        return fundStructureExtensionId;
    }

    /**
     * @param fundStructureExtensionId identifikátor entity
     */
    public void setFundStructureExtensionId(final Integer fundStructureExtensionId) {
        this.fundStructureExtensionId = fundStructureExtensionId;
    }

    /**
     * @return rozšížení struktuálního datového typu
     */
    public RulStructureExtension getStructureExtension() {
        return structureExtension;
    }

    /**
     * @param structureExtension rozšížení struktuálního datového typu
     */
    public void setStructureExtension(final RulStructureExtension structureExtension) {
        this.structureExtension = structureExtension;
    }

    /**
     * @return změna při vytvoření
     */
    public ArrChange getCreateChange() {
        return createChange;
    }

    /**
     * @param createChange změna při vytvoření
     */
    public void setCreateChange(final ArrChange createChange) {
        this.createChange = createChange;
    }

    /**
     * @return změna při mazání
     */
    public ArrChange getDeleteChange() {
        return deleteChange;
    }

    /**
     * @param deleteChange změna při mazání
     */
    public void setDeleteChange(final ArrChange deleteChange) {
        this.deleteChange = deleteChange;
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
    }

}
