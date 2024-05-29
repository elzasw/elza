package cz.tacr.elza.print;

import cz.tacr.elza.api.IUnitdate;
import cz.tacr.elza.domain.convertor.UnitDateConvertor;

/**
 * Rozšiřuje {@link UnitDateText} o strukturovaný zápis datumu.
 *
 */
public class UnitDate implements IUnitdate {

    private String valueFrom;

    private String valueTo;

    private Boolean valueFromEstimated;

    private Boolean valueToEstimated;

    private Long normalizedFrom;

    private Long normalizedTo;

    private String format;

    public UnitDate() {
    }

    public UnitDate(IUnitdate srcItemData) {
        this.valueFrom = srcItemData.getValueFrom();
        this.valueTo = srcItemData.getValueTo();
        this.valueFromEstimated = srcItemData.getValueFromEstimated();
        this.valueToEstimated = srcItemData.getValueToEstimated();
        this.normalizedFrom = srcItemData.getNormalizedFrom();
        this.normalizedTo = srcItemData.getNormalizedTo();
        this.format = srcItemData.getFormat();
    }

    @Override
    public String getFormat() {
        return format;
    }

    @Override
    public void setFormat(final String format) {
        this.format = format;
    }

    @Override
    public void formatAppend(final String format) {
        this.format += format;
    }

    @Override
    public String getValueFrom() {
        return valueFrom;
    }

    @Override
    public void setValueFrom(final String valueFrom) {
        this.valueFrom = valueFrom;
    }

    @Override
    public Boolean getValueFromEstimated() {
        return valueFromEstimated;
    }

    @Override
    public void setValueFromEstimated(final Boolean valueFromEstimated) {
        this.valueFromEstimated = valueFromEstimated;
    }

    @Override
    public Long getNormalizedFrom() {
        return normalizedFrom;
    }

    @Override
    public void setNormalizedFrom(final Long normalizedFrom) {
        this.normalizedFrom = normalizedFrom;
    }

    @Override
    public String getValueTo() {
        return valueTo;
    }

    @Override
    public void setValueTo(final String valueTo) {
        this.valueTo = valueTo;
    }

    @Override
    public Boolean getValueToEstimated() {
        return valueToEstimated;
    }

    @Override
    public void setValueToEstimated(final Boolean valueToEstimated) {
        this.valueToEstimated = valueToEstimated;
    }

    @Override
    public Long getNormalizedTo() {
        return normalizedTo;
    }

    @Override
    public void setNormalizedTo(final Long normalizedTo) {
        this.normalizedTo = normalizedTo;
    }

    public String getValueText() {
        return UnitDateConvertor.convertToString(this);
    }
}
