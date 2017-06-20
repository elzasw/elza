package cz.tacr.elza.print;

import org.apache.commons.lang.StringUtils;

import cz.tacr.elza.domain.ParUnitdate;
import cz.tacr.elza.domain.convertor.UnitDateConvertor;

/**
 * Formatted date/time value for outputs.
 *
 */
public class UnitDateText {

    private String valueText;

    private UnitDateText(String textForm) {
    	valueText = textForm;
	}
    
    protected UnitDateText()
    {
    	
    }

    public String getValueText() {
        return valueText;
    }

    public void setValueText(String valueText) {
        this.valueText = valueText;
    }

    @Override
    public String toString() {
        return valueText;
    }

	public static UnitDateText valueOf(ParUnitdate parUnitdate) {
        if (parUnitdate == null) {
            return null;
        }
        UnitDateText result = null;
        final String format = parUnitdate.getFormat();
        if (StringUtils.isNotBlank(format)) { // musí být nastaven formát
        	String textForm = UnitDateConvertor.convertToString(parUnitdate);
        	if(StringUtils.isNotBlank(textForm)) {
        		result = new UnitDateText(textForm);
        	}
        }
        return result;
	}
    
}
