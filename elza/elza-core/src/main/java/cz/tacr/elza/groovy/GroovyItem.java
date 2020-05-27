package cz.tacr.elza.groovy;

import cz.tacr.elza.api.IUnitdate;
import cz.tacr.elza.domain.convertor.UnitDateConvertor;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;

public class GroovyItem {

    /**
     * Název specifikace.
     */
    private final String spec;

    /**
     * Kód specifikace.
     */
    private final String specCode;

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

    public GroovyItem(@NotNull final String typeCode,
                      @Nullable final String spec,
                      @Nullable final String specCode,
                      @NotNull final String value) {
        this.value = value;
        this.spec = spec;
        this.specCode = specCode;
        this.typeCode = typeCode;
    }

    public GroovyItem(@NotNull final String typeCode,
                      @Nullable final String spec,
                      @Nullable final String specCode,
                      @NotNull final String value,
                      @NotNull final Integer intValue) {
        this.value = value;
        this.spec = spec;
        this.intValue = intValue;
        this.specCode = specCode;
        this.typeCode = typeCode;
    }

    public GroovyItem(@NotNull final String typeCode,
                      @Nullable final String spec,
                      @Nullable final String  specCode,
                      @NotNull final Boolean boolValue) {
        this.boolValue = boolValue;
        this.spec = spec;
        this.specCode = specCode;
        this.typeCode = typeCode;
    }

    public GroovyItem(@NotNull final String typeCode,
                      @Nullable final String spec,
                      @Nullable final String specCode,
                      @NotNull final Integer intValue) {
        this.intValue = intValue;
        this.spec = spec;
        this.specCode = specCode;
        this.typeCode = typeCode;
    }

    public GroovyItem(@NotNull final String typeCode,
                      @Nullable final String spec,
                      @Nullable final String specCode,
                      @NotNull final IUnitdate value) {
        this.value = UnitDateConvertor.convertToString(value);
        this.spec = spec;
        this.specCode = specCode;
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

    public String getSpec() {
        return spec;
    }

    public String getSpecCode() {
        return specCode;
    }

    public String getTypeCode() {
        return typeCode;
    }

    @Override
    public String toString() {
        return "GroovyItem{" +
                "spec='" + spec + '\'' +
                ", specCode='" + specCode + '\'' +
                ", typeCode='" + typeCode + '\'' +
                ", boolValue=" + boolValue +
                ", value='" + value + '\'' +
                ", intValue=" + intValue +
                ", unitdateValue=" + unitdateValue +
                '}';
    }
}
