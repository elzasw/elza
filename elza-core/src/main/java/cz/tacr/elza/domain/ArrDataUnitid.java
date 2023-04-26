package cz.tacr.elza.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import org.apache.commons.lang.Validate;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import cz.tacr.elza.domain.enumeration.StringLength;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.codes.BaseCode;


/**
 * Hodnota atributu archivního popisu typu referenční označení.
 */
@Entity(name = "arr_data_unitid")
@Table
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ArrDataUnitid extends ArrData {

    /**
     * Name of attribute with value
     */
    public static final String FIELD_UNITID = "unitId";

    /**
     * value of the unitid
     */
    // attribute has name unitid and not value
    // because operator TREAT in JPQL needs
    // unique names (tested with Hibernate 5.2)
    // see LockedValueRepository and query findByFundAndItemTypeAndValue
    @Column(name = "unit_value", length = StringLength.LENGTH_250, nullable = false)
    private String unitId;

	public ArrDataUnitid() {

	}

	protected ArrDataUnitid(ArrDataUnitid src) {
		super(src);
        copyValue(src);
	}

    private void copyValue(ArrDataUnitid src) {
        this.unitId = src.unitId;
    }

    public String getUnitId() {
        return unitId;
    }

    public void setUnitId(final String value) {
        this.unitId = value;
    }

    @Override
    public String getFulltextValue() {
        return getUnitId();
    }

	@Override
	public ArrDataUnitid makeCopy() {
		return new ArrDataUnitid(this);
	}

    @Override
    protected boolean isEqualValueInternal(ArrData srcData) {
        ArrDataUnitid src = (ArrDataUnitid)srcData;
        return unitId.equals(src.unitId);
    }

    @Override
    public void mergeInternal(final ArrData srcData) {
        ArrDataUnitid src = (ArrDataUnitid) srcData;
        copyValue(src);
    }

    @Override
    protected void validateInternal() {
        Validate.notNull(unitId);
        // check any leading and trailing whitespace in data
        if (unitId.trim().length() != unitId.length()) {
            throw new BusinessException("Value contains whitespaces at the begining or end",
                    BaseCode.PROPERTY_IS_INVALID)
                            .set("dataId", getDataId())
                            .set("property", unitId);
        }
    }
}
