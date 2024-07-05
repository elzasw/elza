package cz.tacr.elza.groovy;


import cz.tacr.elza.api.IUnitdate;
import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.core.data.ItemType;
import cz.tacr.elza.domain.RulItemSpec;
import cz.tacr.elza.domain.convertor.UnitDateConvertor;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.service.cache.CachedAccessPoint;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotNull;

public class GroovyItem {

    /**
     * Typ prvku
     */
    private final ItemType itemType;

    /**
     * Specifikace.
     */
    private final RulItemSpec rulItemSpec;

    /**
     * Logická hodnota.
     */
    private Boolean boolValue = null;

    /**
     * Textová hodnota.
     */
    private String value = null;

    /**
     * Číselná hodnota.
     */
    private Integer intValue = null;

    /**
     * Hodnota datace.
     */
    private IUnitdate unitdateValue = null;

    /**
     * AP z mezipaměti.
     */
    private CachedAccessPoint accessPoint = null;

    public GroovyItem(@NotNull final ItemType itemType,
                      @Nullable final RulItemSpec rulItemSpec) {
        this.itemType = itemType;
        this.rulItemSpec = rulItemSpec;
    }

    public GroovyItem(@NotNull final ItemType itemType,
                      @Nullable final RulItemSpec rulItemSpec,
                      @NotNull final String value) {
        this(itemType, rulItemSpec);
        if (itemType.getDataType() != DataType.TEXT &&
                itemType.getDataType() != DataType.STRING &&
                // TODO: Improve URI REF support
                itemType.getDataType() != DataType.URI_REF &&
                // TODO: Improve coordinates support
                itemType.getDataType() != DataType.COORDINATES &&
                // TODO: Improve enum support
                itemType.getDataType() != DataType.ENUM) {
            throw new BusinessException("String value not supported", BaseCode.PROPERTY_HAS_INVALID_TYPE);
        }
        this.value = value;
    }

    public GroovyItem(@NotNull final ItemType itemType,
                      @Nullable final RulItemSpec rulItemSpec,
                      @Nullable final CachedAccessPoint accessPoint,
                      @NotNull final String value,
                      @NotNull final Integer intValue) {
        this(itemType, rulItemSpec);
        if (itemType.getDataType() != DataType.RECORD_REF) {
            throw new BusinessException("RecordRef value not supported", BaseCode.PROPERTY_HAS_INVALID_TYPE);
        }
        this.accessPoint = accessPoint;
        this.value = value;
        this.intValue = intValue;
    }

    public GroovyItem(@NotNull final ItemType itemType,
                      @Nullable final RulItemSpec rulItemSpec,
                      @NotNull final Boolean boolValue) {
        this(itemType, rulItemSpec);
        if (itemType.getDataType() != DataType.BIT) {
            throw new BusinessException("Boolean value not supported", BaseCode.PROPERTY_HAS_INVALID_TYPE);
        }
        this.boolValue = boolValue;
    }

    public GroovyItem(@NotNull final ItemType itemType,
                      @Nullable final RulItemSpec rulItemSpec,
                      @NotNull final Integer intValue) {
        this(itemType, rulItemSpec);
        if (itemType.getDataType() != DataType.INT) {
            throw new BusinessException("Integer value not supported", BaseCode.PROPERTY_HAS_INVALID_TYPE);
        }
        this.intValue = intValue;
    }

    public GroovyItem(@NotNull final ItemType itemType,
                      @Nullable final RulItemSpec rulItemSpec,
                      @NotNull final IUnitdate value) {
        this(itemType, rulItemSpec);
        if (itemType.getDataType() != DataType.UNITDATE) {
            throw new BusinessException("Integer value not supported", BaseCode.PROPERTY_HAS_INVALID_TYPE);
        }
        this.value = UnitDateConvertor.convertToString(value);
        this.unitdateValue = value;
    }

    public Boolean getBoolValue() {
        if (boolValue == null) {
            throw new IllegalArgumentException("Item nemá bool hodnotu: " + toString());
        }
        return boolValue;
    }

    public String getValue() {
        if (value != null) {
            return value;
        } else if (boolValue != null) {
            return String.valueOf(boolValue);
        } else if (intValue != null) {
            return String.valueOf(intValue);
        } else {
            throw new IllegalArgumentException("Item nemá string hodnotu: " + toString());
        }
    }

    public Integer getIntValue() {
        if (intValue == null) {
            throw new IllegalArgumentException("Item nemá int hodnotu: " + toString());
        }
        return intValue;
    }

    public IUnitdate getUnitdateValue() {
        if (unitdateValue == null) {
            throw new IllegalArgumentException("Item nemá hodnotu datace: " + toString());
        }
        return unitdateValue;
    }

    public Integer getSpecId() {
        return (rulItemSpec != null) ? rulItemSpec.getItemSpecId() : null;
    }

    public String getSpec() {
        return (rulItemSpec != null) ? rulItemSpec.getName() : null;
    }

    public String getSpecCode() {
        return (rulItemSpec != null) ? rulItemSpec.getCode() : null;
    }

    public Integer getSpecOrder() {
        return (rulItemSpec != null) ? rulItemSpec.getViewOrder() : null;
    }

    public ItemType getItemType() {
        return itemType;
    }

    public String getTypeCode() {
        return itemType.getCode();
    }

    public void addValue(GroovyItem item) {
        if (item != null && item.getValue() != null) {
            value += "; " + item.getValue();
        }
    }

    public void addValue(GroovyItem item, String separator) {
        if (item != null && item.getValue() != null) {
            value += separator + item.getValue();
        }
    }

    public CachedAccessPoint getAccessPoint() {
        return accessPoint;
    }

    public Integer getApTypeId() {
        return (accessPoint != null) ? accessPoint.getApState().getApTypeId() : null;
    }

    @Override
    public String toString() {
        return "GroovyItem{" +
                "spec='" + rulItemSpec + '\'' +
                ", typeCode='" + itemType.getCode() + '\'' +
                ", boolValue=" + boolValue +
                ", value='" + value + '\'' +
                ", intValue=" + intValue +
                ", unitdateValue=" + unitdateValue +
                ", apTypeId=" + getApTypeId() +
                '}';
    }

    public RulItemSpec getSpecType() {
        return rulItemSpec;
    }
}
