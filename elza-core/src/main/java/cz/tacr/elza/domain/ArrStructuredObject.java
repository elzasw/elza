package cz.tacr.elza.domain;

import static cz.tacr.elza.domain.enumeration.StringLength.LENGTH_ENUM;

import javax.persistence.Access;
import javax.persistence.AccessType;
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

import org.hibernate.annotations.Type;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import cz.tacr.elza.api.interfaces.IArrFund;
import cz.tacr.elza.domain.enumeration.StringLength;
import cz.tacr.elza.service.importnodes.vo.Structured;


/**
 * Základní definice pro popis strukturovaného typu a serializaci hodnoty strukturovaného typu.
 *
 * @since 27.10.2017
 */
@Entity(name = "arr_structured_object")
@Table
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ArrStructuredObject implements IArrFund, Structured {

    public final static String TABLE_NAME = "arr_structured_object";

    public final static String FIELD_STRUCTURED_OBJECT_ID = "structuredObjectId";

    public final static String FIELD_CREATE_CHANGE = "createChange";

    public final static String FIELD_CREATE_CHANGE_ID = "createChangeId";

    public final static String FIELD_DELETE_CHANGE = "deleteChange";

    public final static String FIELD_DELETE_CHANGE_ID = "deleteChangeId";

    public final static String FIELD_VALUE = "value";

    public final static String FIELD_STATE = "state";

    @Id
    @GeneratedValue
    @Access(AccessType.PROPERTY) // required to read id without fetch from db
    private Integer structuredObjectId;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ArrChange.class)
    @JoinColumn(name = "createChangeId", nullable = false)
    private ArrChange createChange;

    @Column(name = "createChangeId", updatable = false, insertable = false)
    private Integer createChangeId;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ArrChange.class)
    @JoinColumn(name = "deleteChangeId")
    private ArrChange deleteChange;

    @Column(name = "deleteChangeId", updatable = false, insertable = false)
    private Integer deleteChangeId;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = RulStructuredType.class)
    @JoinColumn(name = "structuredTypeId", nullable = false)
    private RulStructuredType structuredType;

    @Column(name = "structuredTypeId", updatable = false, insertable = false)
    private Integer structuredTypeId;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ArrFund.class)
    @JoinColumn(name = "fundId", nullable = false)
    private ArrFund fund;

    @Column(name = "fundId", updatable = false, insertable = false)
    private Integer fundId;

    @Column(length = StringLength.LENGTH_1000)
    private String value;

    @Column(length = StringLength.LENGTH_1000)
    private String sortValue;

    @Column(length = StringLength.LENGTH_1000)
    private String complement;

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
    public Integer getStructuredObjectId() {
        return structuredObjectId;
    }

    /**
     * @param structuredObjectId identifikátor entity
     */
    public void setStructuredObjectId(final Integer structuredObjectId) {
        this.structuredObjectId = structuredObjectId;
    }

    /**
     * @return typ datového typu
     */
    public RulStructuredType getStructuredType() {
        return structuredType;
    }

    /**
     * @param structuredType typ datového typu
     */
    public void setStructuredType(final RulStructuredType structuredType) {
        this.structuredType = structuredType;
        this.structuredTypeId = structuredType == null ? null : structuredType.getStructuredTypeId();
    }

    /**
     * @return User visible value
     */
    public String getValue() {
        return value;
    }

    /**
     * @param value User visible value
     */
    public void setValue(final String value) {
        this.value = value;
    }

    /**
     * Return value used for sorting
     * @return
     */
    public String getSortValue() {
		return sortValue;
	}

    /**
     * Set sort value
     * @param sortValue
     */
	public void setSortValue(String sortValue) {
		this.sortValue = sortValue;
	}

    /**
     * @return doplněk
     */
    public String getComplement() {
        return complement;
    }

    public void setComplement(String complement) {
        this.complement = complement;
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
        this.createChangeId = createChange == null ? null : createChange.getChangeId();
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
        this.deleteChangeId = deleteChange == null ? null : deleteChange.getChangeId();
    }

    /**
     * @return archivní soubor
     */
    @Override
    public ArrFund getFund() {
        return fund;
    }

    /**
     * @param fund archivní soubor
     */
    public void setFund(final ArrFund fund) {
        this.fund = fund;
        this.fundId = fund == null ? null : fund.getFundId();
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

    public Integer getCreateChangeId() {
        return createChangeId;
    }

    public Integer getDeleteChangeId() {
        return deleteChangeId;
    }

    public Integer getStructuredTypeId() {
        return structuredTypeId;
    }

    public Integer getFundId() {
        return fundId;
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
