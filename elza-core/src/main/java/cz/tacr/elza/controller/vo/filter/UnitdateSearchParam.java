package cz.tacr.elza.controller.vo.filter;

import org.springframework.util.Assert;

/**
 * Hledání podle datace.
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 16. 1. 2017
 */
public class UnitdateSearchParam extends SearchParam {

    private UnitdateCondition condition;

    private Integer calendarId;

    protected UnitdateSearchParam(final String value, final UnitdateCondition condition, final Integer calendarId) {
        super(SearchParamType.UNITDATE, value);

        Assert.notNull(condition);
        Assert.notNull(calendarId);

        this.condition = condition;
        this.calendarId = calendarId;
    }

    public UnitdateCondition getCondition() {
        return condition;
    }

    public Integer getCalendarId() {
        return calendarId;
    }
}
