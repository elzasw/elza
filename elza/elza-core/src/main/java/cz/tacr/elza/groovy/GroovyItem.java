package cz.tacr.elza.groovy;

import cz.tacr.elza.api.IUnitdate;
import cz.tacr.elza.domain.RulItemSpec;
import cz.tacr.elza.domain.convertor.UnitDateConvertor;
import cz.tacr.elza.service.cache.CachedAccessPoint;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotNull;

public class GroovyItem {

    /**
     * Specifikace.
     */
    private final RulItemSpec rulItemSpec;

    /**
     * Kód typu.
     */
    private final String typeCode;

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

    public GroovyItem(@NotNull final String typeCode,
                      @Nullable final RulItemSpec rulItemSpec,
                      @NotNull final String value) {
        this.value = value;
        this.rulItemSpec = rulItemSpec;
        this.typeCode = typeCode;
    }

    public GroovyItem(@NotNull final String typeCode,
                      @Nullable final RulItemSpec rulItemSpec,
                      @NotNull final String value,
                      @NotNull final Integer intValue,
                      @Nullable final CachedAccessPoint accessPoint) {
        this.value = value;
        this.rulItemSpec = rulItemSpec;
        this.intValue = intValue;
        this.typeCode = typeCode;
        this.accessPoint = accessPoint;
    }

    public GroovyItem(@NotNull final String typeCode,
                      @Nullable final RulItemSpec rulItemSpec,
                      @NotNull final Boolean boolValue) {
        this.boolValue = boolValue;
        this.rulItemSpec = rulItemSpec;
        this.typeCode = typeCode;
    }

    public GroovyItem(@NotNull final String typeCode,
                      @Nullable final RulItemSpec rulItemSpec,
                      @NotNull final Integer intValue) {
        this.intValue = intValue;
        this.rulItemSpec = rulItemSpec;
        this.typeCode = typeCode;
    }

    public GroovyItem(@NotNull final String typeCode,
                      @Nullable final RulItemSpec rulItemSpec,
                      @NotNull final IUnitdate value) {
        this.value = UnitDateConvertor.convertToString(value);
        this.rulItemSpec = rulItemSpec;
        this.typeCode = typeCode;
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

    public String getTypeCode() {
        return typeCode;
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
                ", typeCode='" + typeCode + '\'' +
                ", boolValue=" + boolValue +
                ", value='" + value + '\'' +
                ", intValue=" + intValue +
                ", unitdateValue=" + unitdateValue +
                ", apTypeId=" + getApTypeId() +
                '}';
    }
}
