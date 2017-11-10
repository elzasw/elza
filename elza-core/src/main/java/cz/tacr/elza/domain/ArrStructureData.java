package cz.tacr.elza.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import cz.tacr.elza.domain.enumeration.StringLength;
import org.hibernate.annotations.Type;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import static cz.tacr.elza.domain.enumeration.StringLength.LENGTH_ENUM;


/**
 * Základní definice pro popis strukturovaného typu a serializaci hodnoty strukturovaného typu.
 *
 * @since 27.10.2017
 */
@Entity(name = "arr_structure_data")
@Table
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ArrStructureData {

    @Id
    @GeneratedValue
    private Integer structureDataId;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ArrChange.class)
    @JoinColumn(name = "createChangeId", nullable = false)
    private ArrChange createChange;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ArrChange.class)
    @JoinColumn(name = "deleteChangeId")
    private ArrChange deleteChange;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = RulStructureType.class)
    @JoinColumn(name = "structureTypeId", nullable = false)
    private RulStructureType structureType;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ArrFund.class)
    @JoinColumn(name = "fundId", nullable = false)
    private ArrFund fund;

    @Column(length = StringLength.LENGTH_1000)
    private String value;

    @Column(nullable = false)
    private Boolean assignable;

    @Column
    @Lob
    @Type(type = "org.hibernate.type.TextType")
    private String errorDescription;

    @Enumerated(EnumType.STRING)
    @Column(length = LENGTH_ENUM, nullable = false)
    private State state;

    /**
     * @return identifikátor entity
     */
    public Integer getStructureDataId() {
        return structureDataId;
    }

    /**
     * @param structureDataId identifikátor entity
     */
    public void setStructureDataId(final Integer structureDataId) {
        this.structureDataId = structureDataId;
    }

    /**
     * @return typ datového typu
     */
    public RulStructureType getStructureType() {
        return structureType;
    }

    /**
     * @param structureType typ datového typu
     */
    public void setStructureType(final RulStructureType structureType) {
        this.structureType = structureType;
    }

    /**
     * @return priorita vykonávání
     */
    public String getValue() {
        return value;
    }

    /**
     * @param value priorita vykonávání
     */
    public void setValue(final String value) {
        this.value = value;
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

    /**
     * @return přiřaditelný k JP
     */
    public Boolean getAssignable() {
        return assignable;
    }

    /**
     * @param assignable přiřaditelný k JP
     */
    public void setAssignable(final Boolean assignable) {
        this.assignable = assignable;
    }

    /**
     * @return chybový popis
     */
    public String getErrorDescription() {
        return errorDescription;
    }

    /**
     * @param errorDescription chybový popis
     */
    public void setErrorDescription(final String errorDescription) {
        this.errorDescription = errorDescription;
    }

    /**
     * @return stav entity
     */
    public State getState() {
        return state;
    }

    /**
     * @param state stav entity
     */
    public void setState(final State state) {
        this.state = state;
    }

    /**
     * Stav entity.
     */
    public enum State {

        /**
         * Stav při pořízení nového, ale ještě nepotvrzeného.
         */
        TEMP,

        /**
         * Zvalidovaný uložený strukturovaný typ.
         */
        OK,

        /**
         * Uložený strukturovaný typ s chybou validace.
         */
        ERROR

    }
}
