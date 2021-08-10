package cz.tacr.elza.groovy;

import cz.tacr.elza.api.IUnitdate;
import cz.tacr.elza.domain.convertor.UnitDateConvertor;
import org.apache.commons.lang3.Validate;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;

import static cz.tacr.elza.domain.convertor.UnitDateConvertorConsts.CENTURY;

public class GroovyUnitdateFormatter {

    private final GroovyItem from;
    private final GroovyItem to;

    private String estimate = "asi ";
    private String prefixFrom = "";
    private String prefixTo = "";
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
        Validate.notNull(estimate);
        this.estimate = estimate;
        return this;
    }

    public GroovyUnitdateFormatter prefixFrom(final String prefixFrom) {
        Validate.notNull(prefixFrom);
        this.prefixFrom = prefixFrom;
        return this;
    }

    public GroovyUnitdateFormatter prefixTo(final String prefixTo) {
        Validate.notNull(prefixTo);
        this.prefixTo = prefixTo;
        return this;
    }

    public GroovyUnitdateFormatter formatYear() {
        this.formatYear = true;
        return this;
    }

    public GroovyUnitdateFormatter yearEqual(boolean yearEqual, String prefixYearEqual) {
        Validate.notNull(prefixYearEqual);
        this.yearEqual = yearEqual;
        this.prefixYearEqual = prefixYearEqual;
        return this;
    }

    private String buildBeginUnitdate(IUnitdate unitdate) {
        if (formatYear && !unitdate.getFormat().equals(CENTURY)) {
            return UnitDateConvertor.convertYear(unitdate, true);
        } else {
            return UnitDateConvertor.beginToString(unitdate, false);
        }
    }

    private String buildEndUnitdate(IUnitdate unitdate) {
        if (formatYear && !unitdate.getFormat().equals(CENTURY)) {
            return UnitDateConvertor.convertYear(unitdate, false);
        } else {
            return UnitDateConvertor.endToString(unitdate, false);
        }
    }

    private String completeBeginUnitdate(IUnitdate unitdate, String str) {
        if (unitdate.getValueFromEstimated() && !unitdate.getFormat().equals(CENTURY)) {
            str = estimate + str;
        }
        return joinStr(str, prefixFrom);
    }

    private String completeEndUnitdate(IUnitdate unitdate, String str) {
        if (unitdate.getValueToEstimated() && !unitdate.getFormat().equals(CENTURY)) {
            str = estimate + str;
        }
        return joinStr(str, prefixTo);
    }

    private String completeEqualUnitdate(IUnitdate from, IUnitdate to, String str) {
        if (from.getValueFromEstimated()
                && to.getValueToEstimated()
                && !from.getFormat().equals(CENTURY)
                && !to.getFormat().equals(CENTURY)) {
            str = estimate + str;
        }
        return joinStr(str, prefixYearEqual);
    }

    private String joinStr(String str, String prefix) {
        return prefix + str;
    }

    @NotNull
    public String build() {
        String result = "";
        String fromStr = from != null ? buildBeginUnitdate(from.getUnitdateValue()) : null;
        String toStr = to != null ? buildEndUnitdate(to.getUnitdateValue()) : null;

        if (from == null && to == null) {
            return result;
        } else if (from != null && to != null && formatYear && yearEqual && fromStr.equals(toStr)) {
            result = completeEqualUnitdate(from.getUnitdateValue(), to.getUnitdateValue(), fromStr);
        } else if (from != null && to != null) {
            result = completeBeginUnitdate(from.getUnitdateValue(), fromStr) + "-" + completeEndUnitdate(to.getUnitdateValue(), toStr);
        } else if (from != null) {
            result = completeBeginUnitdate(from.getUnitdateValue(), fromStr) + "-";
        } else {
            result = "?" + "-" + completeEndUnitdate(to.getUnitdateValue(), toStr);
        }
        return result;
    }

}
