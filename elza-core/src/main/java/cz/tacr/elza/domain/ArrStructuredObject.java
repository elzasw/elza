package cz.tacr.elza.domain;

import static cz.tacr.elza.domain.enumeration.StringLength.LENGTH_ENUM;

import java.util.UUID;

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
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import org.apache.commons.lang3.Validate;
import org.hibernate.annotations.Type;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import cz.tacr.elza.api.interfaces.ArrFundGetter;
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
public class ArrStructuredObject implements ArrFundGetter, Structured {

    public final static String TABLE_NAME = "arr_structured_object";

    public final static String FIELD_STRUCTURED_OBJECT_ID = "structuredObjectId";

    public final static String FIELD_CREATE_CHANGE = "createChange";

    public final static String FIELD_CREATE_CHANGE_ID = "createChangeId";

    public final static String FIELD_DELETE_CHANGE = "deleteChange";

    public final static String FIELD_DELETE_CHANGE_ID = "deleteChangeId";

    public final static String FIELD_VALUE = "value";

    public final static String FIELD_STATE = "state";

    /**
     * Builder for ArrStructuredObject
     *
     *
     */
    public static class Builder {
        private ArrChange createChange;
        private ArrFund fund;
        private RulStructuredType structureType;
        private State state = State.TEMP;
        private String uuid;

        public Builder(final ArrChange createChange,
                       final ArrFund fund,
                       final RulStructuredType structureType) {
            this.createChange = createChange;
            this.fund = fund;
            this.structureType = structureType;
        }

        public ArrStructuredObject build() {
            ArrStructuredObject so = new ArrStructuredObject();
            so.setCreateChange(createChange);
            so.setAssignable(true);
            so.setFund(fund);
            so.setStructuredType(structureType);
            so.setState(state);

            if (uuid != null) {
                Validate.isTrue(uuid.length() == 36, "Unexpected UUID value: %s", uuid);
                so.setUuid(uuid);
            } else {
                so.setUuid(UUID.randomUUID().toString());
            }
            return so;
        }

        public Builder setState(final State state) {
            this.state = state;
            return this;
        }

        public Builder setUuid(final String uuid) {
            this.uuid = uuid;
            return this;
        }
    }

    /**
     * Make copy of structured object
     * 
     * @param uuid
     *            UUID for new object
     * @return
     */
    public ArrStructuredObject makeCopyWithoutId(final String uuid) {
        ArrStructuredObject trg = new ArrStructuredObject();
        trg.setState(state);
        trg.setValue(value);
        trg.setComplement(complement);
        trg.setAssignable(assignable);
        trg.setDeleteChange(deleteChange);
        trg.setErrorDescription(errorDescription);
        trg.setSortValue(sortValue);
        trg.setUuid(uuid);
        trg.setCreateChange(createChange);
        trg.setFund(fund);
        trg.setStructuredType(structuredType);
        return trg;
    }

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

    @Column(name = "object_value", length = StringLength.LENGTH_1000)
    private String value;

    @Column(length = StringLength.LENGTH_1000)
    private String sortValue;

    @Column(length = StringLength.LENGTH_1000)
    private String complement;

    @Column(nullable = false)
    private Boolean assignable;

    @Column(length = StringLength.LENGTH_36, nullable = false)
    private String uuid;

    @Column
    //@Lob
    //@Type(type = "org.hibernate.type.TextType") TODO hibernate search 6
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

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
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
