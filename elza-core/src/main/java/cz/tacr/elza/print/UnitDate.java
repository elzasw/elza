package cz.tacr.elza.print;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

import cz.tacr.elza.api.IUnitdate;
import cz.tacr.elza.domain.ArrCalendarType;
import cz.tacr.elza.domain.ArrItemUnitdate;
import cz.tacr.elza.domain.convertor.UnitDateConvertor;

/**
 * Rozšiřuje {@link UnitDateText} o strukturovaný zápis datumu.
 *
 */
public class UnitDate extends UnitDateText implements IUnitdate {

	private String valueFrom;
    private String valueTo;
    private Boolean valueFromEstimated;
    private Boolean valueToEstimated;
    private String format;
    
    private String calendar;
    private String calendarCode;
    private ArrCalendarType calendarType;

    private UnitDate(ArrItemUnitdate srcItemData) {
		this.valueFrom = srcItemData.getValueFrom();
		this.valueTo = srcItemData.getValueTo();
		this.valueFromEstimated = srcItemData.getValueFromEstimated();
		this.valueToEstimated = srcItemData.getValueToEstimated();
		this.format = srcItemData.getFormat();
        this.calendarType = srcItemData.getCalendarType();
        this.calendar = calendarType.getName();
        this.calendarCode = calendarType.getCode();
        
        String textForm = UnitDateConvertor.convertToString(this);
        this.setValueText(textForm);
	}

    /**
     * @return hodnota valueText
     */
    public String serialize() {
        if (StringUtils.isNotBlank(getValueText())) {
            return getValueText();
        }
        return UnitDateConvertor.convertToString(this);
    }

    public String getCalendar() {
        return calendar;
    }

    public String getCalendarCode() {
        return calendarCode;
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
    public ArrCalendarType getCalendarType() {
        return calendarType;
    }

	@Override
	public void setCalendarType(ArrCalendarType calendarType) {
		// TODO Auto-generated method stub
		
	}

	@Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SIMPLE_STYLE);
    }

	public static UnitDate valueOf(ArrItemUnitdate itemData) {
		UnitDate unitDate = new UnitDate(itemData);		
		return unitDate;
	}

}
