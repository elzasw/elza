package cz.tacr.elza.controller.vo.filter;

import org.springframework.util.Assert;

import cz.tacr.elza.domain.convertor.CalendarConverter.CalendarType;

/**
 * Hledání podle datace.
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 16. 1. 2017
 */
public class UnitdateSearchParam extends SearchParam {

    private  UnitdateCondition condition;

    private  CalendarType calendarType;

    protected UnitdateSearchParam(final String value, final UnitdateCondition condition, final CalendarType calendarType) {
        super(SearchParamType.UNITDATE, value);

        Assert.notNull(condition);
        Assert.notNull(calendarType);

        this.condition = condition;
        this.calendarType = calendarType;
    }

    public UnitdateCondition getCondition() {
        return condition;
    }

    public CalendarType getCalendarType() {
        return calendarType;
    }
}
