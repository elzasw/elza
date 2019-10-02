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

    public UnitdateSearchParam() {
    }

    protected UnitdateSearchParam(final String value, final UnitdateCondition condition, final Integer calendarId) {
        super(SearchParamType.UNITDATE, value);

        Assert.notNull(condition, "Podmínka musí být vyplněna");
        Assert.notNull(calendarId, "Identifikátor typu kalendáře musí být vyplněn");

        this.condition = condition;
        this.calendarId = calendarId;
    }

    public UnitdateCondition getCondition() {
        return condition;
    }

    public void setCondition(final UnitdateCondition condition) {
        this.condition = condition;
    }

    public Integer getCalendarId() {
        return calendarId;
    }

    public void setCalendarId(final Integer calendarId) {
        this.calendarId = calendarId;
    }
}
