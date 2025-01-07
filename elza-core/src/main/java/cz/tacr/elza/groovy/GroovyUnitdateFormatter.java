package cz.tacr.elza.groovy;

import static cz.tacr.elza.domain.converter.UnitDateConverterConsts.CENTURY;

import java.util.Objects;

import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotNull;

import cz.tacr.elza.api.IUnitdate;
import cz.tacr.elza.domain.converter.UnitDateConverter;

public class GroovyUnitdateFormatter {

    private final GroovyItem from;
    private final GroovyItem to;

    private String estimate = "~ ";
    private String prefixFrom = "";
    private String prefixTo = "";
    
    // Flag to display only year or century from given date
    private boolean formatYear = false;
    
    private boolean yearEqual = false;
    private String prefixYearEqual = "";

    public GroovyUnitdateFormatter(@Nullable final GroovyItem from, @Nullable final GroovyItem to) {
        this.from = from;
        this.to = to;

        // validace, že se jedná o unitdate itemy
        if (from != null) {
            from.getUnitdateValue();
        }
        if (to != null) {
            to.getUnitdateValue();
        }
    }

    public GroovyUnitdateFormatter estimate(final String estimate) {
    	Objects.requireNonNull(estimate);
        this.estimate = estimate;
        return this;
    }

    public GroovyUnitdateFormatter prefixFrom(final String prefixFrom) {
    	Objects.requireNonNull(prefixFrom);
        this.prefixFrom = prefixFrom;
        return this;
    }

    public GroovyUnitdateFormatter prefixTo(final String prefixTo) {
    	Objects.requireNonNull(prefixTo);
        this.prefixTo = prefixTo;
        return this;
    }

    public GroovyUnitdateFormatter formatYear() {
        this.formatYear = true;
        return this;
    }

    public GroovyUnitdateFormatter yearEqual(boolean yearEqual, String prefixYearEqual) {
    	Objects.requireNonNull(prefixYearEqual);
        this.yearEqual = yearEqual;
        this.prefixYearEqual = prefixYearEqual;
        return this;
    }

    // prevod casti from
    private String buildBeginUnitdate(IUnitdate unitdate) {
        if (formatYear && !unitdate.getFormat().equals(CENTURY)) {
            return UnitDateConverter.convertYear(unitdate, true);
        } else {
            return UnitDateConverter.beginToString(unitdate, false);
        }
    }

    private String buildEndUnitdate(IUnitdate unitdate) {
        if (formatYear && !unitdate.getFormat().equals(CENTURY)) {
            return UnitDateConverter.convertYear(unitdate, false);
        } else {
            return UnitDateConverter.endToString(unitdate, false);
        }
    }

    private String completeBeginUnitdate(IUnitdate unitdate, String str) {
        if (unitdate.getValueFromEstimated()) {
            str = estimate + str;
        }
        return joinStr(str, prefixFrom);
    }

    private String completeEndUnitdate(IUnitdate unitdate, String str) {
        if (unitdate.getValueToEstimated()) {
            str = estimate + str;
        }
        return joinStr(str, prefixTo);
    }

    private String completeEqualUnitdate(IUnitdate from, IUnitdate to, String str) {
        if (from.getValueFromEstimated()
                && to.getValueToEstimated()) {
            str = estimate + str;
        }
        return joinStr(str, prefixYearEqual);
    }

    private String joinStr(String str, String prefix) {
        return prefix + str;
    }

    // získat první slovo v řetězci, kde jsou slova oddělena mezerami
    private String getFirstWord(String str) {
        return str != null? str.split(" ")[0] : null;
    }

    @NotNull
    public String build() {
        if (from == null && to == null) {
            return "";
        }        
        String fromStr = from != null ? buildBeginUnitdate(from.getUnitdateValue()) : null;
        String toStr = to != null ? buildEndUnitdate(to.getUnitdateValue()) : null;

        if (from != null && to != null)
        {
        	if(formatYear && yearEqual && fromStr.equals(toStr) && getFirstWord(prefixFrom).equals(getFirstWord(prefixTo))) {
        		return completeEqualUnitdate(from.getUnitdateValue(), to.getUnitdateValue(), fromStr);
        	} else {
        		return completeBeginUnitdate(from.getUnitdateValue(), fromStr) + "-" + completeEndUnitdate(to.getUnitdateValue(), toStr);
        	}
            
        } else if (from != null) {
        	return completeBeginUnitdate(from.getUnitdateValue(), fromStr) + "-";
        } else {
        	return "?" + "-" + completeEndUnitdate(to.getUnitdateValue(), toStr);
        }
    }

}
